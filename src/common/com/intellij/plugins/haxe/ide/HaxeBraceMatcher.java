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
    new BracePair(HaxeTokenTypes.PLCURLY, HaxeTokenTypes.PRCURLY, false),
    new BracePair(HaxeTokenTypes.PLBRACK, HaxeTokenTypes.PRBRACK, false),
    new BracePair(HaxeTokenTypes.PLPAREN, HaxeTokenTypes.PRPAREN, false)
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
           || tokenType == HaxeTokenTypes.OSEMI
           || tokenType == HaxeTokenTypes.OCOMMA
           || tokenType == HaxeTokenTypes.PRPAREN
           || tokenType == HaxeTokenTypes.PRBRACK
           || tokenType == HaxeTokenTypes.PRCURLY
           || tokenType == HaxeTokenTypes.PLCURLY
           || tokenType == HaxeTokenTypes.ODOT;
  }

  @Override
  public int getCodeConstructStart(PsiFile file, int openingBraceOffset) {
    return openingBraceOffset;
  }
}
