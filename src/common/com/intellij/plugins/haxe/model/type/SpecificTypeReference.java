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
package com.intellij.plugins.haxe.model.type;

import com.intellij.psi.PsiElement;

public abstract class SpecificTypeReference {
  static public SpecificTypeReference ensure(SpecificTypeReference clazz) {
    return (clazz != null) ? clazz : new SpecificHaxeClassReference(null, SpecificHaxeClassReference.EMPTY, null, null);
  }

  static public SpecificTypeReference createArray(SpecificTypeReference elementType) {
    return SpecificHaxeClassReference.withGenerics(new HaxeClassReference("Array", null), new SpecificTypeReference[]{elementType}, null);
  }

  public SpecificTypeReference withRangeConstraint(HaxeRange range) {
    return this;
  }

  static public SpecificHaxeClassReference getVoid(PsiElement context) {
    return primitive("Void", context);
  }

  static public SpecificHaxeClassReference getBool(PsiElement context) {
    return primitive("Bool", context);
  }
  static public SpecificHaxeClassReference getInt(PsiElement context) {
    return primitive("Int", context);
  }

  static public SpecificHaxeClassReference getDynamic(PsiElement context) {
    return primitive("Dynamic", context);
  }

  static public SpecificHaxeClassReference getUnknown(PsiElement context) {
    return primitive("Unknown", context);
  }
  static public SpecificHaxeClassReference getInvalid(PsiElement context) {
    return primitive("@@Invalid", context);
  }
  static public SpecificHaxeClassReference getIterator(SpecificHaxeClassReference type) {
    return SpecificHaxeClassReference.withGenerics(new HaxeClassReference("Iterator", type.getElementContext()),
                                                   new SpecificTypeReference[]{type});
  }
  static public SpecificHaxeClassReference primitive(String name, PsiElement context) {
    return SpecificHaxeClassReference.withoutGenerics(new HaxeClassReference(name, context));
  }
  static public SpecificHaxeClassReference primitive(String name, PsiElement context, Object constant) {
    return SpecificHaxeClassReference.withoutGenerics(new HaxeClassReference(name, context), constant);
  }

  final public boolean isUnknown() { return this.toStringWithoutConstant().equals("Unknown"); }
  final public boolean isInvalid() { return this.toStringWithoutConstant().equals("@@Invalid"); }
  final public boolean isVoid() { return this.toStringWithoutConstant().equals("Void"); }
  final public boolean isInt() { return this.toStringWithoutConstant().equals("Int"); }
  final public boolean isNumeric() { return isInt() || isFloat(); }
  final public boolean isBool() { return this.toStringWithoutConstant().equals("Bool"); }
  final public boolean isFloat() { return this.toStringWithoutConstant().equals("Float"); }
  final public boolean isString() { return this.toStringWithoutConstant().equals("String"); }
  final public boolean isArray() {
    if (this instanceof SpecificHaxeClassReference) {
      final SpecificHaxeClassReference reference = (SpecificHaxeClassReference)this;
      return reference.clazz.getName().equals("Array");
    }
    return false;
  }
  final public SpecificTypeReference getArrayElementType() {
    if (isArray()) {
      final SpecificTypeReference[] specifics = ((SpecificHaxeClassReference)this).specifics;
      if (specifics.length >= 1) return specifics[0];
    }
    return null;
  }

  final public SpecificTypeReference getIterableElementType(SpecificTypeReference iterable) {
    if (isArray()) {
      return getArrayElementType();
    }
    // @TODO: Must implement it (it is not int always)
    return getInt(iterable.getElementContext());
  }

  abstract public SpecificTypeReference withConstantValue(Object constantValue);
  public SpecificTypeReference withoutConstantValue() {
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
  abstract public PsiElement getElementContext();
  abstract public String toString();
  abstract public String toStringWithoutConstant();
  public SpecificTypeReference access(String name, HaxeExpressionEvaluatorContext context) {
    return null;
  }
  final public boolean canAssign(SpecificTypeReference type2) {
    return HaxeTypeCompatible.isAssignable(this, type2);
  }
}
