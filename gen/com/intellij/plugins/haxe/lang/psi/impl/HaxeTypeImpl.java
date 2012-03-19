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

public class HaxeTypeImpl extends HaxePsiCompositeElementImpl implements HaxeType {

  public HaxeTypeImpl(ASTNode node) {
    super(node);
  }

  @Override
  @NotNull
  public HaxeExpression getExpression() {
    return findNotNullChildByClass(HaxeExpression.class);
  }

  @Override
  @Nullable
  public HaxeTypeParam getTypeParam() {
    return findChildByClass(HaxeTypeParam.class);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HaxeVisitor) ((HaxeVisitor)visitor).visitType(this);
    else super.accept(visitor);
  }

}
