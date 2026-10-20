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
import com.intellij.lang.Commenter;
import com.intellij.lang.LanguageCommenters;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.ide.HaxeCommenter;
import com.intellij.plugins.haxe.lang.lexer.HaxeDocTokenTypes;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ebishton on 6/6/17.
 */
public class HaxeCommentSelectioner extends ExtendWordSelectionHandlerBase {

  /**
   * A doc comment's inner tokens. They are HaxePsiTokens - PsiJavaTokens -
   * so without this handler the java token selectioner claims each one
   * WHOLE (a double-click selected the entire line) while the java basic
   * word filter keeps the generic word selectioner away from them.
   */
  private static final TokenSet DOC_TOKENS = TokenSet.create(
    HaxeDocTokenTypes.DOC_START, HaxeDocTokenTypes.DOC_LEADING_ASTERISK, HaxeDocTokenTypes.DOC_TAG_NAME,
    HaxeDocTokenTypes.DOC_DATA, HaxeDocTokenTypes.DOC_END);

  /** The doc tokens that are delimiters rather than text; selecting one takes the line, like a comment prefix. */
  private static final TokenSet DOC_DELIMITERS = TokenSet.create(
    HaxeDocTokenTypes.DOC_START, HaxeDocTokenTypes.DOC_LEADING_ASTERISK, HaxeDocTokenTypes.DOC_END);

  @Override
  public boolean canSelect(PsiElement e) {
    IElementType type = e.getNode().getElementType();
    return e.getLanguage().equals(HaxeLanguage.INSTANCE)
           && (HaxeTokenTypeSets.ONLY_COMMENTS.contains(type) || DOC_TOKENS.contains(type));
  }

  @Override
  public List<TextRange> select(PsiElement e, CharSequence editorText, int cursorOffset, Editor editor) {
    final TextRange originalRange = e.getTextRange();

    // For the error condition, let the superclass log the standard error and throw an exception.
    if (originalRange.getEndOffset() > editorText.length()) {
      super.select(e, editorText, cursorOffset, editor);
    }

    final TextRange foundRange = SelectionUtil.selectToken(e, cursorOffset);
    final CharSequence token = editorText.subSequence(foundRange.getStartOffset(), foundRange.getEndOffset());

    final List<TextRange> ranges = new ArrayList<TextRange>(1);
    if (isCommentToken(e, token)) {
      ranges.addAll(expandToWholeLine(editorText, originalRange, true));
    } else {
      // Use the more limited defintion of a word when selecting inside of a comment.
      ranges.add(SelectionUtil.selectWord(e, cursorOffset));
    }

    return ranges;
  }

  private boolean isCommentToken(PsiElement e, CharSequence token) {
    final Commenter commenter = LanguageCommenters.INSTANCE.forLanguage(HaxeLanguage.INSTANCE);
    assert(commenter instanceof HaxeCommenter);
    final IElementType tokenType = e.getNode().getElementType();

    if (DOC_TOKENS.contains(tokenType)) {
      return DOC_DELIMITERS.contains(tokenType);
    }
    if (tokenType == HaxeTokenTypeSets.DOC_COMMENT) {

      // XXX: Should we be checking that the token is at the beginning or end of the element?
      //      Or, that the line prefix is actually the first thing on the line?
      return  ((HaxeCommenter)commenter).getDocumentationCommentLinePrefix().contentEquals(token)
              || ((HaxeCommenter)commenter).getDocumentationCommentPrefix().contentEquals(token)
              || ((HaxeCommenter)commenter).getDocumentationCommentSuffix().contentEquals(token)
              // A lot of folks don't use the proper doc comment terminator "**/", and the compiler
              // accepts a normal block comment terminator "*/".
              || commenter.getBlockCommentSuffix().contentEquals(token);

    } else if (tokenType == HaxeTokenTypeSets.MML_COMMENT) {

      return  commenter.getBlockCommentPrefix().contentEquals(token)
              || commenter.getBlockCommentSuffix().contentEquals(token);

    }
    return commenter.getLineCommentPrefix().contentEquals(token);
  }

}
