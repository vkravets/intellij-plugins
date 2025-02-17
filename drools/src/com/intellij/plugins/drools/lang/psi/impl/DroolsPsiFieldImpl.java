// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.plugins.drools.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Computable;
import com.intellij.plugins.drools.lang.psi.*;
import com.intellij.plugins.drools.lang.psi.util.DroolsElementsFactory;
import com.intellij.plugins.drools.lang.psi.util.DroolsResolveUtil;
import com.intellij.psi.*;
import com.intellij.psi.impl.light.JavaIdentifier;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class DroolsPsiFieldImpl extends DroolsPsiCompositeElementImpl implements DroolsPsiField, DroolsField {

  private Computable<PsiType> myType;

  public DroolsPsiFieldImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  @NotNull
  public String getName() {
    return getFieldName().getText();
  }

  @Override
  public void setInitializer(@Nullable PsiExpression initializer) throws IncorrectOperationException {

  }


  @NotNull
  @Override
  public PsiIdentifier getNameIdentifier() {
    return new JavaIdentifier(getManager(), getFieldName());
  }

  @Nullable
  @Override
  public PsiDocComment getDocComment() {
    return null;
  }

  @Override
  public boolean isDeprecated() {
    return false;
  }

  @Nullable
  @Override
  public PsiClass getContainingClass() {
    return PsiTreeUtil.getParentOfType(this, DroolsPsiClass.class);
  }

  @NotNull
  @Override
  public PsiType getType() {
    final DroolsFieldType droolsFieldType = getFieldType();
    if (droolsFieldType != null) {
      DroolsType type = droolsFieldType.getType();
      if (type != null) {
        final PsiType psiType = DroolsResolveUtil.resolveType(type);
        if (psiType != null) return psiType;
      }
      DroolsPrimitiveType primitiveType = droolsFieldType.getPrimitiveType();
      if (primitiveType != null) {
        final PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(primitiveType.getProject());
        return elementFactory.createTypeFromText(primitiveType.getText(), primitiveType);
      }
    }
    return PsiType.getJavaLangObject(getManager(), getResolveScope());
  }

  @Nullable
  @Override
  public PsiTypeElement getTypeElement() {
    return null;
  }

  @Nullable
  @Override
  public PsiExpression getInitializer() {
    return null;
  }

  @Override
  public boolean hasInitializer() {
    return false;
  }

  @Override
  public void normalizeDeclaration() throws IncorrectOperationException {

  }

  @Nullable
  @Override
  public Object computeConstantValue() {
    return null;
  }

  @Override
  public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
    final DroolsFieldName oldIdentifier = getFieldName();

    final PsiElement nameIdentifier = DroolsElementsFactory.createFieldNameIdentifier(name, getProject());
    if (nameIdentifier != null) {
      oldIdentifier.replace(nameIdentifier);
    }
    return this;
  }

  @Nullable
  @Override
  public PsiModifierList getModifierList() {
    return null;
  }

  @Override
  public boolean hasModifierProperty(@PsiModifier.ModifierConstant @NonNls @NotNull String name) {
    return false;
  }
}
