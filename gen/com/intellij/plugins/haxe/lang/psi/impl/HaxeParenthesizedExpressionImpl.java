/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
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

public class HaxeParenthesizedExpressionImpl extends HaxeExpressionImpl implements HaxeParenthesizedExpression {

  public HaxeParenthesizedExpressionImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HaxeVisitor) ((HaxeVisitor)visitor).visitParenthesizedExpression(this);
    else super.accept(visitor);
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

}
