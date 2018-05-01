/*
 * Copyright 2017 Eric Bishton
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
package com.intellij.plugins.haxe.editor.actions.wordSelection;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;

/**
 * Created by ebishton on 6/6/17.
 */
public class SelectionUtil {

  private SelectionUtil() {}

  static public boolean isWordDelimiter(char c) {
    return !(Character.isAlphabetic(c) || Character.isDigit(c));
  }

  static public boolean isWhitespace(char c) {
    return Character.isWhitespace(c);
  }

  /**
   * Selects the word under the caret, using the definition of isWordDelimiter.
   *
   * @param e
   * @param offset
   * @return
   */
  static public TextRange selectWord(PsiElement e, int offset) {
    return findTokenLimits(e, offset, false);
  }

  /**
   * Selects the token under the caret, using only whitespace as a delimiter.
   *
   * @param e
   * @param offset
   * @return
   */
  static public TextRange selectToken(PsiElement e, int offset) {
    return findTokenLimits(e, offset, true);
  }

  static private boolean isDelimiter(char c, boolean useWhitespace) {
    return useWhitespace ? isWhitespace(c) : isWordDelimiter(c);
  }

  static private TextRange findTokenLimits(PsiElement e, int offset, boolean useWhitespace) {
    final int tokenOffset = e.getTextOffset();
    int startPos = offset - tokenOffset;
    int endPos = startPos;
    final String text = e.getText();
    final int length = text.length();

    // While scanning for the beginning and end of a word, we don't have to
    // worry about escaped characters ("\n") because they are split apart into
    // separate REGULAR_STRING_PART lexemes.  Thus, they are automatically string
    // separators.

    // Scan backward for the start of the word.  If the offset is a space, then
    // we want the word before the startPos.
    while (startPos > 0 && !isDelimiter(text.charAt(startPos - 1), useWhitespace)) {
      --startPos;
    }

    // Scan forward to find the end of the word.  If this offset is whitespace, then
    // we are done.
    while (endPos < length && !isDelimiter(text.charAt(endPos), useWhitespace)) {
      ++endPos;
    }
    return new TextRange(startPos + tokenOffset, endPos + tokenOffset);
  }

}
