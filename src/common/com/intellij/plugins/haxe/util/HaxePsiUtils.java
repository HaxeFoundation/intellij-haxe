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

import java.util.ArrayList;
import java.util.List;

public class HaxePsiUtils {
  static public <T extends PsiElement> T getAncestor(PsiElement element, Class<T> clazz) {
    if (element == null) return null;
    if (clazz.isAssignableFrom(element.getClass())) return (T)element;
    return getAncestor(element.getParent(), clazz);
  }

  static public <T extends PsiElement> T getChild(PsiElement element, Class<T> clazz) {
    if (element == null) return null;
    for (PsiElement psiElement : element.getChildren()) {
      if (clazz.isAssignableFrom(psiElement.getClass())) return (T)psiElement;
    }
    return null;
  }

  static public <T extends PsiElement> T getChild(PsiElement element, Class<T> clazz, String text) {
    if (element == null) return null;
    for (PsiElement psiElement : element.getChildren()) {
      if (clazz.isAssignableFrom(psiElement.getClass()) && psiElement.getText().equals(text)) return (T)psiElement;
    }
    return null;
  }

  static public <T extends PsiElement> List<T> getChilds(PsiElement element, Class<T> clazz) {
    if (element == null) return null;
    ArrayList<T> ts = new ArrayList<T>();
    for (PsiElement psiElement : element.getChildren()) {
      if (clazz.isAssignableFrom(psiElement.getClass())) ts.add((T)psiElement);
    }
    return ts;
  }

  static public void replaceElementWithText(PsiElement element, String text) {
    Document document = PsiDocumentManager.getInstance(element.getProject()).getDocument(element.getContainingFile());
    TextRange range = element.getTextRange();
    document.replaceString(range.getStartOffset(), range.getEndOffset(), text);
  }
}
