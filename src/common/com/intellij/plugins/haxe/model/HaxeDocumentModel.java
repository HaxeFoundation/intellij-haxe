/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018-2020 Eric Bishton
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
package com.intellij.plugins.haxe.model;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.util.HaxeCharUtils;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeDocumentModel {
  private Document document;
  private PsiFile file;

  public HaxeDocumentModel(Document document, PsiFile file) {
    this.document = document;
    this.file = file;
  }

  public HaxeDocumentModel(@NotNull PsiElement aElementInDocument) {
    this(PsiDocumentManager.getInstance(aElementInDocument.getProject()).getDocument(aElementInDocument.getContainingFile()),
         aElementInDocument.getContainingFile());
  }

  static public HaxeDocumentModel fromElement(@NotNull PsiElement aElementInDocument) {
    return new HaxeDocumentModel(aElementInDocument);
  }

  public Document getDocument() {
    return document;
  }

  public PsiFile getFile() {
    return file;
  }

  public void replaceElementText(final PsiElement element, final String text) {
    replaceElementText(element, text, StripSpaces.NONE);
  }

  public void replaceElementText(final TextRange textRange, final String text) {
    replaceElementText(textRange, text, StripSpaces.NONE);
  }

  public void replaceElementText(final PsiElement element, final String text, final StripSpaces strips) {
    if (element == null) return;
    replaceElementText(element.getTextRange(), text, strips);
  }

  public void replaceElementText(final TextRange range, final String text, final StripSpaces strips) {
    if (range == null) return;
    int start = range.getStartOffset();
    int end = range.getEndOffset();
    String documentText = document.getText();

    if (strips.after) {
     while (end < documentText.length() && HaxeCharUtils.isSpace(documentText.charAt(end))) {
        end++;
      }
    }
    if (strips.before) {
      while (start > 0 && HaxeCharUtils.isSpace(documentText.charAt(start - 1))) {
        start--;
      }
    }
    replaceAndFormat(new TextRange(start, end), text);
  }

  public void wrapElement(final PsiElement element, final String before, final String after) {
    wrapElement(element, before, after, StripSpaces.NONE);
  }

  public void wrapElement(final PsiElement element, final String before, final String after, StripSpaces strip) {
    if (element == null) return;
    TextRange range = element.getTextRange();
    wrapTextRange(range, before, after, strip);
  }

  public void wrapTextRange(final TextRange range, String before, String after, final StripSpaces strip) {
    if (range == null) return;
    TextRange.assertProperRange(range);
    if (null == before) before = "";
    if (null == after) after = "";

    String textToWrap = range.subSequence(document.getCharsSequence()).toString();
    replaceElementText(range, before + textToWrap + after, strip);
  }

  public void addTextBeforeElement(final PsiElement element, final String text) {
    if (element == null) return;
    TextRange range = element.getTextRange();
    TextRange toReplace = new TextRange(range.getStartOffset(), range.getStartOffset());
    replaceAndFormat(toReplace, text);
  }

  public void addTextAfterElement(final PsiElement element, final String text) {
    if (element == null) return;
    TextRange range = element.getTextRange();
    TextRange toReplace = new TextRange(range.getEndOffset(), range.getEndOffset());
    replaceAndFormat(toReplace, text);
  }

  /**
   * Replace the text within the given range and reformat it according to the user's
   * code style/formatting rules.
   *
   * NOTE: The PSI may be entirely invalidated and re-created by this call.
   *
   * @param range Range of text or PsiElements to replace.
   * @param text Replacement text (may be null).
   */
  public void replaceAndFormat(@NotNull final TextRange range, @Nullable String text) {
    if (null == text) {
      text = "";
    }

    // Mark the beginning and end so that we have the proper range after adding text.
    // Greedy means that the text immediately added at the beginning/end of the marker are included.
    RangeMarker marker = document.createRangeMarker(range);
    marker.setGreedyToLeft(true);
    marker.setGreedyToRight(true);

    try {

      document.replaceString(range.getStartOffset(), range.getEndOffset(), text);

      //PsiDocumentManager.getInstance(file.getProject()).commitDocument(document); // force update PSI.

      if (marker.isValid()) { // If the range wasn't reduced to zero.
        CodeStyleManager.getInstance(file.getProject()).reformatText(file, marker.getStartOffset(), marker.getEndOffset());
      }
    }
    finally {
      marker.dispose();
    }
  }
}
