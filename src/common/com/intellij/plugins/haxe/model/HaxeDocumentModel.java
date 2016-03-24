/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
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
package com.intellij.plugins.haxe.model;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.util.HaxeCharUtils;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;

public class HaxeDocumentModel {
  private Document document;

  public HaxeDocumentModel(Document document) {
    this.document = document;
  }

  public HaxeDocumentModel(PsiElement aElementInDocument) {
    this(PsiDocumentManager.getInstance(aElementInDocument.getProject()).getDocument(aElementInDocument.getContainingFile()));
  }

  static public HaxeDocumentModel fromElement(PsiElement aElementInDocument) {
    return new HaxeDocumentModel(aElementInDocument);
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
    document.replaceString(start, end, text);
  }

  public void wrapElement(final PsiElement element, final String before, final String after) {
    wrapElement(element, before, after, StripSpaces.NONE);
  }

  public void wrapElement(final PsiElement element, final String before, final String after, StripSpaces strip) {
    if (element == null) return;
    TextRange range = element.getTextRange();
    this.replaceElementText(element, before + element.getText() + after, strip);
  }

  public void addTextBeforeElement(final PsiElement element, final String text) {
    if (element == null) return;
    TextRange range = element.getTextRange();
    document.replaceString(range.getStartOffset(), range.getStartOffset(), text);
  }

  public void addTextAfterElement(final PsiElement element, final String text) {
    if (element == null) return;
    TextRange range = element.getTextRange();
    document.replaceString(range.getEndOffset(), range.getEndOffset(), text);
  }
}
