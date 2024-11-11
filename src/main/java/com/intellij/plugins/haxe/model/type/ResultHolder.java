/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018 Ilya Malanin
 * Copyright 2019 Eric Bishton
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
package com.intellij.plugins.haxe.model.type;

import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.psi.PsiElement;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

@EqualsAndHashCode
public class ResultHolder {
  static public ResultHolder[] EMPTY = new ResultHolder[0];

  @Getter private final PsiElement origin;

  @NotNull
  private SpecificTypeReference type;
  private boolean canMutate = true;
  private int mutationCount = 0;


  public ResultHolder(@NotNull SpecificTypeReference type) {
    this(type, null);
  }
  public ResultHolder(@NotNull SpecificTypeReference type, @Nullable PsiElement origin) {
    this.type = type;
    this.origin = origin;
  }

  @NotNull
  public SpecificTypeReference getType() {
    return type;
  }

  @Nullable
  public SpecificFunctionReference getFunctionType() {
    return (type instanceof SpecificFunctionReference functionType) ? functionType : null;
  }

  @Nullable
  public SpecificHaxeClassReference getClassType() {
    return (type instanceof SpecificHaxeClassReference classReference) ? classReference : null;
  }
  @Nullable
  public SpecificEnumValueReference getEnumValueType() {
    return (type instanceof SpecificEnumValueReference enumValueReference) ? enumValueReference : null;
  }


  public boolean isFunctionType() {
    return (type instanceof SpecificFunctionReference);
  }
  public boolean isAnonymousType() {
    if(type instanceof SpecificHaxeClassReference classReference) return classReference.isAnonymousType();
    return false;
  }

  public boolean isClassType() {
    return (type instanceof SpecificHaxeClassReference);
  }
  public boolean isTypeDef() {
    return (type instanceof SpecificHaxeClassReference classReference) && classReference.isTypeDef();
  }
  public boolean isEnum() {
    if (type instanceof SpecificHaxeClassReference classReference){
      if(classReference.isEnumType()) return true;
      HaxeClass aClass = classReference.getHaxeClass();
      if(aClass != null)  return aClass.isEnum();
    }
    return false;
  }

  public boolean isEnumValueType() {
    return (type instanceof SpecificEnumValueReference);
  }

  public boolean isUnknown() {
    return type.isUnknown();
  }
  public boolean isMissingClassModel() {
    if (type instanceof SpecificHaxeClassReference classReference) {
      return classReference.getHaxeClassModel() == null;
    }
    return true;
  }

  public boolean isVoid() {
    return type.isVoid();
  }

  public boolean isDynamic() {
    return type.isDynamic();
  }

  public boolean isTypeParameter() {
    if(type instanceof  SpecificHaxeClassReference classReference) {
      return classReference.getHaxeClassReference().isTypeParameter();
    }
    return false;
  }

  public ResultHolder setType(@Nullable SpecificTypeReference type) {
    if (type == null) {
      type = SpecificTypeReference.getDynamic(this.type.getElementContext());
    }
    this.type = type;
    mutationCount++;
    return this;
  }


  public void disableMutating() {
    this.canMutate = false;
  }

  public boolean hasMutated() {
    return this.mutationCount > 0;
  }

  public boolean canMutate() {
    return this.canMutate;
  }

  public boolean isImmutable() {
    return !this.canMutate;
  }

  static public List<SpecificTypeReference> types(List<ResultHolder> holders) {
    LinkedList<SpecificTypeReference> out = new LinkedList<SpecificTypeReference>();
    for (ResultHolder holder : holders) {
      out.push(holder.type);
    }
    return out;
  }

  public boolean canAssign(ResultHolder that, HaxeAssignContext  context) {
    return HaxeTypeCompatible.canAssignToFrom(this, that, context);
  }
  public boolean canAssign(ResultHolder that) {
    return HaxeTypeCompatible.canAssignToFrom(this, that, null);
  }

  public void removeConstant() {
    setType(getType().withoutConstantValue());
  }

  public String toString() {
    return this.getType().toString();
  }

  public String toStringWithoutConstant() {
    return this.getType().toStringWithoutConstant();
  }

  public String toPresentationString() {
    return this.getType().toPresentationString();
  }

  public ResultHolder duplicate() {
    return new ResultHolder(this.getType());
  }

  public ResultHolder withConstantValue(Object constantValue) {
    return duplicate().setType(getType().withConstantValue(constantValue));
  }


  public PsiElement getElementContext() {
    return type.getElementContext();
  }


  public ResultHolder withOrigin(PsiElement origin) {
    return new ResultHolder(this.getType(), origin);
  }

  public boolean isInvalid() {
    return type.isInvalid();
  }

  public ResultHolder withElementContext(PsiElement element) {
    ResultHolder duplicate = duplicate();
    duplicate.type = duplicate.type.withElementContext(element);
    return duplicate;
  }

  public  boolean containsTypeParameters() {
    return containsTypeParameters(this);
  }
  public static boolean containsTypeParameters(ResultHolder holder) {
    if (holder.isUnknown()) return  false;
    if (holder.isTypeParameter()) return true;
    SpecificTypeReference type = holder.getType();
    if (type instanceof  SpecificHaxeClassReference classReference) {
      for (ResultHolder specific : classReference.getSpecifics()) {
        if (specific.type != type && containsTypeParameters(specific)) return  true;
      }
    }
    if (type instanceof SpecificFunctionReference  function) {
      return !function.getTypeParameters().isEmpty();
    }
    return false;
  }

  public boolean containsUnknownTypeParameters() {
    return containsUnknownTypeParameters(this);
  }
  public boolean containsUnknownTypes() {
    if(isUnknown()) return true;
    if(isFunctionType()) {
      return containsUnknownTypeParameters(this) || getFunctionType().containsUnknownTypes();
    }else {
      return containsUnknownTypeParameters(this);
    }
  }
  public static boolean containsUnknownTypeParameters(ResultHolder holder) {
    if (holder.isUnknown()) return  false;
    if (holder.isTypeParameter()) return true;
    SpecificTypeReference type = holder.getType();
    if (type instanceof  SpecificHaxeClassReference classReference) {
      for (ResultHolder specific : classReference.getSpecifics()) {
        if (specific.isUnknown() || containsUnknownTypeParameters(specific)) return  true;
      }
    }
    if (type instanceof SpecificFunctionReference  function) {
      List<ResultHolder> parameters = function.getTypeParameters();
      for (ResultHolder parameter : parameters) {
          if(parameter.isUnknown()) return true;
      }
    }
    return false;
  }

  public ResultHolder tryUnwrapNullType() {
    SpecificHaxeClassReference classType = getClassType();
    if (classType != null && classType.isNullType()) {
      if (classType.getSpecifics().length == 1) {
        return classType.getSpecifics()[0];
      }
    }
    return this;
  }

  public boolean isNullWrappedType() {
    SpecificHaxeClassReference classType = getClassType();
    return classType != null && classType.isNullType();
  }

  public ResultHolder wrapInNullType() {
    return getType().wrapInNullType().createHolder();
  }

  public static boolean nullOrUnknown(ResultHolder holder) {
    return holder == null || holder.isUnknown();
  }
}
