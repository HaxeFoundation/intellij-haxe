package com.intellij.plugins.haxe.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.psi.impl.HaxePsiCompositeElementImpl;
import org.jetbrains.annotations.NotNull;

public class HaxeReificationExpression extends HaxePsiCompositeElementImpl {
  public HaxeReificationExpression(@NotNull ASTNode node) {
    super(node);
  }
}
