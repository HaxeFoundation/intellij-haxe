package com.intellij.plugins.haxe.ide.highlight;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.plugins.haxe.lang.lexer.HaxeLexer;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.*;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;

public class HaxeSyntaxHighlighter extends SyntaxHighlighterBase {
  private static final Map<IElementType, TextAttributesKey> ATTRIBUTES = new HashMap<IElementType, TextAttributesKey>();

  static {
    fillMap(ATTRIBUTES, KEYWORDS, HaxeSyntaxHighlighterColors.KEYWORD);
    fillMap(ATTRIBUTES, OPERATORS, HaxeSyntaxHighlighterColors.OPERATION_SIGN);

    ATTRIBUTES.put(LITINT, HaxeSyntaxHighlighterColors.NUMBER);
    ATTRIBUTES.put(LITHEX, HaxeSyntaxHighlighterColors.NUMBER);
    ATTRIBUTES.put(LITOCT, HaxeSyntaxHighlighterColors.NUMBER);
    ATTRIBUTES.put(KFALSE, HaxeSyntaxHighlighterColors.NUMBER);
    ATTRIBUTES.put(KTRUE, HaxeSyntaxHighlighterColors.NUMBER);
    ATTRIBUTES.put(LITFLOAT, HaxeSyntaxHighlighterColors.NUMBER);

    ATTRIBUTES.put(LITSTRING, HaxeSyntaxHighlighterColors.STRING);
    ATTRIBUTES.put(LITCHAR, HaxeSyntaxHighlighterColors.STRING);

    ATTRIBUTES.put(PLPAREN, HaxeSyntaxHighlighterColors.PARENTHS);
    ATTRIBUTES.put(PRPAREN, HaxeSyntaxHighlighterColors.PARENTHS);

    ATTRIBUTES.put(PLCURLY, HaxeSyntaxHighlighterColors.BRACES);
    ATTRIBUTES.put(PRCURLY, HaxeSyntaxHighlighterColors.BRACES);

    ATTRIBUTES.put(PLBRACK, HaxeSyntaxHighlighterColors.BRACKETS);
    ATTRIBUTES.put(PRBRACK, HaxeSyntaxHighlighterColors.BRACKETS);

    ATTRIBUTES.put(OCOMMA, HaxeSyntaxHighlighterColors.COMMA);
    ATTRIBUTES.put(ODOT, HaxeSyntaxHighlighterColors.DOT);
    ATTRIBUTES.put(OSEMI, HaxeSyntaxHighlighterColors.SEMICOLON);

    ATTRIBUTES.put(MML_COMMENT, HaxeSyntaxHighlighterColors.BLOCK_COMMENT);
    ATTRIBUTES.put(MSL_COMMENT, HaxeSyntaxHighlighterColors.LINE_COMMENT);
    ATTRIBUTES.put(DOC_COMMENT, HaxeSyntaxHighlighterColors.DOC_COMMENT);

    fillMap(ATTRIBUTES, BAD_TOKENS, HaxeSyntaxHighlighterColors.BAD_CHARACTER);
  }

  @NotNull
  public Lexer getHighlightingLexer() {
    return new HaxeLexer();
  }

  @NotNull
  public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
    return pack(ATTRIBUTES.get(tokenType));
  }
}

