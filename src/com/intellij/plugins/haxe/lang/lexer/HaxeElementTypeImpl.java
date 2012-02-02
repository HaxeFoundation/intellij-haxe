package com.intellij.plugins.haxe.lang.lexer;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class HaxeElementTypeImpl extends HaxeElementType {

  private String debugName;

  public HaxeElementTypeImpl(@NotNull @NonNls String debugName) {
    super(debugName);
    this.debugName = debugName;
  }

  @Override
  public String toString() {
    return debugName;
  }
}
