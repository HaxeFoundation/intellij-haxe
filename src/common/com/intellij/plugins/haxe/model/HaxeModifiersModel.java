/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018 Ilya Malanin
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

import com.intellij.plugins.haxe.lang.psi.HaxeCustomMeta;
import com.intellij.plugins.haxe.lang.psi.HaxeFinalMeta;
import com.intellij.plugins.haxe.lang.psi.HaxePsiModifier;
import com.intellij.plugins.haxe.lang.psi.HaxePsiModifier.ModifierConstant;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HaxeModifiersModel {
  private PsiElement baseElement;

  public HaxeModifiersModel(PsiElement baseElement) {
    this.baseElement = baseElement;
  }

  public boolean hasModifier(@ModifierConstant String modifier) {
    return getModifierPsi(modifier) != null;
  }

  public boolean hasAnyModifier(@ModifierConstant String... modifiers) {
    for (String modifier : modifiers) if (hasModifier(modifier)) return true;
    return false;
  }

  public boolean hasAllModifiers(@ModifierConstant String... modifiers) {
    for (String modifier : modifiers) if (!hasModifier(modifier)) return false;
    return true;
  }

  public PsiElement getModifierPsi(@ModifierConstant String modifier) {
    PsiElement result = UsefulPsiTreeUtil.getChildWithText(baseElement, HaxePsiModifier.class, modifier);
    if (result == null) result = UsefulPsiTreeUtil.getChildWithText(baseElement, HaxeFinalMeta.class, modifier);
    if (result == null) result = UsefulPsiTreeUtil.getChildWithText(baseElement, HaxeCustomMeta.class, modifier);

    return result;
  }

  public PsiElement getModifierPsiOrBase(@ModifierConstant String modifier) {
    PsiElement psi = getModifierPsi(modifier);
    if (psi == null) psi = this.baseElement;
    return psi;
  }

  public void replaceVisibility(@ModifierConstant String modifier) {
    PsiElement psi = getVisibilityPsi();
    if (psi != null) {
      getDocument().replaceElementText(psi, HaxePsiModifier.getStringWithSpace(modifier), StripSpaces.AFTER);
    } else {
      addModifier(modifier);
    }
  }

  public void removeModifier(@ModifierConstant String modifier) {
    PsiElement psi = getModifierPsi(modifier);
    if (psi != null) {
      getDocument().replaceElementText(psi, "", StripSpaces.AFTER);
    }
  }

  public void sortModifiers() {
    // @TODO implement this!
  }

  private HaxeDocumentModel _document = null;

  @NotNull
  public HaxeDocumentModel getDocument() {
    if (_document == null) _document = new HaxeDocumentModel(baseElement);
    return _document;
  }

  public void addModifier(@ModifierConstant String modifier) {
    getDocument().addTextBeforeElement(baseElement, HaxePsiModifier.getStringWithSpace(modifier));
  }

  public PsiElement getVisibilityPsi() {
    PsiElement element = getModifierPsi(HaxePsiModifier.PUBLIC);
    if (element == null) element = getModifierPsi(HaxePsiModifier.PRIVATE);
    return element;
  }

  public @ModifierConstant
  String getVisibility() {
    if (getModifierPsi(HaxePsiModifier.PUBLIC) != null) return HaxePsiModifier.PUBLIC;
    if (getModifierPsi(HaxePsiModifier.PRIVATE) != null) return HaxePsiModifier.PRIVATE;

    return HaxePsiModifier.EMPTY;
  }

  public List<String> getPresentModifiers(@ModifierConstant String[] modifiers) {
    List<String> result = new SmartList<>();
    for (String modifier : modifiers) {
      if (hasModifier(modifier)) {
        result.add(modifier);
      }
    }

    return result;
  }
}
