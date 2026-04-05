/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2018 Ilya Malanin
 * Copyright 2018-2020 Eric Bishton
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
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.model.type.HaxeTypeResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class HaxeBaseMemberModel implements HaxeNamedComponentModel {
  protected PsiElement basePsi;
  protected HaxeDocumentModel document = null;

  public HaxeBaseMemberModel(PsiElement basePsi) {
    this.basePsi = basePsi;
  }

  static private HaxeNamedComponent getNamedComponentPsi(PsiElement element) {
    if (element == null) return null;
    if (element instanceof HaxeNamedComponent namedComponent) return namedComponent;
    if (element.getParent() instanceof HaxeNamedComponent parentNamedComponent) return parentNamedComponent;
    return getNamedComponentPsi(UsefulPsiTreeUtil.getChild(element, HaxeNamedComponent.class));
  }

  public static HaxeBaseMemberModel fromPsi(PsiElement element) {
    if (element instanceof HaxeMethod method) return method.getModel();
    if (element instanceof HaxeFieldDeclaration fieldDeclaration) return (HaxeBaseMemberModel)fieldDeclaration.getModel();
    if (element instanceof HaxeEnumValueDeclaration enumValueDeclaration)  return (HaxeBaseMemberModel) enumValueDeclaration.getModel();
    if (element instanceof HaxeLocalVarDeclaration varDeclaration) return (HaxeBaseMemberModel) varDeclaration.getModel();
    if (element instanceof HaxeAnonymousTypeField anonymousTypeField) return (HaxeBaseMemberModel) anonymousTypeField.getModel();
    if (element instanceof HaxeObjectLiteralElement objectLiteralElement) return (HaxeBaseMemberModel) objectLiteralElement.getModel();

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
  public PsiElement getNameOrBasePsi() {
    PsiElement element = getNamePsi();
    if (element == null) element = getBasePsi();
    return element;
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

  @Nullable
  public abstract HaxeClassModel getDeclaringClass();

  public abstract HaxeModuleModel getDeclaringModule();

  @Deprecated
  public ResultHolder getResultType() {
    return HaxeTypeResolver.getFieldOrMethodReturnType((AbstractHaxeNamedComponent)this.basePsi);
  }

  public ResultHolder getResultType(@Nullable HaxeGenericResolver resolver) {
    return HaxeTypeResolver.getFieldOrMethodReturnType((AbstractHaxeNamedComponent)this.basePsi, resolver);
  }

  public String getPresentableText(HaxeMethodContext context) {
      return getPresentableText(context, null);
  }

  public String getPresentableText(HaxeMethodContext context, @Nullable HaxeGenericResolver resolver) {
    PsiElement basePsi = getBasePsi();
    if (basePsi instanceof AbstractHaxeNamedComponent namedComponent) {
      return namedComponent.getPresentation().getPresentableText();
    }
    return this.getName();
  }

  @Nullable
  @Override
  public abstract FullyQualifiedInfo getQualifiedInfo();


  public HaxeModule getModule() {
    return PsiTreeUtil.getChildOfType(getDocument().getFile(), HaxeModule.class);
  }

  public PsiPackage getPackage() {
    HaxePackageStatement childOfType = PsiTreeUtil.getChildOfType(getDocument().getFile(), HaxePackageStatement.class);
    if(childOfType!= null) {
      HaxeReferenceExpression reference = childOfType.getReferenceExpression();
      if(reference!= null && reference.resolve() instanceof PsiPackage aPackage) return aPackage;
    }
    return null;
  }

  @Override
  public boolean isValid() {
    return basePsi.isValid();
  }
}
