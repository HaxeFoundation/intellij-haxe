/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
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

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.lexer.HaxeLexerAdapter;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;

/**
 * Highlights *lexer* tokens. (Not PSI elements!) It does not highlight parsed entities, per se.
 */
public class HaxeSyntaxHighlighter extends SyntaxHighlighterBase {
  private static final Map<IElementType, TextAttributesKey> ATTRIBUTES = new HashMap<IElementType, TextAttributesKey>();
  private Project myProject;

  public HaxeSyntaxHighlighter(Project project) {
    myProject = project;
  }

  static {
    fillMap(ATTRIBUTES, HaxeTokenTypeSets.KEYWORDS, HaxeSyntaxHighlighterColors.KEYWORD);
    fillMap(ATTRIBUTES, HaxeTokenTypeSets.OPERATORS, HaxeSyntaxHighlighterColors.OPERATION_SIGN);

    ATTRIBUTES.put(LITERAL_INTEGER, HaxeSyntaxHighlighterColors.NUMBER);
    ATTRIBUTES.put(LITERAL_HEXADECIMAL, HaxeSyntaxHighlighterColors.NUMBER);
    ATTRIBUTES.put(LITERAL_OCTAL, HaxeSyntaxHighlighterColors.NUMBER);
    ATTRIBUTES.put(KEYWORD_FALSE, HaxeSyntaxHighlighterColors.NUMBER);
    ATTRIBUTES.put(KEYWORD_TRUE, HaxeSyntaxHighlighterColors.NUMBER);
    ATTRIBUTES.put(LITERAL_FLOAT, HaxeSyntaxHighlighterColors.NUMBER);

    ATTRIBUTES.put(OPEN_QUOTE, HaxeSyntaxHighlighterColors.STRING);
    ATTRIBUTES.put(CLOSING_QUOTE, HaxeSyntaxHighlighterColors.STRING);
    ATTRIBUTES.put(REGULAR_STRING_PART, HaxeSyntaxHighlighterColors.STRING);

    ATTRIBUTES.put(ENCLOSURE_PARENTHESIS_LEFT, HaxeSyntaxHighlighterColors.PARENTHS);
    ATTRIBUTES.put(ENCLOSURE_PARENTHESIS_RIGHT, HaxeSyntaxHighlighterColors.PARENTHS);

    ATTRIBUTES.put(ENCLOSURE_CURLY_BRACKET_LEFT, HaxeSyntaxHighlighterColors.BRACES);
    ATTRIBUTES.put(ENCLOSURE_CURLY_BRACKET_RIGHT, HaxeSyntaxHighlighterColors.BRACES);

    ATTRIBUTES.put(ENCLOSURE_BRACKET_LEFT, HaxeSyntaxHighlighterColors.BRACKETS);
    ATTRIBUTES.put(ENCLOSURE_BRACKET_RIGHT, HaxeSyntaxHighlighterColors.BRACKETS);

    ATTRIBUTES.put(OPERATOR_COMMA, HaxeSyntaxHighlighterColors.COMMA);
    ATTRIBUTES.put(CC_OPERATOR_DOT, HaxeSyntaxHighlighterColors.DOT);
    ATTRIBUTES.put(OPERATOR_SEMICOLON, HaxeSyntaxHighlighterColors.SEMICOLON);

    ATTRIBUTES.put(HaxeTokenTypeSets.BLOCK_COMMENT, HaxeSyntaxHighlighterColors.BLOCK_COMMENT);
    ATTRIBUTES.put(HaxeTokenTypeSets.LINE_COMMENT, HaxeSyntaxHighlighterColors.LINE_COMMENT);
    ATTRIBUTES.put(HaxeTokenTypeSets.DOC_COMMENT, HaxeSyntaxHighlighterColors.DOC_COMMENT);

    fillMap(ATTRIBUTES, HaxeTokenTypeSets.BAD_TOKENS, HaxeSyntaxHighlighterColors.BAD_CHARACTER);
    fillMap(ATTRIBUTES, HaxeTokenTypeSets.CONDITIONALLY_NOT_COMPILED, HaxeSyntaxHighlighterColors.CONDITIONALLY_NOT_COMPILED);

    //ATTRIBUTES.put(HaxeMetadataTokenTypes.CT_META_PREFIX,  HaxeSyntaxHighlighterColors.METADATA);
    //ATTRIBUTES.put(HaxeMetadataTokenTypes.RT_META_PREFIX, HaxeSyntaxHighlighterColors.METADATA);
    //ATTRIBUTES.put(HaxeMetadataTokenTypes.META_TYPE, HaxeSyntaxHighlighterColors.METADATA);
    //ATTRIBUTES.put(HaxeMetadataTokenTypes.PLPAREN, HaxeSyntaxHighlighterColors.METADATA);
    //ATTRIBUTES.put(HaxeMetadataTokenTypes.PRPAREN,  HaxeSyntaxHighlighterColors.METADATA);
    //ATTRIBUTES.put(HaxeMetadataTokenTypes.INVALID_META_CHARACTER, HaxeSyntaxHighlighterColors.BAD_CHARACTER);
    //ATTRIBUTES.put(HaxeMetadataTokenTypes.EXTRA_DATA, HaxeSyntaxHighlighterColors.BLOCK_COMMENT);
  }

  @NotNull
  public Lexer getHighlightingLexer() {
    return new HaxeLexerAdapter(myProject);
  }

  @NotNull
  public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
    return pack(ATTRIBUTES.get(tokenType));
  }
}

