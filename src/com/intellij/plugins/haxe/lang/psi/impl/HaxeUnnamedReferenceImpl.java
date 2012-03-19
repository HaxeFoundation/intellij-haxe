package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.psi.HaxeIdentifier;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeUnnamedReferenceImpl extends HaxeReferenceImpl {
  public HaxeUnnamedReferenceImpl(ASTNode node) {
    super(node);
  }

  @Override
  public HaxeIdentifier getIdentifier() {
    return null;
  }
}
