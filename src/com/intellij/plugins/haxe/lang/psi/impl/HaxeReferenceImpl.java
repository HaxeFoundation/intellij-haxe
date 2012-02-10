package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.Nullable;

public abstract class HaxeReferenceImpl extends HaxeExpressionImpl implements HaxeReferenceExpression {

  public HaxeReferenceImpl(ASTNode node) {
    super(node);
  }

  @Nullable
  HaxeReferenceExpression getQualifier() {
    return findChildByClass(HaxeReferenceExpression.class);
  }

  @Override
  public PsiReference getReference() {
    // todo
    return super.getReference();
  }
}
