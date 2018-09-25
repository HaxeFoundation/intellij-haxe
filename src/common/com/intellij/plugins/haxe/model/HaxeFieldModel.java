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
import com.intellij.psi.PsiElement;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HaxeFieldModel extends HaxeMemberModel {

  public HaxeFieldModel(@NotNull HaxePsiField element) {
    super(element);
  }

  @Override
  public boolean isFinal() {
    HaxeFieldDeclaration fieldDeclaration = ObjectUtils.tryCast(getBasePsi(), HaxeFieldDeclaration.class);
    if (fieldDeclaration != null) {
      final PsiElement mutabilityPsi = fieldDeclaration.getMutabilityModifier().getFirstChild();
      return mutabilityPsi.getText().equals(HaxePsiModifier.FINAL);
    }
    return false;
  }

  @Nullable
  public HaxePropertyDeclaration getPropertyDeclarationPsi() {
    final PsiElement basePsi = getBasePsi();
    return basePsi instanceof HaxeFieldDeclaration ? ((HaxeFieldDeclaration)basePsi).getPropertyDeclaration() : null;
  }

  @Nullable
  public HaxePropertyAccessor getAccessorPsi(int index) {
    if (getPropertyDeclarationPsi() == null) return null;
    List<HaxePropertyAccessor> list = getPropertyDeclarationPsi().getPropertyAccessorList();
    return (list.size() >= index) ? list.get(index) : null;
  }

  @NotNull
  public HaxePsiField getPsiField() {
    return (HaxePsiField)getBasePsi();
  }

  @Nullable
  public HaxePropertyAccessor getGetterPsi() {
    return getAccessorPsi(0);
  }

  @Nullable
  public HaxePropertyAccessor getSetterPsi() {
    return getAccessorPsi(1);
  }

  public HaxeAccessorType getSetterType() {
    return HaxeAccessorType.fromPsi(getSetterPsi());
  }

  public HaxeAccessorType getGetterType() {
    return HaxeAccessorType.fromPsi(getGetterPsi());
  }

  public boolean isProperty() {
    return getPropertyDeclarationPsi() != null;
  }

  public boolean isReadableFromOutside() {
    return isPublic() && (isRealVar() || this.getGetterType().isAllowedFromOutside());
  }

  public boolean isReadableFromInside() {
    return isRealVar() || this.getGetterType().isAllowedFromInside();
  }

  public boolean isWritableFromOutside() {
    return isPublic() && (isRealVar() || this.getSetterType().isAllowedFromOutside());
  }

  public boolean isWritableFromInside() {
    return isRealVar() || this.getSetterType().isAllowedFromInside();
  }

  public HaxeMethodModel getGetterMethod() {
    if (getGetterType() != HaxeAccessorType.GET) return null;
    return this.getDeclaringClass().getMethod("get_" + this.getName());
  }

  public HaxeMethodModel getSetterMethod() {
    if (getSetterType() != HaxeAccessorType.SET) return null;
    return this.getDeclaringClass().getMethod("set_" + this.getName());
  }

  public boolean isRealVar() {
    if (this.getModifiers().hasModifier(HaxePsiModifier.IS_VAR)) return true;
    if (!isProperty()) return true;
    HaxeAccessorType setter = getSetterType();
    HaxeAccessorType getter = getGetterType();
    return getter == HaxeAccessorType.DEFAULT ||
           getter == HaxeAccessorType.NULL ||
           setter == HaxeAccessorType.DEFAULT ||
           setter == HaxeAccessorType.NULL;
  }

  public boolean hasInitializer() {
    return getInitializerPsi() != null;
  }

  @Nullable
  public HaxeVarInit getInitializerPsi() {
    final PsiElement basePsi = getBasePsi();
    return basePsi instanceof HaxeFieldDeclaration ? ((HaxeFieldDeclaration)basePsi).getVarInit() : null;
  }

  public boolean hasTypeTag() {
    return getTypeTagPsi() != null;
  }

  public HaxeTypeTag getTypeTagPsi() {
    final PsiElement basePsi = getBasePsi();
    if (basePsi instanceof HaxeAnonymousTypeField) {
      return ((HaxeAnonymousTypeField)basePsi).getTypeTag();
    }
    if (basePsi instanceof HaxeFieldDeclaration) {
      return ((HaxeFieldDeclaration)basePsi).getTypeTag();
    }

    return null;
  }

  @Nullable
  @Override
  public HaxeExposableModel getExhibitor() {
    return getDeclaringClass();
  }
}
