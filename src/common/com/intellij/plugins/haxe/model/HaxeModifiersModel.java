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

import com.google.common.collect.Lists;
import com.intellij.plugins.haxe.lang.psi.HaxeCustomMeta;
import com.intellij.plugins.haxe.lang.psi.HaxeDeclarationAttribute;
import com.intellij.plugins.haxe.lang.psi.HaxeFinalMeta;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import org.apache.commons.lang.NotImplementedException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HaxeModifiersModel extends HaxeModifiers {
  private PsiElement baseElement;

  public HaxeModifiersModel(PsiElement baseElement) {
    this.baseElement = baseElement;
  }

  @Override
  public boolean hasModifier(HaxeModifier modifier) {
    return getModifierPsi(modifier) != null;
  }

  // @TODO: This is bad! We need to parse this better!
  public PsiElement getModifierPsi(HaxeModifier modifier) {
    PsiElement result = UsefulPsiTreeUtil.getChildWithText(baseElement, HaxeDeclarationAttribute.class, modifier.getKeyword());
    if (result == null) result = UsefulPsiTreeUtil.getChildWithText(baseElement, HaxeFinalMeta.class, modifier.getKeyword());
    if (result == null) result = UsefulPsiTreeUtil.getChildWithText(baseElement, HaxeCustomMeta.class, modifier.getKeyword());
    return result;
  }

  public PsiElement getModifierPsiOrBase(HaxeModifier modifier) {
    PsiElement psi = getModifierPsi(modifier);
    if (psi == null) psi = this.baseElement;
    return psi;
  }

  @Override
  public void removeModifier(HaxeModifier modifier) {
    PsiElement psi = getModifierPsi(modifier);
    if (psi != null) {
      psi.delete();
    }
  }

  @Override
  public void replaceModifier(HaxeModifier oldModifier, HaxeModifier newModifier) {
    PsiElement psi = getModifierPsi(oldModifier);
    if (psi != null) {
      getDocument().replaceElementText(psi, newModifier.getKeywordWithSpace(), StripSpaces.AFTER);
    } else {
      addModifier(newModifier);
    }
  }

  public void sortModifiers() {
    // @TODO implement this!
  }

  private HaxeDocumentModel _document = null;
  @NotNull
  public HaxeDocumentModel getDocument() {
    if (_document == null) _document = HaxeDocumentModel.fromElement(baseElement);
    return _document;
  }

  @Override
  public void addModifier(HaxeModifier modifier) {
    getDocument().addTextBeforeElement(baseElement, modifier.getKeywordWithSpace());
  }

  // @TODO: This is really bad! We need to parse this better!
  @Override
  public List<HaxeModifier> getAllModifiers() {
    ArrayList<HaxeModifier> modifiers = new ArrayList<HaxeModifier>();

    for (HaxeModifier mod : HaxeExtraModifiers.values()) if (hasModifier(mod)) modifiers.add(mod);
    for (HaxeModifier mod : HaxeVisibility.values()) if (hasModifier(mod)) modifiers.add(mod);

    return modifiers;
  }

  @Override
  public Iterator<HaxeModifier> iterator() {
    return getAllModifiers().iterator();
  }
}
