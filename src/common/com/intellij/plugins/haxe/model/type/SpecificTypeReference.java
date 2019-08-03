/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018-2019 Eric Bishton
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

import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeDummyASTNode;
import com.intellij.plugins.haxe.lang.psi.impl.HaxePsiCompositeElementImpl;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeProjectModel;
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
  public static final String UNKNOWN = "Unknown"; // TODO: Should NOT a legal type name.
  public static final String ITERATOR = "Iterator";
  public static final String INVALID = "@@Invalid";
  public static final String MAP = "Map";
  public static final String ANY = "Any"; // Specifically, the "Any" class; See <Haxe>/std/Any.hx.

  /** A context to use when there is none to be found.  Try very hard not to use this, please. */
  public static final PsiElement UNKNOWN_CONTEXT = new HaxePsiCompositeElementImpl(new HaxeDummyASTNode(UNKNOWN));

  final protected PsiElement context;

  public SpecificTypeReference(@NotNull PsiElement context) {
    this.context = context;
  }

  public static SpecificTypeReference createArray(@NotNull ResultHolder elementType) {
    final PsiElement context = elementType.getElementContext();
    final HaxeClassReference classReference = getStdClassReference(ARRAY, context);
    return SpecificHaxeClassReference.withGenerics(classReference, new ResultHolder[]{elementType}, null);
  }

  public static SpecificTypeReference createMap(@NotNull ResultHolder keyType, @NotNull ResultHolder valueType) {
    final PsiElement context = keyType.getElementContext();
    final HaxeClassReference classReference = getStdClassReference(MAP, context);
    return SpecificHaxeClassReference.withGenerics(classReference, new ResultHolder[]{keyType, valueType}, null);
  }

  public static SpecificHaxeClassReference getVoid(@NotNull PsiElement context) {
    return primitive(VOID, context);
  }

  public static SpecificHaxeClassReference getBool(@NotNull PsiElement context) {
    return primitive(BOOL, context);
  }

  public static SpecificHaxeClassReference getInt(@NotNull PsiElement context) {
    return primitive(INT, context);
  }

  public static SpecificHaxeClassReference getInt(@NotNull PsiElement context, int value) {
    return primitive(INT, context, value);
  }

  public static SpecificHaxeClassReference getDynamic(@NotNull PsiElement context) {
    return primitive(DYNAMIC, context);
  }

  public static SpecificHaxeClassReference getAny(@NotNull PsiElement context) {
    return primitive(ANY, context);
  }

  public static SpecificHaxeClassReference getUnknown(@NotNull PsiElement context) {
    return SpecificHaxeClassReference.withoutGenerics(getUnknownClassReference(context));
  }

  public static SpecificHaxeClassReference getInvalid(@NotNull PsiElement context) {
    return SpecificHaxeClassReference.withoutGenerics(new HaxeClassReference(INVALID, context));
  }

  public static SpecificHaxeClassReference getIterator(SpecificHaxeClassReference type) {
    final PsiElement context = type.getElementContext();
    final HaxeClassReference classReference = getStdClassReference(ITERATOR, context);
    return SpecificHaxeClassReference.withGenerics(classReference, new ResultHolder[]{type.createHolder()});
  }

  public static SpecificHaxeClassReference primitive(String name, @NotNull PsiElement context) {
    return SpecificHaxeClassReference.withoutGenerics(getStdClassReference(name, context));
  }

  public static SpecificHaxeClassReference primitive(String name, @NotNull PsiElement context, Object constant) {
    return SpecificHaxeClassReference.withoutGenerics(getStdClassReference(name, context), constant);
  }

  public SpecificTypeReference withRangeConstraint(HaxeRange range) {
    return this;
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
    return isNamedType(ARRAY);
  }

  final public boolean isMap() {
    return isNamedType(MAP);
  }

  final public boolean isAny() {
    return isNamedType(ANY);
  }

  private boolean isNamedType(String typeName) {
    if (this instanceof SpecificHaxeClassReference) {
      final SpecificHaxeClassReference reference = (SpecificHaxeClassReference)this;
      return reference.getHaxeClassReference().getName().equals(typeName);
    }
    return false;
  }

  final public ResultHolder getArrayElementType() {
    if (isArray()) {
      final ResultHolder[] specifics = ((SpecificHaxeClassReference)this).getSpecifics();
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
  public ResultHolder access(String name, HaxeExpressionEvaluatorContext context, HaxeGenericResolver resolver) {
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

  public abstract boolean canBeTypeVariable();

  @NotNull
  private static HaxeClassReference getStdClassReference(String className, PsiElement context) {
    final HaxeClassModel model = getStdTypeModel(className, context);
    HaxeClassReference classReference;
    if (model != null) {
      classReference = new HaxeClassReference(model, context);
    } else {
      classReference = new HaxeClassReference(className, context);
    }
    return classReference;
  }

  @Nullable
  private static HaxeClassModel getStdTypeModel(String name, PsiElement context) {
    return HaxeProjectModel.fromElement(context).getStdPackage().getClassModel(name);
  }

  @NotNull
  private static HaxeClassReference getUnknownClassReference(@NotNull PsiElement context) {
    return new HaxeClassReference(HaxeClass.UNKNOWN_CLASS.getModel(), context);
  }
}
