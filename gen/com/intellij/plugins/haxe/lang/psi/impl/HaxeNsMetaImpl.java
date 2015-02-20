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

public class HaxeNsMetaImpl extends HaxePsiCompositeElementImpl implements HaxeNsMeta {

  public HaxeNsMetaImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HaxeVisitor) ((HaxeVisitor)visitor).visitNsMeta(this);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public HaxeStringLiteralExpression getStringLiteralExpression() {
    return findChildByClass(HaxeStringLiteralExpression.class);
  }

}
