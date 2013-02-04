package com.intellij.plugins.haxe.ide;

import com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.psi.TokenType;
import com.intellij.util.ArrayUtil;

/**
 * @author fedor.korotkov
 */
public class HaxeQuoteHandler extends SimpleTokenSetQuoteHandler {
  public HaxeQuoteHandler() {
    super(ArrayUtil.append(HaxeTokenTypeSets.STRINGS.getTypes(), TokenType.BAD_CHARACTER));
  }
}
