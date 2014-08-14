package com.intellij.plugins.haxe.hxml.lexer;

import com.intellij.lexer.FlexAdapter;

/**
 * Created by eliasku on 8/8/14.
 */
public class HXMLLexerAdapter extends FlexAdapter {
  public HXMLLexerAdapter() {
    super(new HXMLLexer());
  }
}
