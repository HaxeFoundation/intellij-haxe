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

import com.intellij.plugins.haxe.lang.psi.HaxeCustomMeta;
import com.intellij.plugins.haxe.lang.psi.HaxeDeclarationAttribute;
import com.intellij.plugins.haxe.lang.psi.HaxeFinalMeta;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public class HaxeModifiersModel {
  private PsiElement baseElement;

  public HaxeModifiersModel(PsiElement baseElement) {
    this.baseElement = baseElement;
  }

  public boolean hasModifier(HaxeModifierType modifier) {
    return getModifierPsi(modifier) != null;
  }

  public boolean hasAnyModifier(HaxeModifierType... modifiers) {
    for (HaxeModifierType modifier : modifiers) if (hasModifier(modifier)) return true;
    return false;
  }

  public boolean hasAllModifiers(HaxeModifierType... modifiers) {
    for (HaxeModifierType modifier : modifiers) if (!hasModifier(modifier)) return false;
    return true;
  }

  public PsiElement getModifierPsi(HaxeModifierType modifier) {
    PsiElement result = UsefulPsiTreeUtil.getChildWithText(baseElement, HaxeDeclarationAttribute.class, modifier.s);
    if (result == null) result = UsefulPsiTreeUtil.getChildWithText(baseElement, HaxeFinalMeta.class, modifier.s);
    if (result == null) result = UsefulPsiTreeUtil.getChildWithText(baseElement, HaxeCustomMeta.class, modifier.s);
    return result;
  }

  public PsiElement getModifierPsiOrBase(HaxeModifierType modifier) {
    PsiElement psi = getModifierPsi(modifier);
    if (psi == null) psi = this.baseElement;
    return psi;
  }

  public void replaceVisibility(HaxeModifierType modifier) {
    PsiElement psi = getVisibilityPsi();
    if (psi != null) {
      getDocument().replaceElementText(psi, modifier.getStringWithSpace(), StripSpaces.AFTER);
    } else {
      addModifier(modifier);
    }
  }

  public void removeModifier(HaxeModifierType modifier) {
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

  public void addModifier(HaxeModifierType modifier) {
    getDocument().addTextBeforeElement(baseElement, modifier.getStringWithSpace());
  }

  public PsiElement getVisibilityPsi() {
    PsiElement element = getModifierPsi(HaxeModifierType.PUBLIC);
    if (element == null) element = getModifierPsi(HaxeModifierType.PRIVATE);
    return element;
  }

  public HaxeModifierType getVisibility() {
    if (getModifierPsi(HaxeModifierType.PUBLIC) != null) return HaxeModifierType.PUBLIC;
    if (getModifierPsi(HaxeModifierType.PRIVATE) != null) return HaxeModifierType.PRIVATE;
    return HaxeModifierType.EMPTY;
  }
}
