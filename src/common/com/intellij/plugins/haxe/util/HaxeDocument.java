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
package com.intellij.plugins.haxe.util;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import org.apache.commons.lang.CharUtils;

public class HaxeDocument {
  private Document document;

  public HaxeDocument(Document document) {
    this.document = document;
  }

  public HaxeDocument(PsiElement aElementInDocument) {
    this(PsiDocumentManager.getInstance(aElementInDocument.getProject()).getDocument(aElementInDocument.getContainingFile()));
  }

  public void replaceElementText(PsiElement element, String text) {
    if (element != null) {
      TextRange range = element.getTextRange();
      document.replaceString(range.getStartOffset(), range.getEndOffset(), text);
    }
  }

  public void replaceElementPlusSpacesText(PsiElement element, String text) {
    if (element != null) {
      TextRange range = element.getTextRange();
      int start = range.getStartOffset();
      int end = range.getEndOffset();
      String documentText = document.getText();
      while (end < documentText.length() && HaxeCharUtils.isSpace(documentText.charAt(end))) {
        end++;
      }
      document.replaceString(start, end, text);
    }
  }

  public void addTextBeforeElement(PsiElement element, String text) {
    if (element != null) {
      TextRange range = element.getTextRange();
      document.replaceString(range.getStartOffset(), range.getStartOffset(), text);
    }
  }
}
