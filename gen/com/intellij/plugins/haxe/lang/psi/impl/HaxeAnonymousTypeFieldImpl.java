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

public class HaxeAnonymousTypeFieldImpl extends AbstractHaxeNamedComponent implements HaxeAnonymousTypeField {

  public HaxeAnonymousTypeFieldImpl(ASTNode node) {
    super(node);
  }

  @Override
  @NotNull
  public HaxeComponentName getComponentName() {
    return findNotNullChildByClass(HaxeComponentName.class);
  }

  @Override
  @NotNull
  public HaxeTypeTag getTypeTag() {
    return findNotNullChildByClass(HaxeTypeTag.class);
  }

  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HaxeVisitor) ((HaxeVisitor)visitor).visitAnonymousTypeField(this);
    else super.accept(visitor);
  }

}
