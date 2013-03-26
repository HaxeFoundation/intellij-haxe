// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HaxeVarDeclaration extends HaxePsiCompositeElement {

  @NotNull
  List<HaxeAutoBuildMacro> getAutoBuildMacroList();

  @NotNull
  List<HaxeBuildMacro> getBuildMacroList();

  @NotNull
  List<HaxeCustomMeta> getCustomMetaList();

  @NotNull
  List<HaxeDeclarationAttribute> getDeclarationAttributeList();

  @NotNull
  List<HaxeGetterMeta> getGetterMetaList();

  @NotNull
  List<HaxeMetaMeta> getMetaMetaList();

  @NotNull
  List<HaxeNsMeta> getNsMetaList();

  @NotNull
  List<HaxeRequireMeta> getRequireMetaList();

  @NotNull
  List<HaxeSetterMeta> getSetterMetaList();

  @NotNull
  List<HaxeVarDeclarationPart> getVarDeclarationPartList();

}
