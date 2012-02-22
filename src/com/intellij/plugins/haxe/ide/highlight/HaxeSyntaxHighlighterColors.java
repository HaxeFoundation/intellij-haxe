package com.intellij.plugins.haxe.ide.highlight;

import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.SyntaxHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

/**
 * @author fedor.korotkov
 */
public class HaxeSyntaxHighlighterColors {
  public static final TextAttributesKey LINE_COMMENT =
    createTextAttributesKey("HAXE_LINE_COMMENT", SyntaxHighlighterColors.LINE_COMMENT.getDefaultAttributes());
  public static final TextAttributesKey BLOCK_COMMENT =
    createTextAttributesKey("HAXE_BLOCK_COMMENT", SyntaxHighlighterColors.JAVA_BLOCK_COMMENT.getDefaultAttributes());
  public static final TextAttributesKey KEYWORD =
    createTextAttributesKey("HAXE_KEYWORD", SyntaxHighlighterColors.KEYWORD.getDefaultAttributes());
  public static final TextAttributesKey NUMBER =
    createTextAttributesKey("HAXE_NUMBER", SyntaxHighlighterColors.NUMBER.getDefaultAttributes());
  public static final TextAttributesKey STRING =
    createTextAttributesKey("HAXE_STRING", SyntaxHighlighterColors.STRING.getDefaultAttributes());
  public static final TextAttributesKey OPERATION_SIGN =
    createTextAttributesKey("HAXE_OPERATION_SIGN", SyntaxHighlighterColors.OPERATION_SIGN.getDefaultAttributes());
  public static final TextAttributesKey PARENTHS =
    createTextAttributesKey("HAXE_PARENTH", SyntaxHighlighterColors.PARENTHS.getDefaultAttributes());
  public static final TextAttributesKey BRACKETS =
    createTextAttributesKey("HAXE_BRACKETS", SyntaxHighlighterColors.BRACKETS.getDefaultAttributes());
  public static final TextAttributesKey BRACES =
    createTextAttributesKey("HAXE_BRACES", SyntaxHighlighterColors.BRACES.getDefaultAttributes());
  public static final TextAttributesKey COMMA = createTextAttributesKey("HAXE_COMMA", SyntaxHighlighterColors.COMMA.getDefaultAttributes());
  public static final TextAttributesKey DOT = createTextAttributesKey("HAXE_DOT", SyntaxHighlighterColors.DOT.getDefaultAttributes());
  public static final TextAttributesKey SEMICOLON =
    createTextAttributesKey("HAXE_SEMICOLON", SyntaxHighlighterColors.JAVA_SEMICOLON.getDefaultAttributes());
  public static final TextAttributesKey BAD_CHARACTER =
      createTextAttributesKey("HAXE_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER.getDefaultAttributes());
}
