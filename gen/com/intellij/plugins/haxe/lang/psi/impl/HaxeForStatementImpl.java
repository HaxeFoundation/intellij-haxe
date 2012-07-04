// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;
import com.intellij.plugins.haxe.lang.psi.*;

public class HaxeForStatementImpl extends AbstractHaxeNamedComponent implements HaxeForStatement {

  public HaxeForStatementImpl(ASTNode node) {
    super(node);
  }

  @Override
  @Nullable
  public HaxeBlockStatement getBlockStatement() {
    return findChildByClass(HaxeBlockStatement.class);
  }

  @Override
  @Nullable
  public HaxeBreakStatement getBreakStatement() {
    return findChildByClass(HaxeBreakStatement.class);
  }

  @Override
  @Nullable
  public HaxeComponentName getComponentName() {
    return findChildByClass(HaxeComponentName.class);
  }

  @Override
  @Nullable
  public HaxeContinueStatement getContinueStatement() {
    return findChildByClass(HaxeContinueStatement.class);
  }

  @Override
  @Nullable
  public HaxeDoWhileStatement getDoWhileStatement() {
    return findChildByClass(HaxeDoWhileStatement.class);
  }

  @Override
  @Nullable
  public HaxeExpression getExpression() {
    return findChildByClass(HaxeExpression.class);
  }

  @Override
  @Nullable
  public HaxeForStatement getForStatement() {
    return findChildByClass(HaxeForStatement.class);
  }

  @Override
  @Nullable
  public HaxeIfStatement getIfStatement() {
    return findChildByClass(HaxeIfStatement.class);
  }

  @Override
  @Nullable
  public HaxeIterable getIterable() {
    return findChildByClass(HaxeIterable.class);
  }

  @Override
  @Nullable
  public HaxeLocalFunctionDeclaration getLocalFunctionDeclaration() {
    return findChildByClass(HaxeLocalFunctionDeclaration.class);
  }

  @Override
  @Nullable
  public HaxeLocalVarDeclaration getLocalVarDeclaration() {
    return findChildByClass(HaxeLocalVarDeclaration.class);
  }

  @Override
  @Nullable
  public HaxeReturnStatement getReturnStatement() {
    return findChildByClass(HaxeReturnStatement.class);
  }

  @Override
  @Nullable
  public HaxeSwitchStatement getSwitchStatement() {
    return findChildByClass(HaxeSwitchStatement.class);
  }

  @Override
  @Nullable
  public HaxeThrowStatement getThrowStatement() {
    return findChildByClass(HaxeThrowStatement.class);
  }

  @Override
  @Nullable
  public HaxeTryStatement getTryStatement() {
    return findChildByClass(HaxeTryStatement.class);
  }

  @Override
  @Nullable
  public HaxeWhileStatement getWhileStatement() {
    return findChildByClass(HaxeWhileStatement.class);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HaxeVisitor) ((HaxeVisitor)visitor).visitForStatement(this);
    else super.accept(visitor);
  }

}
