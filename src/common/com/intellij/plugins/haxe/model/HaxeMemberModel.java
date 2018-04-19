/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2018 Ilya Malanin
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
import com.intellij.plugins.haxe.util.HaxeAbstractEnumUtil;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.plugins.haxe.model.HaxeModifierType.*;

abstract public class HaxeMemberModel implements HaxeModel {
  private PsiElement basePsi;

  public HaxeMemberModel(PsiElement basePsi) {
    this.basePsi = basePsi;
  }

  @Override
  public PsiElement getBasePsi() {
    return basePsi;
  }

  public boolean isPublic() {
    HaxeClassModel declaringClass = getDeclaringClass();

    return hasModifier(PUBLIC)
           // Fields and methods of externs and interfaces are public by default, private modifier for them should be defined explicitly
           || ((declaringClass.isInterface() || declaringClass.isExtern()) && !hasModifier(PRIVATE))
           || isOverriddenPublicMethod()
           || getDeclaringClass().hasMeta("@:publicFields");
  }

  private boolean isOverriddenPublicMethod() {
    if (hasModifier(OVERRIDE)) {
      final HaxeMemberModel parentMember = getParentMember();
      return parentMember != null && parentMember.isPublic();
    }

    return false;
  }

  public boolean hasModifier(HaxeModifierType aPublic) {
    return this.getModifiers().hasModifier(aPublic);
  }

  public boolean isStatic() {
    return hasModifier(STATIC);
  }

  private HaxeDocumentModel _document = null;

  @NotNull
  public HaxeDocumentModel getDocument() {
    if (_document == null) _document = new HaxeDocumentModel(this.getBasePsi());
    return _document;
  }

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

  @NotNull
  public PsiElement getNameOrBasePsi() {
    PsiElement element = getNamePsi();
    if (element == null) element = getBasePsi();
    return element;
  }

  abstract public HaxeClassModel getDeclaringClass();

  private HaxeModifiersModel _modifiers;

  @NotNull
  public HaxeModifiersModel getModifiers() {
    if (_modifiers == null) _modifiers = new HaxeModifiersModel(basePsi);
    return _modifiers;
  }

  public static HaxeMemberModel fromPsi(PsiElement element) {
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
    if (element instanceof HaxeParameter) return new HaxeParameterModel((HaxeParameter)element);
    if (element instanceof HaxeForStatement) return null;
    final PsiElement parent = element.getParent();
    return (parent != null) ? fromPsi(parent) : null;
  }

  public ResultHolder getResultType() {
    return HaxeTypeResolver.getFieldOrMethodReturnType((AbstractHaxeNamedComponent)this.basePsi);
  }

  public String getPresentableText(HaxeMethodContext context) {
    return this.getName() + ":" + getResultType();
  }

  public HaxeMemberModel getParentMember() {
    final HaxeClassModel aClass = getDeclaringClass().getParentClass();
    return (aClass != null) ? aClass.getMember(this.getName()) : null;
  }

  @Nullable
  @Override
  public FullyQualifiedInfo getQualifiedInfo() {
    if (getDeclaringClass() != null && isStatic() && isPublic()) {
      FullyQualifiedInfo containerInfo = getDeclaringClass().getQualifiedInfo();
      if (containerInfo != null) {
        return new FullyQualifiedInfo(containerInfo.packagePath, containerInfo.fileName, containerInfo.className, getName());
      }
    }
    return null;
  }
}
