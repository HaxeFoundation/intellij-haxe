// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HaxeVarDeclaration extends HaxePsiCompositeElement {

  @NotNull
  List<HaxeCustomMeta> getCustomMetaList();

  @Nullable
  HaxeDeclarationAttributeList getDeclarationAttributeList();

  @NotNull
  List<HaxeGetterMeta> getGetterMetaList();

  @NotNull
  List<HaxeNsMeta> getNsMetaList();

  @NotNull
  List<HaxeRequireMeta> getRequireMetaList();

  @NotNull
  List<HaxeSetterMeta> getSetterMetaList();

  @NotNull
  List<HaxeVarDeclarationPart> getVarDeclarationPartList();

}
