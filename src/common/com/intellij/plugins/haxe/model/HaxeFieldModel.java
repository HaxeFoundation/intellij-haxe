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
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HaxeFieldModel extends HaxeMemberModel {
  private HaxeVarDeclaration element;

  public HaxeFieldModel(HaxeVarDeclaration element) {
    super(element, element, UsefulPsiTreeUtil.getChild(element, HaxeVarDeclarationPart.class));
    this.element = element;
  }

  @Override
  public PsiElement getPsi() {
    return element;
  }

  public HaxeVarDeclaration getFieldPsi() {
    return element;
  }

  @NotNull
  public HaxeVarDeclarationPart getDeclarationPsi() {
    return element.getVarDeclarationPart();
  }

  private HaxeClassModel _declaringClass = null;
  public HaxeClassModel getDeclaringClass() {
    if (_declaringClass == null) {
      HaxeClass aClass = (HaxeClass)this.element.getContainingClass();
      _declaringClass = (aClass != null) ? aClass.getModel() : null;
    }
    return _declaringClass;
  }

  @Nullable
  public HaxePropertyDeclaration getPropertyDeclarationPsi() {
    return getDeclarationPsi().getPropertyDeclaration();
  }

  @Nullable
  public HaxePropertyAccessor getAccessorPsi(int index) {
    if (getPropertyDeclarationPsi() == null) return null;
    List<HaxePropertyAccessor> list = getPropertyDeclarationPsi().getPropertyAccessorList();
    return (list.size() >= index) ? list.get(index) : null;
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
    if (getGetterType() != HaxeAccessorType.SET) return null;
    return this.getDeclaringClass().getMethod("set_" + this.getName());
  }

  public boolean isRealVar() {
    if (this.getModifiers().hasModifier(HaxeModifierType.IS_VAR)) return true;
    if (!isProperty()) return true;
    if (getSetterType() == HaxeAccessorType.NULL || getSetterType() == HaxeAccessorType.DEFAULT) {
      return true;
    }
    return false;
  }

  public boolean hasInitializer() {
    return getInitializerPsi() != null;
  }

  public HaxeVarInit getInitializerPsi() {
    return getDeclarationPsi().getVarInit();
  }

  public boolean hasTypeTag() {
    return getTypeTagPsi() != null;
  }

  public HaxeTypeTag getTypeTagPsi() {
    return getDeclarationPsi().getTypeTag();
  }
}
