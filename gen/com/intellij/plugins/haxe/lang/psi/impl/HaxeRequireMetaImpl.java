// This is a generated file. Not intended for manual editing.
package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.psi.HaxeIdentifier;
import com.intellij.plugins.haxe.lang.psi.HaxeRequireMeta;
import org.jetbrains.annotations.NotNull;

public class HaxeRequireMetaImpl extends HaxePsiCompositeElementImpl implements HaxeRequireMeta {

  public HaxeRequireMetaImpl(ASTNode node) {
    super(node);
  }

  @Override
  @NotNull
  public HaxeIdentifier getIdentifier() {
    return findNotNullChildByClass(HaxeIdentifier.class);
  }
}
