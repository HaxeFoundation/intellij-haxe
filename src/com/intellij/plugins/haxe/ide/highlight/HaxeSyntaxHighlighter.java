package com.intellij.plugins.haxe.ide.highlight;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.plugins.haxe.lang.lexer.HaxeLexer;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.*;

public class HaxeSyntaxHighlighter extends SyntaxHighlighterBase implements HaxeTokenTypes {

  private static final Map<IElementType, TextAttributesKey> ATTRIBUTES = new HashMap<IElementType, TextAttributesKey>();

  public static final String DEFAULT_SETTINGS_ID = "haxe.default";
  public static final String LINE_COMMENT_ID = "haxe.line.comment";
  public static final String BLOCK_COMMENT_ID = "haxe.block.comment";
  public static final String STRING_ID = "haxe.string";
  public static final String NUMBER_ID = "haxe.number";
  public static final String KEYWORD_ID = "haxe.keyword";
  public static final String IDENTIFIER_ID = "haxe.identifier";

  @NonNls
  public static final String BRACKETS_ID = "haxe.brackets";

  @NonNls
  public static final String OPERATOR_ID = "haxe.operators";


  public static final String TYPE_NAME_ID = "haxe.type.name";

  public static final String VARIABLE_ID = "haxe.variable";

  private static final TextAttributes DEFAULT_ATTRIB = new TextAttributes(Color.black, null, null, null, Font.PLAIN);

  public static final TextAttributesKey LINE_COMMENT = TextAttributesKey.createTextAttributesKey(LINE_COMMENT_ID,
                                                                                                 new TextAttributes(new Color(128, 128, 0),
                                                                                                                    null, null, null,
                                                                                                                    Font.PLAIN));

  public static final TextAttributesKey BLOCK_COMMENT = TextAttributesKey.createTextAttributesKey(BLOCK_COMMENT_ID,
                                                                                                  new TextAttributes(new Color(128, 128, 0),
                                                                                                                     null, null, null,
                                                                                                                     Font.PLAIN));

  public static final TextAttributesKey KEYWORD = TextAttributesKey.createTextAttributesKey(KEYWORD_ID,
                                                                                            new TextAttributes(Color.blue, null, null, null,
                                                                                                               Font.BOLD));

  public static final TextAttributesKey STRING = TextAttributesKey.createTextAttributesKey(STRING_ID,
                                                                                           new TextAttributes(Color.red, null, null, null,
                                                                                                              Font.PLAIN));

  public static final TextAttributesKey IDENTIFIER = TextAttributesKey.createTextAttributesKey(IDENTIFIER_ID, DEFAULT_ATTRIB.clone());

  public static final TextAttributesKey NUMBER = TextAttributesKey.createTextAttributesKey(NUMBER_ID,
                                                                                           TextAttributes.merge(DEFAULT_ATTRIB,
                                                                                                                new TextAttributes(
                                                                                                                  Color.gray, null, null,
                                                                                                                  null, Font.PLAIN)));

  public static final TextAttributesKey TYPE_NAME = TextAttributesKey.createTextAttributesKey(TYPE_NAME_ID,
                                                                                              new TextAttributes(new Color(0, 128, 0), null,
                                                                                                                 null, null, Font.BOLD));

  public static final TextAttributesKey VARIABLE = TextAttributesKey.createTextAttributesKey(VARIABLE_ID,
                                                                                             new TextAttributes(Color.gray, null, null,
                                                                                                                null, Font.BOLD));

  public static final TextAttributesKey BRACKET = TextAttributesKey.createTextAttributesKey(BRACKETS_ID, DEFAULT_ATTRIB.clone());

  public static final TextAttributesKey OPERATOR = TextAttributesKey.createTextAttributesKey(OPERATOR_ID, DEFAULT_ATTRIB.clone());

  public static final TextAttributesKey DEFAULT = TextAttributesKey.createTextAttributesKey(DEFAULT_SETTINGS_ID);

  static {
    fillMap(ATTRIBUTES, LINE_COMMENTS, LINE_COMMENT);
    fillMap(ATTRIBUTES, BLOCK_COMMENTS, BLOCK_COMMENT);
    fillMap(ATTRIBUTES, KEYWORDS, KEYWORD);
    fillMap(ATTRIBUTES, NUMBERS, NUMBER);
    fillMap(ATTRIBUTES, STRINGS, STRING);
    fillMap(ATTRIBUTES, TokenSet.create(PLPAREN, PRPAREN), BRACKET);
    fillMap(ATTRIBUTES, TokenSet.create(PLBRACK, PRBRACK), BRACKET);
    fillMap(ATTRIBUTES, TokenSet.create(PLCURLY, PRCURLY), BRACKET);
    fillMap(ATTRIBUTES, OPERATORS, OPERATOR);
    fillMap(ATTRIBUTES, IDENTIFIERS, IDENTIFIER);
    fillMap(ATTRIBUTES, BAD_TOKENS, CodeInsightColors.ERRORS_ATTRIBUTES);
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

