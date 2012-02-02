package com.intellij.plugins.haxe.lang.lexer;

import com.intellij.lexer.FlexAdapter;

import java.io.Reader;

public class HaxeFlexLexer extends FlexAdapter {
  public HaxeFlexLexer() {
    super(new _HaxeLexer((Reader)null));
  }
}
