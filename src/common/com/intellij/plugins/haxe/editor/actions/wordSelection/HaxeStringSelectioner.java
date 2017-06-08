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

import com.intellij.codeInsight.editorActions.ExtendWordSelectionHandlerBase;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.HaxePsiToken;
import com.intellij.psi.PsiElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ebishton on 6/6/17.
 */
public class HaxeStringSelectioner extends ExtendWordSelectionHandlerBase {

  @Override
  public boolean canSelect(PsiElement e) {
    return (e instanceof HaxePsiToken
            && e.getLanguage().equals(HaxeLanguage.INSTANCE)
            && HaxeTokenTypes.REGULAR_STRING_PART.equals(((HaxePsiToken)e).getTokenType()));
  }

  @Override
  public List<TextRange> select(PsiElement e, CharSequence editorText, int cursorOffset, Editor editor) {
    final TextRange originalRange = e.getTextRange();

    // For the error condition, let the superclass log the standard error and throw an exception.
    if (originalRange.getEndOffset() > editorText.length()) {
      super.select(e, editorText, cursorOffset, editor);
    }

    // XXX: Check for the range actually being in the element?  Shouldn't happen.

    final List<TextRange> ranges = new ArrayList<TextRange>(1);
    ranges.add(SelectionUtil.selectWord(e, cursorOffset));
    return ranges;
  }

}
