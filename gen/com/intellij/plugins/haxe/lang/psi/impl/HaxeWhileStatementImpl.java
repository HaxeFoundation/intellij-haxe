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

public class HaxeWhileStatementImpl extends HaxeStatementPsiMixinImpl implements HaxeWhileStatement {

  public HaxeWhileStatementImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HaxeVisitor) ((HaxeVisitor)visitor).visitWhileStatement(this);
    else super.accept(visitor);
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
  public List<HaxeContinueStatement> getContinueStatementList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeContinueStatement.class);
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
  public List<HaxeMacroClassList> getMacroClassListList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeMacroClassList.class);
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

}
