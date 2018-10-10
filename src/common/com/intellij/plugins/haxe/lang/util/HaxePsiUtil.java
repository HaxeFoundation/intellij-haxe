/*
 * Copyright 2018 Ilya Malanin
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
package com.intellij.plugins.haxe.lang.util;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxePsiUtil {
  @Nullable
  public static IElementType getOperationType(@NotNull HaxeExpression expression) {
    final ASTNode token = expression.getNode().findChildByType(HaxeTokenTypeSets.UNARY_OPERATORS);
    if (token != null) return token.getElementType();
    return null;
  }

  public static boolean isIncrementDecrementOperation(@Nullable PsiElement element) {
    if (element instanceof HaxePrefixExpression) {
      final IElementType operationType = HaxePsiUtil.getOperationType((HaxePrefixExpression)element);
      return HaxeTokenTypeSets.UNARY_READ_WRITE_OPERATORS.contains(operationType);
    }
    else return element instanceof HaxeSuffixExpression;
  }

  @Nullable
  public static PsiElement skipParenthesizedExprUp(@Nullable PsiElement parent) {
    while (parent instanceof HaxeParenthesizedExpression) {
      parent = parent.getParent();
    }
    return parent;
  }

  public static boolean isOnAssignmentLeftHand(@NotNull HaxeExpression expr) {
    final PsiElement parent = PsiTreeUtil.skipParentsOfType(expr, HaxeParenthesizedExpression.class);
    return parent instanceof HaxeAssignExpression &&
           PsiTreeUtil.isAncestor(((HaxeAssignExpression)parent).getExpressionList().get(0), expr, false);
  }

  public static boolean isAccessedForWriting(@NotNull HaxeExpression expr) {
    if (isOnAssignmentLeftHand(expr)) return true;
    final PsiElement parent = skipParenthesizedExprUp(expr.getParent());
    return isIncrementDecrementOperation(parent);
  }

  public static boolean isAccessedForReading(@NotNull HaxeExpression expr) {
    final PsiElement parent = PsiTreeUtil.skipParentsOfType(expr, HaxeParenthesizedExpression.class);
    return !(parent instanceof HaxeAssignExpression) ||
           !PsiTreeUtil.isAncestor(((HaxeAssignExpression)parent).getExpressionList().get(0), expr, false) ||
           getAssignOperationElementType((HaxeAssignExpression)parent) != HaxeTokenTypes.OASSIGN;
  }

  @Nullable
  private static IElementType getAssignOperationElementType(@NotNull HaxeAssignExpression element) {
    final ASTNode token = element.getAssignOperation().getNode().findChildByType(HaxeTokenTypeSets.ASSIGN_OPERATORS);
    if (token != null) return token.getElementType();
    return null;
  }
}
