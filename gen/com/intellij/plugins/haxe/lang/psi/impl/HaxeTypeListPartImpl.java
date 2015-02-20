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

public class HaxeTypeListPartImpl extends HaxeTypeListPartPsiMixinImpl implements HaxeTypeListPart {

  public HaxeTypeListPartImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HaxeVisitor) ((HaxeVisitor)visitor).visitTypeListPart(this);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<HaxeFunctionType> getFunctionTypeList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeFunctionType.class);
  }

  @Override
  @NotNull
  public List<HaxeTypeOrAnonymous> getTypeOrAnonymousList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeTypeOrAnonymous.class);
  }

}
