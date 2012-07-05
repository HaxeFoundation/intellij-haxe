package com.intellij.plugins.haxe.ide.highlight;

import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.SyntaxHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;

import java.awt.*;

import static com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey;

/**
 * @author fedor.korotkov
 */
public class HaxeSyntaxHighlighterColors {
  public static final String HAXE_KEYWORD = "HAXE_KEYWORD";
  public static final String HAXE_CLASS = "HAXE_CLASS";
  public static final String HAXE_INTERFACE = "HAXE_INTERFACE";
  public static final String HAXE_STATIC_MEMBER_FUNCTION = "HAXE_STATIC_MEMBER_FUNCTION";
  public static final String HAXE_INSTANCE_MEMBER_FUNCTION = "HAXE_INSTANCE_MEMBER_FUNCTION";
  public static final String HAXE_INSTANCE_MEMBER_VARIABLE = "HAXE_INSTANCE_MEMBER_VARIABLE";
  public static final String HAXE_STATIC_MEMBER_VARIABLE = "HAXE_STATIC_MEMBER_VARIABLE";
  public static final String HAXE_LOCAL_VARIABLE = "HAXE_LOCAL_VARIABLE";
  public static final String HAXE_PARAMETER = "HAXE_PARAMETER";
  public static final String HAXE_DEFINED_VAR = "HAXE_DEFINED_VAR";
  public static final String HAXE_UNDEFINED_VAR = "HAXE_UNDEFINED_VAR";

  public static final TextAttributesKey LINE_COMMENT =
    createTextAttributesKey("HAXE_LINE_COMMENT", SyntaxHighlighterColors.LINE_COMMENT.getDefaultAttributes());
  public static final TextAttributesKey BLOCK_COMMENT =
    createTextAttributesKey("HAXE_BLOCK_COMMENT", SyntaxHighlighterColors.JAVA_BLOCK_COMMENT.getDefaultAttributes());
  public static final TextAttributesKey DOC_COMMENT =
    createTextAttributesKey("HAXE_DOC_COMMENT", SyntaxHighlighterColors.DOC_COMMENT.getDefaultAttributes());
  private static final Color NOT_COMPILED_COLOR = new Color(56, 73, 46);
  public static final TextAttributesKey CONDITIONALLY_NOT_COMPILED =
    createTextAttributesKey("HAXE_CONDITIONALLY_NOT_COMPILED", new TextAttributes(NOT_COMPILED_COLOR, null, null, null, Font.PLAIN));
  public static final TextAttributesKey DEFINED_VAR =
      createTextAttributesKey("HAXE_DEFINED_VAR", new TextAttributes(NOT_COMPILED_COLOR, null, null, null, Font.BOLD));
  public static final TextAttributesKey UNDEFINED_VAR =
      createTextAttributesKey("HAXE_UNDEFINED_VAR", new TextAttributes(NOT_COMPILED_COLOR, null, null, null, Font.PLAIN));
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
  public static final TextAttributesKey CLASS =
    createTextAttributesKey(HAXE_CLASS, HighlightInfoType.CLASS_NAME.getAttributesKey().getDefaultAttributes());
  public static final TextAttributesKey INTERFACE =
    createTextAttributesKey(HAXE_INTERFACE, HighlightInfoType.INTERFACE_NAME.getAttributesKey().getDefaultAttributes());
  public static final TextAttributesKey STATIC_MEMBER_FUNCTION =
    createTextAttributesKey(HAXE_STATIC_MEMBER_FUNCTION, HighlightInfoType.STATIC_METHOD.getAttributesKey().getDefaultAttributes());
  public static final TextAttributesKey INSTANCE_MEMBER_FUNCTION =
    createTextAttributesKey(HAXE_INSTANCE_MEMBER_FUNCTION, new TextAttributes(new Color(0x7a, 0x7a, 43), Color.white, null, null, 0));
  public static final TextAttributesKey INSTANCE_MEMBER_VARIABLE =
    createTextAttributesKey(HAXE_INSTANCE_MEMBER_VARIABLE, HighlightInfoType.INSTANCE_FIELD.getAttributesKey().getDefaultAttributes());
  public static final TextAttributesKey STATIC_MEMBER_VARIABLE =
    createTextAttributesKey(HAXE_STATIC_MEMBER_VARIABLE, HighlightInfoType.STATIC_FIELD.getAttributesKey().getDefaultAttributes());
  public static final TextAttributesKey LOCAL_VARIABLE =
    createTextAttributesKey(HAXE_LOCAL_VARIABLE, new TextAttributes(new Color(69, 131, 131), Color.white, null, null, 0));
  public static final TextAttributesKey PARAMETER =
    createTextAttributesKey(HAXE_PARAMETER, new TextAttributes(Color.black, Color.white, Color.black, EffectType.LINE_UNDERSCORE, 0));
}
