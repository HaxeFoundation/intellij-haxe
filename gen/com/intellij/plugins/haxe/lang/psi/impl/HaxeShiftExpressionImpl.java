// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;
import com.intellij.plugins.haxe.lang.psi.*;

public class HaxeShiftExpressionImpl extends HaxeExpressionImpl implements HaxeShiftExpression {

  public HaxeShiftExpressionImpl(ASTNode node) {
    super(node);
  }

  @Override
  @NotNull
  public List<HaxeExpression> getExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeExpression.class);
  }

  @Override
  @NotNull
  public HaxeShiftOperator getShiftOperator() {
    return findNotNullChildByClass(HaxeShiftOperator.class);
  }

}
