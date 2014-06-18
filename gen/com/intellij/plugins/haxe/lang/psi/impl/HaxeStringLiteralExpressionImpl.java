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

public class HaxeStringLiteralExpressionImpl extends HaxeReferenceImpl implements HaxeStringLiteralExpression {

  public HaxeStringLiteralExpressionImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HaxeVisitor) ((HaxeVisitor)visitor).visitStringLiteralExpression(this);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<HaxeLongTemplateEntry> getLongTemplateEntryList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeLongTemplateEntry.class);
  }

  @Override
  @NotNull
  public List<HaxeShortTemplateEntry> getShortTemplateEntryList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeShortTemplateEntry.class);
  }

}
