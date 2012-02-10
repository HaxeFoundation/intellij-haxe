package com.intellij.plugins.haxe.lang.lexer;

import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class HaxeElementType extends IElementType {

  public HaxeElementType(@NotNull @NonNls String debugName) {
    super(debugName, HaxeLanguage.INSTANCE);
  }
}
