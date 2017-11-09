/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2017 Ilya Malanin
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
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HaxeFieldModel extends HaxeMemberModel {
  private HaxePsiField element;

  public HaxeFieldModel(HaxePsiField element) {
    super(element);

    this.element = element;
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
    return element instanceof HaxeVarDeclaration ? ((HaxeVarDeclaration)element).getPropertyDeclaration() : null;
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
    if (getSetterType() != HaxeAccessorType.SET) return null;
    return this.getDeclaringClass().getMethod("set_" + this.getName());
  }

  public boolean isRealVar() {
    if (this.getModifiers().hasModifier(HaxeModifierType.IS_VAR)) return true;
    if (!isProperty()) return true;
    HaxeAccessorType setter = getSetterType();
    HaxeAccessorType getter = getGetterType();
    if (setter == HaxeAccessorType.NULL || setter == HaxeAccessorType.DEFAULT) {
      return true;
    }
    else if (setter == HaxeAccessorType.NEVER &&
             (getter == HaxeAccessorType.DEFAULT || getter == HaxeAccessorType.NULL)) {
      return true;
    }
    return false;
  }

  public boolean hasInitializer() {
    return getInitializerPsi() != null;
  }

  @Nullable
  public HaxeVarInit getInitializerPsi() {
    return element instanceof HaxeVarDeclaration ? ((HaxeVarDeclaration)element).getVarInit() : null;
  }

  public boolean hasTypeTag() {
    return getTypeTagPsi() != null;
  }

  public HaxeTypeTag getTypeTagPsi() {
    if (element instanceof HaxeAnonymousTypeField) {
      return ((HaxeAnonymousTypeField)element).getTypeTag();
    }
    if (element instanceof HaxeVarDeclaration) {
      return ((HaxeVarDeclaration)element).getTypeTag();
    }

    return null;
  }

  @Nullable
  @Override
  public HaxeExposableModel getExhibitor() {
    return getDeclaringClass();
  }

  @Nullable
  @Override
  public FullyQualifiedInfo getQualifiedInfo() {
    return null;
  }
}
