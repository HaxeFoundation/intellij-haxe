// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HaxeExternFunctionDeclaration extends HaxePsiCompositeElement {

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
