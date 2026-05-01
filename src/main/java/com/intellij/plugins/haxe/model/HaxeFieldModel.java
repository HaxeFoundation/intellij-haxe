/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2018 Ilya Malanin
 * Copyright 2019-2020 Eric Bishton
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
import com.intellij.plugins.haxe.lang.psi.stubs.stub.HaxeFieldStub;
import com.intellij.plugins.haxe.lang.util.HaxeExpressionUtil;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.StubBasedPsiElement;
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

  public boolean isMacroName() {
    return getPsiField().isMacroName();
  }

  @Nullable
  private HaxeFieldStub getFieldStub() {
    if (getPsiField() instanceof StubBasedPsiElement<?> stubPsi) {
      if (stubPsi.getStub() instanceof HaxeFieldStub fieldStub) {
        return fieldStub;
      }
    }
    return null;
  }

  @Nullable
  public HaxePropertyDeclaration getPropertyDeclarationPsi() {
    final PsiElement basePsi = getBasePsi();
    return basePsi instanceof HaxeFieldDeclaration ? ((HaxeFieldDeclaration)basePsi).getPropertyDeclaration() : null;
  }

  /**
   * Returns the full property declaration text (e.g. {@code "(get, set)"}), using the stub when available,
   * falling back to PSI. Returns {@code null} if this is not a property field.
   */
  @Nullable
  public String getPropertyDeclarationText() {
    HaxeFieldStub stub = getFieldStub();
    if (stub != null) {
      return stub.isProperty() ? "(" + stub.getGetter() + ", " + stub.getSetter() + ")" : null;
    }
    HaxePropertyDeclaration decl = getPropertyDeclarationPsi();
    return decl != null ? decl.getText() : null;
  }

  /** Returns the getter accessor text (e.g. {@code "get"}, {@code "null"}), using stub when available. */
  @Nullable
  public String getGetterText() {
    HaxeFieldStub stub = getFieldStub();
    if (stub != null) return stub.getGetter();
    HaxePropertyAccessor psi = getGetterPsi();
    return psi != null ? psi.getText() : null;
  }

  /** Returns the setter accessor text (e.g. {@code "set"}, {@code "never"}), using stub when available. */
  @Nullable
  public String getSetterText() {
    HaxeFieldStub stub = getFieldStub();
    if (stub != null) return stub.getSetter();
    HaxePropertyAccessor psi = getSetterPsi();
    return psi != null ? psi.getText() : null;
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

  @NotNull
  public HaxeAccessorType getSetterType() {
    HaxeFieldStub stub = getFieldStub();
    if (stub != null) return HaxeAccessorType.from(stub.getSetter());
    return HaxeAccessorType.fromPsi(getSetterPsi());
  }

  @NotNull
  public HaxeAccessorType getGetterType() {
    HaxeFieldStub stub = getFieldStub();
    if (stub != null) return HaxeAccessorType.from(stub.getGetter());
    return HaxeAccessorType.fromPsi(getGetterPsi());
  }

  public boolean isProperty() {
    HaxeFieldStub stub = getFieldStub();
    if (stub != null) return stub.isProperty();
    return getPropertyDeclarationPsi() != null;
  }

  public boolean isReadableFromOutside() {
    return isPublic() &&  this.getGetterType().isAllowedFromOutside();
  }

  public boolean isWritableFromOutside() {
    return isPublic() && (this.getSetterType().isAllowedFromOutside());
  }

  public boolean isReadableFromInside() {
    return isRealVar() || this.getGetterType().isAllowedFromInside();
  }

  public boolean isWritableFromInside() {
    return  this.getSetterType().isAllowedFromInside();
  }

  public boolean isReadableFromPropertyAccessor() {
    return isRealVar();
  }

  public boolean isWritableFromPropertyAccessor() {
    return isRealVar();
  }



  @Nullable
  public HaxeMethodModel getGetterMethod() {
    HaxeAccessorType getterType = getGetterType();
    if (getterType != HaxeAccessorType.GET && getterType!= HaxeAccessorType.PRIVATE_GET) return null;
    HaxeClassModel declaringClass = this.getDeclaringClass();
    boolean macroName = isMacroName();
    String name = macroName ? this.getName().substring(1) : this.getName();
    String prefix = macroName ? "$" : "";

    if (declaringClass != null) {
      return declaringClass.getMethod(prefix + "get_" + name, null);
    }
    HaxeModuleModel declaringModule = this.getDeclaringModule();
    if (declaringModule != null) {
      return declaringModule.getMethod(prefix + "get_" + name, null);
    }
    return null;
  }

  @Nullable
  public HaxeMethodModel getSetterMethod() {
    HaxeAccessorType setterType = getSetterType();
    if (setterType != HaxeAccessorType.SET && setterType != HaxeAccessorType.PRIVATE_SET) return null;
    HaxeClassModel declaringClass = this.getDeclaringClass();
    boolean macroName = isMacroName();
    String name = macroName ? this.getName().substring(1) : this.getName();
    String prefix = macroName ? "$" : "";
    if (declaringClass != null) {
      return declaringClass.getMethod(prefix + "set_" + name, null);
    }
    HaxeModuleModel declaringModule = this.getDeclaringModule();
    if (declaringModule != null) {
      return declaringModule.getMethod(prefix + "set_" + name, null);
    }
    return null;
  }

  public boolean isRealVar() {
    HaxeFieldStub fieldStub = getFieldStub();
    if (this.getModifiers().hasModifier(HaxePsiModifier.IS_VAR_META)) return true;
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

  /**
   * Tells whether a field is a constant, according to the rules for default parameters.
   *
   * @return true, if the field is a constant; false, otherwise.
   */
  public boolean isConstant() {
      if (getBasePsi() instanceof HaxeEnumValueDeclarationField field) {
        return true;
      }

    return isStatic() && isInline() && HaxeExpressionUtil.isConstantExpression(getInitializerExpression());
  }

  public boolean isEnumValue() {
    return getBasePsi() instanceof HaxeEnumValueDeclaration;
  }

  @Override
  public String getPresentableText(HaxeMethodContext context) {
    return getPresentableText(context, null);
  }
  @Override
  public String getPresentableText(HaxeMethodContext context, HaxeGenericResolver resolver) {
    ResultHolder type = getResultType(resolver);
    return type == null ? this.getName() : this.getName() + ":" + type;
  }

  /**
   * Gets the initializer expression for the field.
   * @return
   */
  @Nullable
  public HaxeExpression getInitializerExpression() {
    HaxeVarInit initializer = getInitializerPsi();
    if (null != initializer) {
      HaxeExpression expression = initializer.getExpression();
      return expression;
    }
    return null;
  }

  public boolean isOptional() {
    if(getBasePsi() instanceof HaxeOptionalFieldDeclaration) return true;
    if(getBasePsi() instanceof HaxeAnonymousTypeField anonymous) {
      if (anonymous.getOptionalMark() != null)return true;
    }
    return hasOptionalMeta();
  }
}
