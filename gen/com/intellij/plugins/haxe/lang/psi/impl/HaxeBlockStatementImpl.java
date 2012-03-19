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

public class HaxeBlockStatementImpl extends HaxePsiCompositeElementImpl implements HaxeBlockStatement {

  public HaxeBlockStatementImpl(ASTNode node) {
    super(node);
  }

  @Override
  @NotNull
  public List<HaxeBlockStatement> getBlockStatementList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeBlockStatement.class);
  }

  @Override
  @NotNull
  public List<HaxeBreakStatement> getBreakStatementList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeBreakStatement.class);
  }

  @Override
  @NotNull
  public List<HaxeCaseStatement> getCaseStatementList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeCaseStatement.class);
  }

  @Override
  @NotNull
  public List<HaxeContinueStatement> getContinueStatementList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeContinueStatement.class);
  }

  @Override
  @NotNull
  public List<HaxeDefaultStatement> getDefaultStatementList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeDefaultStatement.class);
  }

  @Override
  @NotNull
  public List<HaxeDoWhileStatement> getDoWhileStatementList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeDoWhileStatement.class);
  }

  @Override
  @NotNull
  public List<HaxeExpression> getExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeExpression.class);
  }

  @Override
  @NotNull
  public List<HaxeForStatement> getForStatementList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeForStatement.class);
  }

  @Override
  @NotNull
  public List<HaxeIfStatement> getIfStatementList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeIfStatement.class);
  }

  @Override
  @NotNull
  public List<HaxeLocalFunctionDeclaration> getLocalFunctionDeclarationList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeLocalFunctionDeclaration.class);
  }

  @Override
  @NotNull
  public List<HaxeLocalVarDeclaration> getLocalVarDeclarationList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeLocalVarDeclaration.class);
  }

  @Override
  @NotNull
  public List<HaxePp> getPpList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxePp.class);
  }

  @Override
  @NotNull
  public List<HaxeReturnStatement> getReturnStatementList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeReturnStatement.class);
  }

  @Override
  @NotNull
  public List<HaxeSwitchStatement> getSwitchStatementList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeSwitchStatement.class);
  }

  @Override
  @NotNull
  public List<HaxeThrowStatement> getThrowStatementList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeThrowStatement.class);
  }

  @Override
  @NotNull
  public List<HaxeTryStatement> getTryStatementList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeTryStatement.class);
  }

  @Override
  @NotNull
  public List<HaxeWhileStatement> getWhileStatementList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeWhileStatement.class);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HaxeVisitor) ((HaxeVisitor)visitor).visitBlockStatement(this);
    else super.accept(visitor);
  }

}
