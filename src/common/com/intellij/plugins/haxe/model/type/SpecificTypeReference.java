/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018 Eric Bishton
 * Copyright 2018 Ilya Malanin
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

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class SpecificTypeReference {
  public static final String VOID = "Void";
  public static final String BOOL = "Bool";
  public static final String INT = "Int";
  public static final String FLOAT = "Float";
  public static final String STRING = "String";
  public static final String ARRAY = "Array";
  public static final String DYNAMIC = "Dynamic";
  public static final String UNKNOWN = "Unknown";
  public static final String ITERATOR = "Iterator";
  public static final String INVALID = "@@Invalid";

  final protected PsiElement context;

  public SpecificTypeReference(@NotNull PsiElement context) {
    this.context = context;
  }

  static public SpecificTypeReference createArray(@NotNull ResultHolder elementType) {
    return SpecificHaxeClassReference
      .withGenerics(new HaxeClassReference(ARRAY, elementType.getElementContext()), new ResultHolder[]{elementType}, null);
  }

  static public SpecificTypeReference createMap(@NotNull ResultHolder keyType, @NotNull ResultHolder valueType) {
    return SpecificHaxeClassReference
      .withGenerics(new HaxeClassReference("Map", keyType.getElementContext()), new ResultHolder[]{keyType, valueType}, null);
  }

  public SpecificTypeReference withRangeConstraint(HaxeRange range) {
    return this;
  }

  static public SpecificHaxeClassReference getVoid(@NotNull PsiElement context) {
    return primitive(VOID, context);
  }

  static public SpecificHaxeClassReference getBool(@NotNull PsiElement context) {
    return primitive(BOOL, context);
  }

  static public SpecificHaxeClassReference getInt(@NotNull PsiElement context) {
    return primitive(INT, context);
  }

  static public SpecificHaxeClassReference getInt(@NotNull PsiElement context, int value) {
    return primitive(INT, context, value);
  }

  static public SpecificHaxeClassReference getDynamic(@NotNull PsiElement context) {
    return primitive(DYNAMIC, context);
  }

  static public SpecificHaxeClassReference getUnknown(@NotNull PsiElement context) {
    return primitive(UNKNOWN, context);
  }

  static public SpecificHaxeClassReference getInvalid(@NotNull PsiElement context) {
    return primitive(INVALID, context);
  }

  static public SpecificHaxeClassReference getIterator(SpecificHaxeClassReference type) {
    return SpecificHaxeClassReference.withGenerics(new HaxeClassReference(ITERATOR, type.getElementContext()),
                                                   new ResultHolder[]{type.createHolder()});
  }

  static public SpecificHaxeClassReference primitive(String name, @NotNull PsiElement context) {
    return SpecificHaxeClassReference.withoutGenerics(new HaxeClassReference(name, context));
  }

  static public SpecificHaxeClassReference primitive(String name, @NotNull PsiElement context, Object constant) {
    return SpecificHaxeClassReference.withoutGenerics(new HaxeClassReference(name, context), constant);
  }

  final public boolean isUnknown() {
    return this.toStringWithoutConstant().equals(UNKNOWN);
  }

  final public boolean isDynamic() {
    return this.toStringWithoutConstant().equals(DYNAMIC);
  }

  final public boolean isInvalid() {
    return this.toStringWithoutConstant().equals(INVALID);
  }

  final public boolean isVoid() {
    return this.toStringWithoutConstant().equals(VOID);
  }

  final public boolean isInt() {
    return this.toStringWithoutConstant().equals(INT);
  }

  final public boolean isNumeric() {
    return isInt() || isFloat();
  }

  final public boolean isBool() {
    return this.toStringWithoutConstant().equals(BOOL);
  }

  final public boolean isFloat() {
    return this.toStringWithoutConstant().equals(FLOAT);
  }

  final public boolean isString() {
    return this.toStringWithoutConstant().equals(STRING);
  }

  final public boolean isArray() {
    if (this instanceof SpecificHaxeClassReference) {
      final SpecificHaxeClassReference reference = (SpecificHaxeClassReference)this;
      return reference.clazz.getName().equals(ARRAY);
    }
    return false;
  }

  final public ResultHolder getArrayElementType() {
    if (isArray()) {
      final ResultHolder[] specifics = ((SpecificHaxeClassReference)this).specifics;
      if (specifics.length >= 1) return specifics[0];
    }
    return getUnknown(context).createHolder();
  }

  final public ResultHolder getIterableElementType(SpecificTypeReference iterable) {
    if (isArray()) {
      return getArrayElementType();
    }
    // @TODO: Must implement it (it is not int always)
    return getInt(iterable.getElementContext()).createHolder();
  }

  abstract public SpecificTypeReference withConstantValue(Object constantValue);

  //public void mutateConstantValue(Object constantValue) {
//
  //}
  final public SpecificTypeReference withoutConstantValue() {
    return withConstantValue(null);
  }

  public boolean isConstant() {
    return this.getConstant() != null;
  }

  public HaxeRange getRangeConstraint() {
    return null;
  }

  public Object getConstant() {
    return null;
  }

  final public boolean getConstantAsBool() {
    return HaxeTypeUtils.getBoolValue(getConstant());
  }

  final public double getConstantAsDouble() {
    return HaxeTypeUtils.getDoubleValue(getConstant());
  }

  final public int getConstantAsInt() {
    return HaxeTypeUtils.getIntValue(getConstant());
  }

  @NotNull
  final public PsiElement getElementContext() {
    return context;
  }

  abstract public String toString();

  abstract public String toStringWithoutConstant();

  public String toStringWithConstant() {
    return toString();
  }

  @Nullable
  public ResultHolder access(String name, HaxeExpressionEvaluatorContext context) {
    return null;
  }

  final public boolean canAssign(SpecificTypeReference type2) {
    return HaxeTypeCompatible.canAssignToFrom(this, type2);
  }

  final public boolean canAssign(ResultHolder type2) {
    return HaxeTypeCompatible.canAssignToFrom(this, type2);
  }

  public ResultHolder createHolder() {
    return new ResultHolder(this);
  }
}
