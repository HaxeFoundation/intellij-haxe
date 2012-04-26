// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HaxeFunctionPrototypeDeclarationWithAttributes extends HaxeComponentWithDeclarationList {

  @NotNull
  List<HaxeAutoBuildMacro> getAutoBuildMacroList();

  @NotNull
  List<HaxeBuildMacro> getBuildMacroList();

  @Nullable
  HaxeComponentName getComponentName();

  @NotNull
  List<HaxeCustomMeta> getCustomMetaList();

  @Nullable
  HaxeDeclarationAttributeList getDeclarationAttributeList();

  @Nullable
  HaxeGenericParam getGenericParam();

  @NotNull
  List<HaxeGetterMeta> getGetterMetaList();

  @NotNull
  List<HaxeNsMeta> getNsMetaList();

  @NotNull
  List<HaxeOverloadMeta> getOverloadMetaList();

  @Nullable
  HaxeParameterList getParameterList();

  @NotNull
  List<HaxeRequireMeta> getRequireMetaList();

  @NotNull
  List<HaxeSetterMeta> getSetterMetaList();

  @Nullable
  HaxeTypeTag getTypeTag();

}
