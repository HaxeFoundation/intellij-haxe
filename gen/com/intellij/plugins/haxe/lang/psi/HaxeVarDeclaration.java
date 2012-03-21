// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HaxeVarDeclaration extends HaxePsiCompositeElement {

  @Nullable
  HaxeDeclarationAttributeList getDeclarationAttributeList();

  @NotNull
  List<HaxeRequireMeta> getRequireMetaList();

  @NotNull
  List<HaxeVarDeclarationPart> getVarDeclarationPartList();

}
