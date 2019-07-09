/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2018 Ilya Malanin
 * Copyright 2018-2019 Eric Bishton
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

import com.intellij.openapi.util.Key;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxeNamedComponent;
import com.intellij.plugins.haxe.model.type.HaxeTypeResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.util.HaxeAbstractEnumUtil;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class HaxeBaseMemberModel implements HaxeModel {
  protected static final Key<HaxeClassModel> DECLARING_CLASS_MODEL_KEY = new Key<>("HAXE_DECLARING_CLASS_MODEL");
  protected PsiElement basePsi;
  protected HaxeDocumentModel document = null;

  public HaxeBaseMemberModel(PsiElement basePsi) {
    this.basePsi = basePsi;
  }

  static private HaxeNamedComponent getNamedComponentPsi(PsiElement element) {
    if (element == null) return null;
    if (element instanceof HaxeNamedComponent) return (HaxeNamedComponent)element;
    if (element.getParent() instanceof HaxeNamedComponent) return (HaxeNamedComponent)element.getParent();
    return getNamedComponentPsi(UsefulPsiTreeUtil.getChild(element, HaxeNamedComponent.class));
  }

  public static HaxeBaseMemberModel fromPsi(PsiElement element) {
    if (element instanceof HaxeMethod) return ((HaxeMethod)element).getModel();
    if (element instanceof HaxeFieldDeclaration) {
      PsiClass containingClass = ((HaxeFieldDeclaration)element).getContainingClass();
      if (HaxeAbstractEnumUtil.isAbstractEnum(containingClass) && HaxeAbstractEnumUtil.couldBeAbstractEnumField(element)) {
        return new HaxeEnumValueModel((HaxeFieldDeclaration)element);
      }
      return new HaxeFieldModel((HaxeFieldDeclaration)element);
    }
    if (element instanceof HaxeEnumValueDeclaration) return new HaxeEnumValueModel((HaxeEnumValueDeclaration)element);
    if (element instanceof HaxeLocalVarDeclaration) return new HaxeLocalVarModel((HaxeLocalVarDeclaration)element);
    if (element instanceof HaxeAnonymousTypeField) return new HaxeAnonymousTypeFieldModel((HaxeAnonymousTypeField)element);
    if (element instanceof HaxeParameter) return new HaxeParameterModel((HaxeParameter)element);
    if (element instanceof HaxeForStatement) return null;
    final PsiElement parent = element.getParent();
    return (parent != null) ? fromPsi(parent) : null;
  }

  @Override
  public PsiElement getBasePsi() {
    return basePsi;
  }

  @NotNull
  public HaxeDocumentModel getDocument() {
    if (document == null) document = new HaxeDocumentModel(this.getBasePsi());
    return document;
  }

  public HaxeNamedComponent getNamedComponentPsi() {
    return getNamedComponentPsi(basePsi);
  }

  public String getName() {
    HaxeComponentName namePsi = getNamePsi();
    return namePsi == null ? "" : namePsi.getText();
  }

  public HaxeComponentName getNamePsi() {
    HaxeComponentName componentName = UsefulPsiTreeUtil.getChild(basePsi, HaxeComponentName.class);
    if (componentName != null && componentName.getParent() instanceof HaxeNamedComponent) {
      return componentName;
    }
    return null;
  }

  public abstract HaxeClassModel getDeclaringClass();

  public ResultHolder getResultType() {
    return HaxeTypeResolver.getFieldOrMethodReturnType((AbstractHaxeNamedComponent)this.basePsi);
  }

  public String getPresentableText(HaxeMethodContext context) {
    return this.getName() + ":" + getResultType();
  }

  @Nullable
  @Override
  public abstract FullyQualifiedInfo getQualifiedInfo();
}
