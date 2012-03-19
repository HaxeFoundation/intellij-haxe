// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HaxeParenthesizedExpression extends HaxeExpression {

  @Nullable
  HaxeBlockStatement getBlockStatement();

  @Nullable
  HaxeBreakStatement getBreakStatement();

  @Nullable
  HaxeCaseStatement getCaseStatement();

  @Nullable
  HaxeContinueStatement getContinueStatement();

  @Nullable
  HaxeDefaultStatement getDefaultStatement();

  @Nullable
  HaxeDoWhileStatement getDoWhileStatement();

  @Nullable
  HaxeExpression getExpression();

  @Nullable
  HaxeForStatement getForStatement();

  @Nullable
  HaxeIfStatement getIfStatement();

  @Nullable
  HaxeLocalFunctionDeclaration getLocalFunctionDeclaration();

  @Nullable
  HaxeLocalVarDeclaration getLocalVarDeclaration();

  @Nullable
  HaxePp getPp();

  @Nullable
  HaxeReturnStatement getReturnStatement();

  @Nullable
  HaxeSwitchStatement getSwitchStatement();

  @Nullable
  HaxeThrowStatement getThrowStatement();

  @Nullable
  HaxeTryStatement getTryStatement();

  @Nullable
  HaxeWhileStatement getWhileStatement();

}
