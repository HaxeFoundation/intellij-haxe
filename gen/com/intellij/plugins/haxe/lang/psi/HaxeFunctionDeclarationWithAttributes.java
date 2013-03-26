// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HaxeFunctionDeclarationWithAttributes extends HaxeComponentWithDeclarationList {

  @NotNull
  List<HaxeAutoBuildMacro> getAutoBuildMacroList();

  @Nullable
  HaxeBlockStatement getBlockStatement();

  @NotNull
  List<HaxeBuildMacro> getBuildMacroList();

  @Nullable
  HaxeComponentName getComponentName();

  @NotNull
  List<HaxeCustomMeta> getCustomMetaList();

  @NotNull
  List<HaxeDeclarationAttribute> getDeclarationAttributeList();

  @Nullable
  HaxeExpression getExpression();

  @Nullable
  HaxeGenericParam getGenericParam();

  @NotNull
  List<HaxeGetterMeta> getGetterMetaList();

  @NotNull
  List<HaxeMetaMeta> getMetaMetaList();

  @NotNull
  List<HaxeNsMeta> getNsMetaList();

  @NotNull
  List<HaxeOverloadMeta> getOverloadMetaList();

  @Nullable
  HaxeParameterList getParameterList();

  @NotNull
  List<HaxeRequireMeta> getRequireMetaList();

  @Nullable
  HaxeReturnStatementWithoutSemicolon getReturnStatementWithoutSemicolon();

  @NotNull
  List<HaxeSetterMeta> getSetterMetaList();

  @Nullable
  HaxeThrowStatement getThrowStatement();

  @Nullable
  HaxeTypeTag getTypeTag();

}
