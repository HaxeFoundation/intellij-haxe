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
package com.intellij.plugins.haxe.ide;

import com.intellij.lang.BracePair;
import com.intellij.lang.PairedBraceMatcher;
import com.intellij.plugins.haxe.lang.lexer.HaxeElementType;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeBraceMatcher implements PairedBraceMatcher {

  private static final BracePair[] PAIRS = new BracePair[]{
    new BracePair(HaxeTokenTypes.ENCLOSURE_CURLY_BRACKET_LEFT, HaxeTokenTypes.ENCLOSURE_CURLY_BRACKET_RIGHT, false),
    new BracePair(HaxeTokenTypes.ENCLOSURE_BRACKET_LEFT, HaxeTokenTypes.ENCLOSURE_BRACKET_RIGHT, false),
    new BracePair(HaxeTokenTypes.ENCLOSURE_PARENTHESIS_LEFT, HaxeTokenTypes.ENCLOSURE_PARENTHESIS_RIGHT, false)
  };

  @Override
  public BracePair[] getPairs() {
    return PAIRS;
  }

  @Override
  public boolean isPairedBracesAllowedBeforeType(@NotNull IElementType lbraceType, @Nullable IElementType contextType) {
    if (contextType instanceof HaxeElementType) return isPairedBracesAllowedBeforeTypeInHaxe(contextType);
    return true;
  }

  private static boolean isPairedBracesAllowedBeforeTypeInHaxe(final IElementType tokenType) {
    return HaxeTokenTypeSets.COMMENTS.contains(tokenType)
           || HaxeTokenTypeSets.WHITESPACES.contains(tokenType)
           || tokenType == HaxeTokenTypes.OPERATOR_SEMICOLON
           || tokenType == HaxeTokenTypes.OPERATOR_COMMA
           || tokenType == HaxeTokenTypes.ENCLOSURE_PARENTHESIS_RIGHT
           || tokenType == HaxeTokenTypes.ENCLOSURE_BRACKET_RIGHT
           || tokenType == HaxeTokenTypes.ENCLOSURE_CURLY_BRACKET_RIGHT
           || tokenType == HaxeTokenTypes.ENCLOSURE_CURLY_BRACKET_LEFT
           || tokenType == HaxeTokenTypes.CC_OPERATOR_DOT;
  }

  @Override
  public int getCodeConstructStart(PsiFile file, int openingBraceOffset) {
    return openingBraceOffset;
  }
}
