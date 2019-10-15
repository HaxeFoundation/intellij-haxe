/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.ide.highlight;

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;

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
  public static final String HAXE_UNPARSEABLE_DATA = "HAXE_UNPARSEABLE_DATA";

  public static final TextAttributesKey LINE_COMMENT =
    createTextAttributesKey("HAXE_LINE_COMMENT", DefaultLanguageHighlighterColors.LINE_COMMENT);
  public static final TextAttributesKey BLOCK_COMMENT =
    createTextAttributesKey("HAXE_BLOCK_COMMENT", DefaultLanguageHighlighterColors.BLOCK_COMMENT);
  public static final TextAttributesKey DOC_COMMENT =
    createTextAttributesKey("HAXE_DOC_COMMENT", DefaultLanguageHighlighterColors.DOC_COMMENT);

  public static final TextAttributesKey DEFINED_VAR = createTextAttributesKey("HAXE_DEFINED_VAR");
  public static final TextAttributesKey UNDEFINED_VAR = createTextAttributesKey("HAXE_UNDEFINED_VAR");
  public static final TextAttributesKey CONDITIONALLY_NOT_COMPILED = createTextAttributesKey("HAXE_CONDITIONALLY_NOT_COMPILED");
  public static final TextAttributesKey UNPARSEABLE_DATA = createTextAttributesKey(HAXE_UNPARSEABLE_DATA);

  public static final TextAttributesKey KEYWORD =
    createTextAttributesKey("HAXE_KEYWORD", DefaultLanguageHighlighterColors.KEYWORD);
  public static final TextAttributesKey NUMBER =
    createTextAttributesKey("HAXE_NUMBER", DefaultLanguageHighlighterColors.NUMBER);
  public static final TextAttributesKey STRING =
    createTextAttributesKey("HAXE_STRING", DefaultLanguageHighlighterColors.STRING);
  public static final TextAttributesKey OPERATION_SIGN =
    createTextAttributesKey("HAXE_OPERATION_SIGN", DefaultLanguageHighlighterColors.OPERATION_SIGN);
  public static final TextAttributesKey PARENTHS =
    createTextAttributesKey("HAXE_PARENTH", DefaultLanguageHighlighterColors.PARENTHESES);
  public static final TextAttributesKey BRACKETS =
    createTextAttributesKey("HAXE_BRACKETS", DefaultLanguageHighlighterColors.BRACKETS);
  public static final TextAttributesKey BRACES =
    createTextAttributesKey("HAXE_BRACES", DefaultLanguageHighlighterColors.BRACES);
  public static final TextAttributesKey COMMA = createTextAttributesKey("HAXE_COMMA", DefaultLanguageHighlighterColors.COMMA);
  public static final TextAttributesKey DOT = createTextAttributesKey("HAXE_DOT", DefaultLanguageHighlighterColors.DOT);
  public static final TextAttributesKey SEMICOLON =
    createTextAttributesKey("HAXE_SEMICOLON", DefaultLanguageHighlighterColors.SEMICOLON);
  public static final TextAttributesKey BAD_CHARACTER =
    createTextAttributesKey("HAXE_BAD_CHARACTER", HighlighterColors.BAD_CHARACTER);
  public static final TextAttributesKey CLASS =
    createTextAttributesKey(HAXE_CLASS, DefaultLanguageHighlighterColors.CLASS_NAME);
  public static final TextAttributesKey INTERFACE =
    createTextAttributesKey(HAXE_INTERFACE, DefaultLanguageHighlighterColors.INTERFACE_NAME);
  public static final TextAttributesKey STATIC_MEMBER_FUNCTION =
    createTextAttributesKey(HAXE_STATIC_MEMBER_FUNCTION, DefaultLanguageHighlighterColors.STATIC_METHOD);
  public static final TextAttributesKey INSTANCE_MEMBER_FUNCTION =
    createTextAttributesKey(HAXE_INSTANCE_MEMBER_FUNCTION, DefaultLanguageHighlighterColors.INSTANCE_METHOD);
  public static final TextAttributesKey INSTANCE_MEMBER_VARIABLE =
    createTextAttributesKey(HAXE_INSTANCE_MEMBER_VARIABLE, DefaultLanguageHighlighterColors.INSTANCE_FIELD);
  public static final TextAttributesKey STATIC_MEMBER_VARIABLE =
    createTextAttributesKey(HAXE_STATIC_MEMBER_VARIABLE, DefaultLanguageHighlighterColors.STATIC_FIELD);
  public static final TextAttributesKey LOCAL_VARIABLE =
    createTextAttributesKey(HAXE_LOCAL_VARIABLE, DefaultLanguageHighlighterColors.LOCAL_VARIABLE);
  public static final TextAttributesKey PARAMETER =
    createTextAttributesKey(HAXE_PARAMETER, DefaultLanguageHighlighterColors.PARAMETER);
}
