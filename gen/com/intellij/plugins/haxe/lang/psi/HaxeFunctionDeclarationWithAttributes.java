// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HaxeFunctionDeclarationWithAttributes extends HaxeMethod {

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
  List<HaxeDebugMeta> getDebugMetaList();

  @NotNull
  List<HaxeDeclarationAttribute> getDeclarationAttributeList();

  @Nullable
  HaxeDoWhileStatement getDoWhileStatement();

  @Nullable
  HaxeExpression getExpression();

  @NotNull
  List<HaxeFinalMeta> getFinalMetaList();

  @Nullable
  HaxeForStatement getForStatement();

  @Nullable
  HaxeGenericParam getGenericParam();

  @NotNull
  List<HaxeGetterMeta> getGetterMetaList();

  @Nullable
  HaxeIfStatement getIfStatement();

  @NotNull
  List<HaxeKeepMeta> getKeepMetaList();

  @NotNull
  List<HaxeMetaMeta> getMetaMetaList();

  @NotNull
  List<HaxeNoDebugMeta> getNoDebugMetaList();

  @NotNull
  List<HaxeNsMeta> getNsMetaList();

  @NotNull
  List<HaxeOverloadMeta> getOverloadMetaList();

  @Nullable
  HaxeParameterList getParameterList();

  @NotNull
  List<HaxeProtectedMeta> getProtectedMetaList();

  @NotNull
  List<HaxeRequireMeta> getRequireMetaList();

  @Nullable
  HaxeReturnStatement getReturnStatement();

  @NotNull
  List<HaxeSetterMeta> getSetterMetaList();

  @Nullable
  HaxeThrowStatement getThrowStatement();

  @Nullable
  HaxeTypeTag getTypeTag();

  @Nullable
  HaxeWhileStatement getWhileStatement();

}
