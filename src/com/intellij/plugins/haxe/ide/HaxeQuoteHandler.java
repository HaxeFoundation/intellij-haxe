package com.intellij.plugins.haxe.ide;

import com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.psi.TokenType;

/**
 * @author fedor.korotkov
 */
public class HaxeQuoteHandler extends SimpleTokenSetQuoteHandler {
  public HaxeQuoteHandler() {
    super(HaxeTokenTypes.LITSTRING, TokenType.BAD_CHARACTER);
  }
}
