// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.hxml.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.intellij.plugins.haxe.hxml.psi.HXMLTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.plugins.haxe.hxml.psi.*;

public class HXMLMainImpl extends ASTWrapperPsiElement implements HXMLMain {

  public HXMLMainImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HXMLVisitor) ((HXMLVisitor)visitor).visitMain(this);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public HXMLQualifiedName getQualifiedName() {
    return findNotNullChildByClass(HXMLQualifiedName.class);
  }

}
