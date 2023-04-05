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
package com.intellij.plugins.haxe.editor.smartEnter;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.HaxeExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeIfStatement;
import com.intellij.plugins.haxe.util.HaxeCodeGenerateUtil;
import com.intellij.plugins.haxe.util.HaxeRangeUtil;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;

/**
 * Created by as3boyan on 06.10.14.
 */
public class IfConditionFixer implements Fixer {

  public static final String BLOCK_BEGIN = "{";
  public static final String BLOCK_END = "}";
  public static final String GUARD_BEGIN = "(";
  public static final String GUARD_END = ")";
  public static final String GUARD_DEFAULT_CONTENT = "false";

  @Override
  public void apply(Editor editor, HaxeSmartEnterProcessor processor, PsiElement psiElement) throws IncorrectOperationException {
    if (psiElement instanceof HaxeIfStatement) {
      // It's already a fully parsed and complete statement.  Nothing to do.
      return;
    }
    boolean changed = false;
    if (psiElement.getNode().getElementType() == HaxeTokenTypes.KEYWORD_IF) {
      changed = fixIfStatement(editor, psiElement);
    }
    if (isExpression(psiElement)) {
      PsiElement prev = UsefulPsiTreeUtil.getPrevSiblingSkipWhiteSpacesAndComments(psiElement, true);
      if (null != prev && prev.getNode().getElementType() == HaxeTokenTypes.KEYWORD_IF) {
        changed |= wrapExpressionInParens(editor, psiElement);
        changed |= fixIfStatement(editor, prev);
      }
    }
    processor.setSkipEnter(changed);
  }

  private boolean isExpression(PsiElement psiElement) {
    return psiElement instanceof HaxeExpression;
  }

  private boolean wrapExpressionInParens(Editor editor, PsiElement expression) {
    return HaxeCodeGenerateUtil.addMissingDelimiters(editor, expression, HaxeTokenTypes.ENCLOSURE_PARENTHESIS_LEFT,
                                                     HaxeTokenTypes.ENCLOSURE_PARENTHESIS_RIGHT);
  }

  private boolean fixIfStatement(Editor editor, PsiElement ifKeyword) {
    if (null == ifKeyword) {
      return false;
    }

    Document doc = editor.getDocument();
    Pair<PsiElement,PsiElement> parens = HaxeCodeGenerateUtil.getParens(HaxeCodeGenerateUtil.Position.FOLLOWING, ifKeyword);
    Pair<PsiElement,PsiElement> block = HaxeCodeGenerateUtil.getCurlyBrackets(HaxeCodeGenerateUtil.Position.FOLLOWING,
                                                                              parens.second != null ? parens.second : ifKeyword);

    boolean hasGuard = parens.first != null || parens.second != null;
    // Use this with partial block fixup code...
    //   boolean hasBlock = block.first != null || block.second != null;
    // instead of...
    boolean hasBlock = block.first != null && block.second != null;

    TextRange guardRange = HaxeRangeUtil.getCombinedRange(parens.first, parens.second);
    TextRange blockRange = HaxeRangeUtil.getCombinedRange(block.first, block.second);
    boolean changed = false;

    // Work from the end of the statement toward the beginning, so that we don't have to recalculate offsets.

    if (!hasBlock) {
      int offset = hasGuard ? guardRange.getEndOffset() : ifKeyword.getTextRange().getEndOffset();
      String insert = BLOCK_BEGIN + BLOCK_END;
      doc.insertString(offset, insert);
      blockRange = TextRange.from(offset, insert.length());
      changed = true;
      editor.getCaretModel().moveToOffset(offset + BLOCK_BEGIN.length());
    }
    // A bunch of issues with this around detecting the unparsed(failed,dummy) elements after the block and not including
    // *their* blocks as part of the statement we are fixing.  Otherwise, the code works.
    //else {
    //  if (null == block.second) {
    //    // TODO: Get fancy and try to find the end of the block... (e.g. scan for ';'?)
    //    int offset = block.first.getTextRange().getEndOffset();
    //    doc.insertString(offset, BLOCK_END);
    //    blockRange = blockRange.grown(BLOCK_END.length());
    //    changed = true;
    //  }
    //  else if (null == block.first) {
    //    int offset = hasGuard ? guardRange.getEndOffset() : ifKeyword.getTextRange().getEndOffset();
    //    doc.insertString(offset, BLOCK_BEGIN);
    //    blockRange = TextRange.create(offset, blockRange.getEndOffset() + BLOCK_BEGIN.length());
    //    changed = true;
    //  }
    //}

    if (!hasGuard) {
      int offset = ifKeyword.getTextRange().getEndOffset();
      String insert = GUARD_BEGIN + GUARD_DEFAULT_CONTENT + GUARD_END;
      doc.insertString(offset, insert);
      int newpos = offset + GUARD_BEGIN.length();
      editor.getCaretModel().moveToOffset(newpos);
      editor.getCaretModel().getPrimaryCaret().setSelection(newpos, newpos + GUARD_DEFAULT_CONTENT.length());
      changed = true;
    } else {
      if (null == parens.second) {
        int offset = block.first != null ? blockRange.getStartOffset() : ifKeyword.getTextRange().getEndOffset();
        doc.insertString(offset, GUARD_END);
        changed = true;
      }
      if (null == parens.first) {
        int offset =  ifKeyword.getTextRange().getEndOffset();
        doc.insertString(offset, GUARD_BEGIN);
        changed = true;
      }
    }
    return changed;
  }
}
