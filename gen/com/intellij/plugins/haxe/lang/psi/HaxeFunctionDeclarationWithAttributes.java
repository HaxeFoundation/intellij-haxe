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

  @Nullable
  HaxeDeclarationAttributeList getDeclarationAttributeList();

  @Nullable
  HaxeExpression getExpression();

  @NotNull
  List<HaxeGetterMeta> getGetterMetaList();

  @NotNull
  List<HaxeNsMeta> getNsMetaList();

  @Nullable
  HaxeParameterList getParameterList();

  @NotNull
  List<HaxeRequireMeta> getRequireMetaList();

  @Nullable
  HaxeReturnStatementWithoutSemicolon getReturnStatementWithoutSemicolon();

  @NotNull
  List<HaxeSetterMeta> getSetterMetaList();

  @Nullable
  HaxeTypeParam getTypeParam();

  @Nullable
  HaxeTypeTag getTypeTag();

}
