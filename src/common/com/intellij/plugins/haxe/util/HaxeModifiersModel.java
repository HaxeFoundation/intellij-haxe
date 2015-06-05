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
import com.intellij.plugins.haxe.lang.psi.HaxeDeclarationAttribute;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeModifiersModel {
  private PsiElement baseElement;

  public HaxeModifiersModel(PsiElement baseElement) {
    this.baseElement = baseElement;
  }


  public @Nullable
  PsiElement getOverride() {
    return getModifier("override");
  }

  public @Nullable PsiElement getStatic() {
    return getModifier("static");
  }

  public @Nullable PsiElement getInline() {
    return getModifier("inline");
  }

  public @Nullable PsiElement getPublic() {
    return getModifier("public");
  }

  public @Nullable PsiElement getPrivate() {
    return getModifier("private");
  }

  public @Nullable PsiElement getFinal() {
    return getModifier("@:final");
  }

  public PsiElement getModifier(String text) {
    return HaxePsiUtils.getChild(baseElement, HaxeDeclarationAttribute.class, text);
  }

  public void replaceVisibility(String text) {
    PsiElement modifier = getVisibilityPsi();
    if (modifier != null) {
      getDocument().replaceElementText(modifier, text);
    } else {
      addModifier(text);
    }
  }

  public void removeModifier(String text) {
    PsiElement modifier = getModifier(text);
    if (modifier != null) {
      getDocument().replaceElementText(modifier, "");
    }
  }

  private HaxeDocument _document = null;
  @NotNull
  public HaxeDocument getDocument() {
    if (_document == null) _document = new HaxeDocument(baseElement);
    return _document;
  }

  public void addModifier(String text) {
    getDocument().addTextBeforeElement(baseElement, text + " ");
  }

  public PsiElement getVisibilityPsi() {
    PsiElement element = null;
    element = getPublic(); if (element != null) return element;
    element = getPrivate(); if (element != null) return element;
    return null;
  }

  public HaxeVisibilityType getVisibility() {
    if (getPublic() != null) return HaxeVisibilityType.PUBLIC;
    if (getPrivate() != null) return HaxeVisibilityType.PRIVATE;
    return HaxeVisibilityType.NONE;
  }
}
