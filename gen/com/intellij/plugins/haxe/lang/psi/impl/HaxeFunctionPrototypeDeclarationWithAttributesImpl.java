// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.psi.*;
import org.jetbrains.annotations.Nullable;

public class HaxeFunctionPrototypeDeclarationWithAttributesImpl extends HaxePsiCompositeElementImpl
  implements HaxeFunctionPrototypeDeclarationWithAttributes {

  public HaxeFunctionPrototypeDeclarationWithAttributesImpl(ASTNode node) {
    super(node);
  }

  @Override
  @Nullable
  public HaxeDeclarationAttributeList getDeclarationAttributeList() {
    return findChildByClass(HaxeDeclarationAttributeList.class);
  }

  @Override
  @Nullable
  public HaxeParameterList getParameterList() {
    return findChildByClass(HaxeParameterList.class);
  }

  @Override
  @Nullable
  public HaxeTypeParam getTypeParam() {
    return findChildByClass(HaxeTypeParam.class);
  }

  @Override
  @Nullable
  public HaxeTypeTag getTypeTag() {
    return findChildByClass(HaxeTypeTag.class);
  }
}
