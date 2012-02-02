// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;
import com.intellij.plugins.haxe.lang.psi.*;

public class HaxeTypeListImpl extends HaxePsiCompositeElementImpl implements HaxeTypeList {

  public HaxeTypeListImpl(ASTNode node) {
    super(node);
  }

  @Override
  @Nullable
  public HaxeFunctionType getFunctionType() {
    return findChildByClass(HaxeFunctionType.class);
  }

  @Override
  @Nullable
  public HaxeTypeConstraint getTypeConstraint() {
    return findChildByClass(HaxeTypeConstraint.class);
  }

  @Override
  @NotNull
  public List<HaxeTypeList> getTypeListList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HaxeTypeList.class);
  }

}
