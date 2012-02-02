package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public class HaxePsiCompositeElementImpl extends ASTWrapperPsiElement {
  public HaxePsiCompositeElementImpl(@NotNull ASTNode node) {
    super(node);
  }

  public IElementType getTokenType() {
    return getNode().getElementType();
  }

  public String toString() {
    return getTokenType().toString();
  }
}
