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

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxeNamedComponent;
import com.intellij.plugins.haxe.model.type.HaxeTypeResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificTypeReference;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMember;
import org.jetbrains.annotations.NotNull;

abstract public class HaxeMemberModel {
  private PsiMember basePsi;
  private PsiElement modifiersPsi;
  private PsiElement baseNamePsi;

  public HaxeMemberModel(PsiMember basePsi, PsiElement modifiersPsi, PsiElement baseNamePsi) {
    this.basePsi = basePsi;
    this.modifiersPsi = modifiersPsi;
    this.baseNamePsi = baseNamePsi;
  }

  public boolean isPublic() {
    return this.getModifiers().hasModifier(HaxeModifierType.PUBLIC);
  }
  public boolean isStatic() {
    return getModifiers().hasModifier(HaxeModifierType.STATIC);
  }

  private HaxeMetasModel metas;
  public HaxeMetasModel getMetas() {
    if (metas == null) metas = new HaxeMetasModel(basePsi);
    return metas;
  }

  private HaxeDocumentModel _document = null;
  @NotNull
  public HaxeDocumentModel getDocument() {
    if (_document == null) _document = HaxeDocumentModel.fromElement(this.getPsi());
    return _document;
  }

  final public PsiElement getPsi() { return basePsi; }

  public HaxeNamedComponent getNamedComponentPsi() {
    return getNamedComponentPsi(basePsi);
  }

  static private HaxeNamedComponent getNamedComponentPsi(PsiElement element) {
    if (element == null) return null;
    if (element instanceof HaxeNamedComponent) return (HaxeNamedComponent)element;
    if (element.getParent() instanceof HaxeNamedComponent) return (HaxeNamedComponent)element.getParent();
    return getNamedComponentPsi(UsefulPsiTreeUtil.getChild(element, HaxeNamedComponent.class));
  }

  public String getName() {
    PsiElement namePsi = getNamePsi();
    return (namePsi != null) ? namePsi.getText() : "";
  }

  public PsiElement getNamePsi() {
    PsiElement child = UsefulPsiTreeUtil.getChild(UsefulPsiTreeUtil.getChild(baseNamePsi, HaxeComponentName.class), HaxeIdentifier.class);
    if (child == null) child = UsefulPsiTreeUtil.getToken(baseNamePsi, "new");
    return child;
  }

  @NotNull
  public PsiElement getNameOrBasePsi() {
    PsiElement element = getNamePsi();
    if (element == null) element = getPsi();
    return element;
  }

  private HaxeClassModel _declaringClass = null;

  public HaxeClassModel getDeclaringClass() {
    if (_declaringClass == null) {
      HaxeClass aClass = (HaxeClass)this.basePsi.getContainingClass();
      _declaringClass = (aClass != null) ? aClass.getModel() : null;
    }
    return _declaringClass;
  }

  private HaxeModifiersModel _modifiers;
  @NotNull
  public HaxeModifiersModel getModifiers() {
    if (_modifiers == null) _modifiers = new HaxeModifiersModel(modifiersPsi);
    return _modifiers;
  }

  public static HaxeMemberModel fromPsi(PsiElement element) {
    if (element instanceof HaxeMethod) return ((HaxeMethod)element).getModel();
    if (element instanceof HaxeVarDeclaration) return ((HaxeVarDeclaration)element).getModel();
    final PsiElement parent = element.getParent();
    return (parent != null) ? fromPsi(parent) : null;
  }

  public ResultHolder getResultType() {
    return HaxeTypeResolver.getFieldOrMethodReturnType((AbstractHaxeNamedComponent)this.baseNamePsi);
  }

  abstract public ResultHolder getMemberType();

  public String getPresentableText() {
    return this.getName() + ":" + getResultType();
  }

  public HaxeMemberModel getParentMember() {
    final HaxeClassModel aClass = getDeclaringClass().getParentClass();
    return (aClass != null) ? aClass.getMember(this.getName()) : null;
  }

  public String toString() { return this.getName(); }
}
