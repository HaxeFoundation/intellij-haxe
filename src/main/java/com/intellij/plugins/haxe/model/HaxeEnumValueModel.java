/*
 * Copyright 2018 Ilya Malanin
 * Copyright 2020 Eric Bishton
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
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.plugins.haxe.util.HaxePresentableUtil.getPresentableParameterList;

public class HaxeEnumValueModel extends HaxeMemberModel {
  private final boolean isAbstractType;
  private final boolean hasConstructor;
  private final boolean hasReturnType;

  public HaxeEnumValueModel(@NotNull HaxeEnumValueDeclaration declaration) {
    super(declaration);

    hasConstructor = declaration.getParameterList() != null;
    hasReturnType = declaration.getReturnType() != null;
    isAbstractType = false;
  }

  public HaxeEnumValueModel(@NotNull HaxeFieldDeclaration declaration) {
    super(declaration);

    isAbstractType = true;
    hasConstructor = false;
    hasReturnType = true;
  }

  @Override
  public boolean isStatic() {
    return true;
  }

  @Override
  public boolean isPublic() {
    return !isAbstractType() || !hasModifier(HaxePsiModifier.PRIVATE);
  }

  public boolean isAbstractType() {
    return this.isAbstractType;
  }

  @Nullable
  public HaxeEnumValueDeclaration getEnumValuePsi() {
    PsiElement declaration = getBasePsi();
    return declaration instanceof HaxeEnumValueDeclaration ? (HaxeEnumValueDeclaration)declaration : null;
  }

  @Nullable
  @Override
  public HaxeExposableModel getExhibitor() {
    return getDeclaringClass();
  }

  public boolean hasConstructor() {
    return hasConstructor;
  }

  @Override
  public ResultHolder getResultType(@Nullable HaxeGenericResolver resolver) {
    PsiClass aClass = getMemberPsi().getContainingClass();
    if (aClass instanceof HaxeClass haxeClass) {

      HaxeClassReference superclassReference = new HaxeClassReference(haxeClass.getModel(), haxeClass);
      SpecificHaxeClassReference reference =
        SpecificHaxeClassReference.withGenerics(superclassReference, resolver.getSpecificsFor(haxeClass));

      ResultHolder holder = reference.createHolder();
      return holder;

    }
    return SpecificHaxeClassReference.getUnknown(aClass).createHolder();
  }

  @Nullable
  public HaxeParameterList getConstructorParameters() {
    HaxeEnumValueDeclaration declaration = getEnumValuePsi();
    return null != declaration ? declaration.getParameterList() : null;
  }

  @Override
  public String getPresentableText(HaxeMethodContext context) {
    StringBuilder result = new StringBuilder(getName());
    if (hasConstructor()) {
      result
        .append("(")
        .append(getPresentableParameterList((HaxeEnumValueDeclaration)getBasePsi()))
        .append((")"));
    }

    if (hasReturnType) {
      result
        .append(":")
        .append(getResultType().toString());
    }
    return result.toString();
  }
}
