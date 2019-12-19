// This is a generated file. Not intended for manual editing.
package name.kropp.intellij.makefile.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.navigation.NavigationItem;
import com.intellij.psi.StubBasedPsiElement;
import name.kropp.intellij.makefile.stub.MakefileTargetStubElement;
import com.intellij.navigation.ItemPresentation;

public interface MakefileTarget extends MakefileNamedElement, NavigationItem, StubBasedPsiElement<MakefileTargetStubElement> {

  @Nullable
  String getName();

  @NotNull
  PsiElement setName(@NotNull String newName);

  @Nullable
  PsiElement getNameIdentifier();

  @NotNull
  ItemPresentation getPresentation();

  boolean isSpecialTarget();

  boolean isPatternTarget();

  boolean matches(@NotNull String prerequisite);

  @Nullable
  String getDocComment();

}
