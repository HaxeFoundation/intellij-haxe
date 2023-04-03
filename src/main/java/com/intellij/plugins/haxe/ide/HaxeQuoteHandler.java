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

import com.intellij.codeInsight.editorActions.SimpleTokenSetQuoteHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;

/**
 * @author fedor.korotkov
 */
public class HaxeQuoteHandler extends SimpleTokenSetQuoteHandler {
  public HaxeQuoteHandler() {
    super(HaxeTokenTypes.OPEN_QUOTE, HaxeTokenTypes.CLOSING_QUOTE);
  }

  @Override
  public boolean isClosingQuote(HighlighterIterator iterator, int offset) {
    return HaxeTokenTypes.CLOSING_QUOTE == iterator.getTokenType()
        && offset == iterator.getEnd() - 1;
  }

  @Override
  public boolean isOpeningQuote(HighlighterIterator iterator, int offset) {
    return HaxeTokenTypes.OPEN_QUOTE == iterator.getTokenType()
        && offset == iterator.getStart();
  }

  @Override
  public boolean hasNonClosedLiteral(Editor editor, HighlighterIterator iterator, int offset) {
    return super.hasNonClosedLiteral(editor, iterator, offset);
  }

  @Override
  protected boolean isNonClosedLiteral(HighlighterIterator iterator, CharSequence chars) {
    return super.isNonClosedLiteral(iterator, chars);
  }

  @Override
  public boolean isInsideLiteral(HighlighterIterator iterator) {
    return super.isInsideLiteral(iterator);
  }
}
