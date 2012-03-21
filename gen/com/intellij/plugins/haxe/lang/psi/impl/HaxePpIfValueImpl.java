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

public class HaxePpIfValueImpl extends HaxePsiCompositeElementImpl implements HaxePpIfValue {

  public HaxePpIfValueImpl(ASTNode node) {
    super(node);
  }

  @Override
  @NotNull
  public List<HaxeExpression> getExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeExpression.class);
  }

  @Override
  @Nullable
  public HaxePpElse getPpElse() {
    return findChildByClass(HaxePpElse.class);
  }

  @Override
  @NotNull
  public List<HaxePpElseIf> getPpElseIfList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxePpElseIf.class);
  }

  @Override
  @Nullable
  public HaxePpEnd getPpEnd() {
    return findChildByClass(HaxePpEnd.class);
  }

  @Override
  @NotNull
  public HaxePpIf getPpIf() {
    return findNotNullChildByClass(HaxePpIf.class);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HaxeVisitor) ((HaxeVisitor)visitor).visitPpIfValue(this);
    else super.accept(visitor);
  }

}
