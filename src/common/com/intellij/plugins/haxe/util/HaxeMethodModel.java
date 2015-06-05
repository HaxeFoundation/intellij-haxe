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
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaToken;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeMethodModel {
  private HaxeMethodPsiMixin haxeMethod;
  private String name;

  public HaxeMethodModel(HaxeMethodPsiMixin haxeMethod) {
    this.haxeMethod = haxeMethod;
    this.name = haxeMethod.getName();
  }

  public HaxeMethodPsiMixin getPsi() {
    return haxeMethod;
  }

  public PsiElement getNamePsi() {
    PsiElement child = HaxePsiUtils.getChild(HaxePsiUtils.getChild(haxeMethod, HaxeComponentName.class), HaxeIdentifier.class);
    if (child == null) child = HaxePsiUtils.getToken(haxeMethod, "new");
    return child;
  }

  public boolean isStatic() {
    return getModifiers().hasModifier(HaxeModifierType.STATIC);
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
      HaxeClass aClass = (HaxeClass)this.haxeMethod.getContainingClass();
      _declaringClass = (aClass != null) ? aClass.getModel() : null;
    }
    return _declaringClass;
  }

  public String getName() {
    return name;
  }

  public String getFullName() {
    return this.getDeclaringClass().getName() + "." + this.getName();
  }

  private HaxeModifiersModel _modifiers;
  @NotNull
  public HaxeModifiersModel getModifiers() {
    if (_modifiers == null) _modifiers = new HaxeModifiersModel(this.haxeMethod);
    return _modifiers;
  }

  private HaxeDocument _document = null;
  @NotNull
  public HaxeDocument getDocument() {
    if (_document == null) _document = new HaxeDocument(haxeMethod);
    return _document;
  }

  public boolean isConstructor() {
    return this.getName().equals("new");
  }

  public boolean isStaticInit() {
    return this.getName().equals("__init__");
  }
}

