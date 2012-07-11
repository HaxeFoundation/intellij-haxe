package com.intellij.plugins.haxe.ide.highlight;

import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.SyntaxHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.colors.TextAttributesKeyDefaults;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.openapi.editor.markup.TextAttributes;

import java.awt.*;

import static com.intellij.openapi.editor.colors.TextAttributesKeyDefaults.createTextAttributesKey;

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
    createTextAttributesKey("HAXE_LINE_COMMENT", TextAttributesKeyDefaults.getDefaultAttributes(SyntaxHighlighterColors.LINE_COMMENT));
  public static final TextAttributesKey BLOCK_COMMENT =
    createTextAttributesKey("HAXE_BLOCK_COMMENT", TextAttributesKeyDefaults.getDefaultAttributes(SyntaxHighlighterColors.JAVA_BLOCK_COMMENT));
  public static final TextAttributesKey DOC_COMMENT =
    createTextAttributesKey("HAXE_DOC_COMMENT", TextAttributesKeyDefaults.getDefaultAttributes(SyntaxHighlighterColors.DOC_COMMENT));
  private static final Color NOT_COMPILED_COLOR = new Color(56, 73, 46);
  public static final TextAttributesKey CONDITIONALLY_NOT_COMPILED =
    createTextAttributesKey("HAXE_CONDITIONALLY_NOT_COMPILED", new TextAttributes(NOT_COMPILED_COLOR, null, null, null, Font.PLAIN));
  public static final TextAttributesKey DEFINED_VAR =
      createTextAttributesKey("HAXE_DEFINED_VAR", new TextAttributes(NOT_COMPILED_COLOR, null, null, null, Font.BOLD));
  public static final TextAttributesKey UNDEFINED_VAR =
      createTextAttributesKey("HAXE_UNDEFINED_VAR", new TextAttributes(NOT_COMPILED_COLOR, null, null, null, Font.PLAIN));
  public static final TextAttributesKey KEYWORD =
    createTextAttributesKey("HAXE_KEYWORD", TextAttributesKeyDefaults.getDefaultAttributes(SyntaxHighlighterColors.KEYWORD));
  public static final TextAttributesKey NUMBER =
    createTextAttributesKey("HAXE_NUMBER", TextAttributesKeyDefaults.getDefaultAttributes(SyntaxHighlighterColors.NUMBER));
  public static final TextAttributesKey STRING =
    createTextAttributesKey("HAXE_STRING", TextAttributesKeyDefaults.getDefaultAttributes(SyntaxHighlighterColors.STRING));
  public static final TextAttributesKey OPERATION_SIGN =
    createTextAttributesKey("HAXE_OPERATION_SIGN", TextAttributesKeyDefaults.getDefaultAttributes(SyntaxHighlighterColors.OPERATION_SIGN));
  public static final TextAttributesKey PARENTHS =
    createTextAttributesKey("HAXE_PARENTH", TextAttributesKeyDefaults.getDefaultAttributes(SyntaxHighlighterColors.PARENTHS));
  public static final TextAttributesKey BRACKETS =
    createTextAttributesKey("HAXE_BRACKETS", TextAttributesKeyDefaults.getDefaultAttributes(SyntaxHighlighterColors.BRACKETS));
  public static final TextAttributesKey BRACES =
    createTextAttributesKey("HAXE_BRACES", TextAttributesKeyDefaults.getDefaultAttributes(SyntaxHighlighterColors.BRACES));
  public static final TextAttributesKey COMMA = createTextAttributesKey("HAXE_COMMA", TextAttributesKeyDefaults
    .getDefaultAttributes(SyntaxHighlighterColors.COMMA));
  public static final TextAttributesKey DOT = createTextAttributesKey("HAXE_DOT", TextAttributesKeyDefaults
    .getDefaultAttributes(SyntaxHighlighterColors.DOT));
  public static final TextAttributesKey SEMICOLON =
    createTextAttributesKey("HAXE_SEMICOLON", TextAttributesKeyDefaults.getDefaultAttributes(SyntaxHighlighterColors.JAVA_SEMICOLON));
  public static final TextAttributesKey BAD_CHARACTER =
    createTextAttributesKey("HAXE_BAD_CHARACTER", TextAttributesKeyDefaults.getDefaultAttributes(HighlighterColors.BAD_CHARACTER));
  public static final TextAttributesKey CLASS =
    createTextAttributesKey(HAXE_CLASS, TextAttributesKeyDefaults.getDefaultAttributes(HighlightInfoType.CLASS_NAME.getAttributesKey()));
  public static final TextAttributesKey INTERFACE =
    createTextAttributesKey(HAXE_INTERFACE, TextAttributesKeyDefaults
      .getDefaultAttributes(HighlightInfoType.INTERFACE_NAME.getAttributesKey()));
  public static final TextAttributesKey STATIC_MEMBER_FUNCTION =
    createTextAttributesKey(HAXE_STATIC_MEMBER_FUNCTION, TextAttributesKeyDefaults
      .getDefaultAttributes(HighlightInfoType.STATIC_METHOD.getAttributesKey()));
  public static final TextAttributesKey INSTANCE_MEMBER_FUNCTION =
    createTextAttributesKey(HAXE_INSTANCE_MEMBER_FUNCTION, new TextAttributes(new Color(0x7a, 0x7a, 43), Color.white, null, null, 0));
  public static final TextAttributesKey INSTANCE_MEMBER_VARIABLE =
    createTextAttributesKey(HAXE_INSTANCE_MEMBER_VARIABLE, TextAttributesKeyDefaults
      .getDefaultAttributes(HighlightInfoType.INSTANCE_FIELD.getAttributesKey()));
  public static final TextAttributesKey STATIC_MEMBER_VARIABLE =
    createTextAttributesKey(HAXE_STATIC_MEMBER_VARIABLE, TextAttributesKeyDefaults
      .getDefaultAttributes(HighlightInfoType.STATIC_FIELD.getAttributesKey()));
  public static final TextAttributesKey LOCAL_VARIABLE =
    createTextAttributesKey(HAXE_LOCAL_VARIABLE, new TextAttributes(new Color(69, 131, 131), Color.white, null, null, 0));
  public static final TextAttributesKey PARAMETER =
    createTextAttributesKey(HAXE_PARAMETER, new TextAttributes(Color.black, Color.white, Color.black, EffectType.LINE_UNDERSCORE, 0));
}
