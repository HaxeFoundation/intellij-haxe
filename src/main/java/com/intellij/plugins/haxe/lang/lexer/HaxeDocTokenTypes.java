package com.intellij.plugins.haxe.lang.lexer;

import com.intellij.psi.tree.IElementType;

/**
 * Tokens inside a lazily parsed doc comment ({@link HaxeDocLexer}). Line-leading
 * whitespace the formatter may manage is emitted as real WHITE_SPACE; everything
 * else within a line lives inside these tokens and is never reformatted.
 */
public interface HaxeDocTokenTypes {
  IElementType DOC_START = new HaxeElementType("DOC_START");
  IElementType DOC_LEADING_ASTERISK = new HaxeElementType("DOC_LEADING_ASTERISK");
  IElementType DOC_TAG_NAME = new HaxeElementType("DOC_TAG_NAME");
  IElementType DOC_DATA = new HaxeElementType("DOC_DATA");
  IElementType DOC_END = new HaxeElementType("DOC_END");
}
