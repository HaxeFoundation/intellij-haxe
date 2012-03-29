// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HaxeFunctionPrototypeDeclarationWithAttributes extends HaxeComponent {

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

  @NotNull
  List<HaxeGetterMeta> getGetterMetaList();

  @NotNull
  List<HaxeNsMeta> getNsMetaList();

  @Nullable
  HaxeParameterList getParameterList();

  @NotNull
  List<HaxeRequireMeta> getRequireMetaList();

  @NotNull
  List<HaxeSetterMeta> getSetterMetaList();

  @Nullable
  HaxeTypeParam getTypeParam();

  @Nullable
  HaxeTypeTag getTypeTag();

}
