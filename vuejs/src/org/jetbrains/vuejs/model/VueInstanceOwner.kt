// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.vuejs.model

import com.intellij.lang.javascript.psi.JSRecordType
import com.intellij.lang.javascript.psi.JSType
import com.intellij.lang.javascript.psi.ecma6.TypeScriptInterface
import com.intellij.lang.javascript.psi.ecma6.TypeScriptTypeAlias
import com.intellij.lang.javascript.psi.stubs.JSImplicitElement
import com.intellij.lang.javascript.psi.types.*
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.castSafelyTo
import org.jetbrains.vuejs.codeInsight.resolveSymbolFromNodeModule
import org.jetbrains.vuejs.index.VUE_MODULE
import org.jetbrains.vuejs.model.source.VUE_NAMESPACE
import org.jetbrains.vuejs.model.source.VueContainerInfoProvider
import java.util.*

interface VueInstanceOwner : VueScopeElement {
  val thisType: JSType
    get() = source?.let { source ->
      CachedValuesManager.getCachedValue(source) {
        CachedValueProvider.Result.create(buildInstanceType(this), PsiModificationTracker.MODIFICATION_COUNT)
      }
    } ?: JSAnyType.get(source, false)
}

private val VUE_INSTANCE_PROPERTIES: List<String> = listOf(
  "\$el", "\$options", "\$parent", "\$root", "\$children", "\$refs", "\$slots",
  "\$scopedSlots", "\$isServer", "\$data", "\$props",
  "\$ssrContext", "\$vnode", "\$attrs", "\$listeners")

private val VUE_INSTANCE_METHODS: List<String> = listOf(
  "\$mount", "\$forceUpdate", "\$destroy", "\$set", "\$delete", "\$watch", "\$on",
  "\$once", "\$off", "\$emit", "\$nextTick", "\$createElement")

private fun buildInstanceType(instance: VueInstanceOwner): JSType? {
  val source = instance.source ?: return null
  val result = mutableMapOf<String, JSRecordType.PropertySignature>()
  contributeDefaultInstanceProperties(source, result)
  contributeComponentProperties(instance, source, result)
  contributePropertiesFromProviders(instance, result)
  return JSSimpleRecordTypeImpl(JSTypeSourceFactory.createTypeSource(source, true), result.values.toList())
}

private fun contributeDefaultInstanceProperties(source: PsiElement,
                                                result: MutableMap<String, JSRecordType.PropertySignature>): MutableMap<String, JSRecordType.PropertySignature> {
  val defaultInstanceType =
    resolveSymbolFromNodeModule(source, VUE_MODULE, "ComponentPublicInstance", TypeScriptTypeAlias::class.java)
      ?.typeDeclaration?.jsType
    ?: resolveSymbolFromNodeModule(source, VUE_MODULE, VUE_NAMESPACE, TypeScriptInterface::class.java)?.jsType
  if (defaultInstanceType != null) {
    defaultInstanceType.asRecordType()
      .properties
      .associateByTo(result) { it.memberName }
  }
  else {
    // Fallback to a predefined list of properties without any typings
    VUE_INSTANCE_PROPERTIES.forEach {
      result[it] = createImplicitPropertySignature(it, null, source)
    }
    VUE_INSTANCE_METHODS.forEach {
      result[it] = createImplicitPropertySignature(it, null, source, kind = JSImplicitElement.Type.Function)
    }
  }
  return result
}

private fun contributePropertiesFromProviders(instance: VueInstanceOwner, result: MutableMap<String, JSRecordType.PropertySignature>) {
  val unmodifiableResult = Collections.unmodifiableMap(result)
  VueContainerInfoProvider.getProviders().asSequence()
    .flatMap { it.getThisTypeProperties(instance, unmodifiableResult).asSequence() }
    .associateByTo(result) { it.memberName }
}

private fun contributeComponentProperties(instance: VueInstanceOwner,
                                          source: PsiElement,
                                          result: MutableMap<String, JSRecordType.PropertySignature>) {
  val proximityMap = mutableMapOf<String, VueModelVisitor.Proximity>()

  val props = mutableMapOf<String, JSRecordType.PropertySignature>()
  val computed = mutableMapOf<String, JSRecordType.PropertySignature>()
  val data = mutableMapOf<String, JSRecordType.PropertySignature>()
  val methods = mutableMapOf<String, JSRecordType.PropertySignature>()

  instance.castSafelyTo<VueEntitiesContainer>()
    ?.acceptPropertiesAndMethods(object : VueModelProximityVisitor() {

      override fun visitInputProperty(prop: VueInputProperty, proximity: Proximity): Boolean {
        process(prop, proximity, props, true)
        return true
      }

      override fun visitComputedProperty(computedProperty: VueComputedProperty, proximity: Proximity): Boolean {
        process(computedProperty, proximity, computed, true)
        return true
      }

      override fun visitDataProperty(dataProperty: VueDataProperty, proximity: Proximity): Boolean {
        process(dataProperty, proximity, data, false)
        return true
      }

      override fun visitMethod(method: VueMethod, proximity: Proximity): Boolean {
        process(method, proximity, methods, false)
        return true
      }

      private fun process(property: VueProperty,
                          proximity: Proximity,
                          dest: MutableMap<String, JSRecordType.PropertySignature>,
                          isReadOnly: Boolean) {
        if ((proximityMap.putIfAbsent(property.name, proximity) ?: proximity) >= proximity) {
          dest.merge(property.name,
                     JSRecordTypeImpl.PropertySignatureImpl(property.name, property.jsType, false, isReadOnly, property.source),
                     ::mergeSignatures)
        }
      }

    }, onlyPublic = false)

  replaceStandardProperty("\$props", props.values.toList(), source, result)
  replaceStandardProperty("\$data", data.values.toList(), source, result)

  result.keys.removeIf {
    props.containsKey(it) || data.containsKey(it) || computed.containsKey(it) || methods.containsKey(it)
  }

  mergePut(result, props)
  mergePut(result, data)
  mergePut(result, computed)
  mergePut(result, methods)
}

private fun replaceStandardProperty(propName: String, properties: List<JSRecordType.PropertySignature>,
                                    defaultSource: PsiElement, result: MutableMap<String, JSRecordType.PropertySignature>) {
  val propSource = result[propName]?.memberSource?.singleElement ?: defaultSource
  result[propName] = createImplicitPropertySignature(
    propName, JSSimpleRecordTypeImpl(JSTypeSourceFactory.createTypeSource(propSource, false), properties), propSource)
}

private fun mergeSignatures(existing: JSRecordType.PropertySignature,
                            updated: JSRecordType.PropertySignature): JSRecordType.PropertySignature {
  val existingType = existing.jsType
  val updatedType = updated.jsType
  val type: JSType? = if (existingType == null || updatedType == null)
    null
  else
    JSCompositeTypeFactory.createUnionType(existingType.source, existingType, updatedType)
  return JSRecordTypeImpl.PropertySignatureImpl(
    existing.memberName, type, existing.isOptional && updated.isOptional,
    false, JSRecordMemberSourceFactory.createSource(existing.memberSource.allSourceElements +
                                                    updated.memberSource.allSourceElements,
                                                    JSRecordType.MemberSourceKind.Union))
}

private fun mergePut(result: MutableMap<String, JSRecordType.PropertySignature>,
                     contributions: MutableMap<String, JSRecordType.PropertySignature>) =
  contributions.forEach { (name, value) ->
    result.merge(name, value, ::mergeSignatures)
  }

fun createImplicitPropertySignature(name: String,
                                    type: JSType?,
                                    source: PsiElement,
                                    equivalentToSource: Boolean = false,
                                    isReadOnly: Boolean = false,
                                    kind: JSImplicitElement.Type = JSImplicitElement.Type.Property): JSRecordType.PropertySignature {
  return JSRecordTypeImpl.PropertySignatureImpl(name, type, false, isReadOnly,
                                                VueImplicitElement(name, type, source, kind, equivalentToSource))
}