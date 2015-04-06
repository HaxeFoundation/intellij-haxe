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

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.lexer.HaxeLexer;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.*;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;

public class HaxeSyntaxHighlighter extends SyntaxHighlighterBase {
  private static final Map<IElementType, TextAttributesKey> ATTRIBUTES = new HashMap<IElementType, TextAttributesKey>();
  private Project myProject;

  public HaxeSyntaxHighlighter(Project project) {
    myProject = project;
  }

  static {
    fillMap(ATTRIBUTES, KEYWORDS, HaxeSyntaxHighlighterColors.KEYWORD);
    fillMap(ATTRIBUTES, OPERATORS, HaxeSyntaxHighlighterColors.OPERATION_SIGN);

    ATTRIBUTES.put(LITINT, HaxeSyntaxHighlighterColors.NUMBER);
    ATTRIBUTES.put(LITHEX, HaxeSyntaxHighlighterColors.NUMBER);
    ATTRIBUTES.put(LITOCT, HaxeSyntaxHighlighterColors.NUMBER);
    ATTRIBUTES.put(KFALSE, HaxeSyntaxHighlighterColors.NUMBER);
    ATTRIBUTES.put(KTRUE, HaxeSyntaxHighlighterColors.NUMBER);
    ATTRIBUTES.put(LITFLOAT, HaxeSyntaxHighlighterColors.NUMBER);

    ATTRIBUTES.put(OPEN_QUOTE, HaxeSyntaxHighlighterColors.STRING);
    ATTRIBUTES.put(CLOSING_QUOTE, HaxeSyntaxHighlighterColors.STRING);
    ATTRIBUTES.put(REGULAR_STRING_PART, HaxeSyntaxHighlighterColors.STRING);

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
    fillMap(ATTRIBUTES, CONDITIONALLY_NOT_COMPILED, HaxeSyntaxHighlighterColors.CONDITIONALLY_NOT_COMPILED);
  }

  @NotNull
  public Lexer getHighlightingLexer() {
    return new HaxeLexer(myProject);
  }

  @NotNull
  public TextAttributesKey[] getTokenHighlights(IElementType tokenType) {
    return pack(ATTRIBUTES.get(tokenType));
  }
}

