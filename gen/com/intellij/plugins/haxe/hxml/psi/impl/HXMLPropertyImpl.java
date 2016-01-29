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

public class HXMLPropertyImpl extends ASTWrapperPsiElement implements HXMLProperty {

  public HXMLPropertyImpl(ASTNode node) {
    super(node);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HXMLVisitor) ((HXMLVisitor)visitor).visitProperty(this);
    else super.accept(visitor);
  }

  public String getKey() {
    return HXMLPsiImplUtil.getKey(this);
  }

  public String getValue() {
    return HXMLPsiImplUtil.getValue(this);
  }

}
