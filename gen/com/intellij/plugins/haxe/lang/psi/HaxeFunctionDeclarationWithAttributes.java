// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface HaxeFunctionDeclarationWithAttributes extends HaxeComponent {

  @Nullable
  public HaxeBlockStatement getBlockStatement();

  @Nullable
  public HaxeComponentName getComponentName();

  @Nullable
  public HaxeDeclarationAttributeList getDeclarationAttributeList();

  @Nullable
  public HaxeParameterList getParameterList();

  @NotNull
  public List<HaxeRequireMeta> getRequireMetaList();

  @Nullable
  public HaxeReturnStatementWithoutSemicolon getReturnStatementWithoutSemicolon();

  @Nullable
  public HaxeTypeParam getTypeParam();

  @Nullable
  public HaxeTypeTag getTypeTag();
}
