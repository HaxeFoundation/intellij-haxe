// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;
import com.intellij.plugins.haxe.lang.psi.*;

public class HaxeVarDeclarationImpl extends HaxePsiCompositeElementImpl implements HaxeVarDeclaration {

  public HaxeVarDeclarationImpl(ASTNode node) {
    super(node);
  }

  @Override
  @Nullable
  public HaxeDeclarationAttributeList getDeclarationAttributeList() {
    return findChildByClass(HaxeDeclarationAttributeList.class);
  }

  @Override
  @NotNull
  public List<HaxeVarDeclarationPart> getVarDeclarationPartList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeVarDeclarationPart.class);
  }

}
