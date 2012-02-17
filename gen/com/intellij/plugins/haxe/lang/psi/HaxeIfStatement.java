// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HaxeIfStatement extends HaxePsiCompositeElement {

  @Nullable
  public HaxeBlockStatement getBlockStatement();

  @Nullable
  public HaxeBreakStatement getBreakStatement();

  @Nullable
  public HaxeCaseStatement getCaseStatement();

  @Nullable
  public HaxeContinueStatement getContinueStatement();

  @Nullable
  public HaxeDefaultStatement getDefaultStatement();

  @Nullable
  public HaxeDoWhileStatement getDoWhileStatement();

  @NotNull
  public List<HaxeExpression> getExpressionList();

  @Nullable
  public HaxeForStatement getForStatement();

  @Nullable
  public HaxeIfStatement getIfStatement();

  @Nullable
  public HaxeLocalFunctionDeclaration getLocalFunctionDeclaration();

  @Nullable
  public HaxeLocalVarDeclaration getLocalVarDeclaration();

  @Nullable
  public HaxePp getPp();

  @Nullable
  public HaxeReturnStatement getReturnStatement();

  @Nullable
  public HaxeSwitchStatement getSwitchStatement();

  @Nullable
  public HaxeThrowStatement getThrowStatement();

  @Nullable
  public HaxeTryStatement getTryStatement();

  @Nullable
  public HaxeWhileStatement getWhileStatement();

}
