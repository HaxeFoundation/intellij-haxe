// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HaxeBlockStatement extends HaxePsiCompositeElement {

  @NotNull
  public List<HaxeBlockStatement> getBlockStatementList();

  @NotNull
  public List<HaxeBreakStatement> getBreakStatementList();

  @NotNull
  public List<HaxeCaseStatement> getCaseStatementList();

  @NotNull
  public List<HaxeContinueStatement> getContinueStatementList();

  @NotNull
  public List<HaxeDefaultStatement> getDefaultStatementList();

  @NotNull
  public List<HaxeDoWhileStatement> getDoWhileStatementList();

  @NotNull
  public List<HaxeExpression> getExpressionList();

  @NotNull
  public List<HaxeForStatement> getForStatementList();

  @NotNull
  public List<HaxeIfStatement> getIfStatementList();

  @NotNull
  public List<HaxeLocalFunctionDeclaration> getLocalFunctionDeclarationList();

  @NotNull
  public List<HaxeLocalVarDeclaration> getLocalVarDeclarationList();

  @NotNull
  public List<HaxePp> getPpList();

  @NotNull
  public List<HaxeReturnStatement> getReturnStatementList();

  @NotNull
  public List<HaxeSwitchStatement> getSwitchStatementList();

  @NotNull
  public List<HaxeThrowStatement> getThrowStatementList();

  @NotNull
  public List<HaxeTryStatement> getTryStatementList();

  @NotNull
  public List<HaxeWhileStatement> getWhileStatementList();

}
