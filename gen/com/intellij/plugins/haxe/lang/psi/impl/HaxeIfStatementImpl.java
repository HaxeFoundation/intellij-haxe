/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

public class HaxeIfStatementImpl extends HaxeStatementPsiMixinImpl implements HaxeIfStatement {

  public HaxeIfStatementImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HaxeVisitor) ((HaxeVisitor)visitor).visitIfStatement(this);
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
