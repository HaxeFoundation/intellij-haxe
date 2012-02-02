// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;
import com.intellij.plugins.haxe.lang.psi.*;

public class HaxeTypeConstraintImpl extends HaxePsiCompositeElementImpl implements HaxeTypeConstraint {

  public HaxeTypeConstraintImpl(ASTNode node) {
    super(node);
  }

  @Override
  @Nullable
  public HaxeTypeList getTypeList() {
    return findChildByClass(HaxeTypeList.class);
  }

}
