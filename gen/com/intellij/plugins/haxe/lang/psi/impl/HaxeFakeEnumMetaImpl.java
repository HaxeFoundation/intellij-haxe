// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.psi.HaxeFakeEnumMeta;
import com.intellij.plugins.haxe.lang.psi.HaxeIdentifier;
import org.jetbrains.annotations.NotNull;

public class HaxeFakeEnumMetaImpl extends HaxePsiCompositeElementImpl implements HaxeFakeEnumMeta {

  public HaxeFakeEnumMetaImpl(ASTNode node) {
    super(node);
  }

  @Override
  @NotNull
  public HaxeIdentifier getIdentifier() {
    return findNotNullChildByClass(HaxeIdentifier.class);
  }
}
