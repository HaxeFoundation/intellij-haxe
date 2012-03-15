// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;
import com.intellij.plugins.haxe.lang.psi.*;

public class HaxeNewExpressionImpl extends HaxeExpressionImpl implements HaxeNewExpression {

  public HaxeNewExpressionImpl(ASTNode node) {
    super(node);
  }

  @Override
  @Nullable
  public HaxeExpressionList getExpressionList() {
    return findChildByClass(HaxeExpressionList.class);
  }

  @Override
  @NotNull
  public HaxeType getType() {
    return findNotNullChildByClass(HaxeType.class);
  }

}
