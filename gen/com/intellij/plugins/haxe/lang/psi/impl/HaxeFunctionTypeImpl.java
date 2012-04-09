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

public class HaxeFunctionTypeImpl extends HaxePsiCompositeElementImpl implements HaxeFunctionType {

  public HaxeFunctionTypeImpl(ASTNode node) {
    super(node);
  }

  @Override
  @NotNull
  public List<HaxeAnonymousType> getAnonymousTypeList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeAnonymousType.class);
  }

  @Override
  @Nullable
  public HaxeFunctionType getFunctionType() {
    return findChildByClass(HaxeFunctionType.class);
  }

  @Override
  @Nullable
  public HaxeType getType() {
    return findChildByClass(HaxeType.class);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HaxeVisitor) ((HaxeVisitor)visitor).visitFunctionType(this);
    else super.accept(visitor);
  }

}
