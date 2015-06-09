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

import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeType;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxeNamedComponent;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxePsiClass;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.psi.PsiElement;

public class SpecificHaxeClassReference extends SpecificTypeReference {
  static public SpecificHaxeClassReference[] EMPTY = new SpecificHaxeClassReference[0];
  final public HaxeClassReference clazz;
  final public SpecificTypeReference[] specifics;
  final public Object constantValue;
  final public HaxeRange rangeConstraint;

  public SpecificHaxeClassReference(HaxeClassReference clazz, SpecificTypeReference[] specifics, Object constantValue, HaxeRange rangeConstraint) {
    this.clazz = clazz;
    this.specifics = specifics;
    this.constantValue = constantValue;
    this.rangeConstraint = rangeConstraint;
  }

  public SpecificHaxeClassReference withConstantValue(Object constantValue) {
    if (this.constantValue == constantValue) return this;
    return new SpecificHaxeClassReference(clazz, specifics.clone(), constantValue, rangeConstraint);
  }

  @Override
  public SpecificTypeReference withRangeConstraint(HaxeRange range) {
    if (this.rangeConstraint == range) return this;
    return new SpecificHaxeClassReference(clazz, specifics.clone(), constantValue, range);
  }

  @Override
  public HaxeRange getRangeConstraint() {
    return this.rangeConstraint;
  }

  @Override
  public Object getConstant() {
    return this.constantValue;
  }

  @Override
  public PsiElement getElementContext() {
    return this.clazz.elementContext;
  }

  public SpecificHaxeClassReference withoutConstantValue() {
    return withConstantValue(null);
  }

  static public SpecificHaxeClassReference withoutGenerics(HaxeClassReference clazz) {
    return new SpecificHaxeClassReference(clazz, EMPTY, null, null);
  }

  static public SpecificHaxeClassReference withoutGenerics(HaxeClassReference clazz, Object constantValue) {
    return new SpecificHaxeClassReference(clazz, EMPTY, constantValue, null);
  }

  static public SpecificHaxeClassReference withGenerics(HaxeClassReference clazz, SpecificTypeReference[] specifics) {
    return new SpecificHaxeClassReference(clazz, specifics, null, null);
  }

  static public SpecificHaxeClassReference withGenerics(HaxeClassReference clazz, SpecificTypeReference[] specifics, Object constantValue) {
    return new SpecificHaxeClassReference(clazz, specifics, constantValue, null);
  }

  public String toStringWithoutConstant() {
    if (this.clazz == null) return "Unknown";
    String out = this.clazz.getName();
    if (specifics.length > 0) {
      out += "<";
      for (int n = 0; n < specifics.length; n++) {
        if (n > 0) out += ", ";
        out += specifics[n].toString();
      }
      out += ">";
    }
    return out;
  }

  public String toStringWithConstant() {
    String out = toStringWithoutConstant();
    if (constantValue != null) {
      if (out.equals("Int")) {
        out += " = " + (int)HaxeTypeUtils.getDoubleValue(constantValue);
      } else if (out.equals("String")) {
        out += " = " + constantValue + "";
      } else {
        out += " = " + constantValue;
      }
    }
    if (rangeConstraint != null) {
      out += " [" + rangeConstraint + "]";
    }
    return out;
  }

  @Override
  public String toString() {
    //return toStringWithoutConstant();
    return toStringWithConstant();
  }

  @Override
  public SpecificTypeReference access(String name, HaxeExpressionEvaluatorContext context) {
    if (this.clazz == null || name == null) {
      return null;
    }
    HaxeClass aClass = this.clazz.getHaxeClass();
    if (aClass ==  null) {
      return null;
    }
    AbstractHaxeNamedComponent field = (AbstractHaxeNamedComponent)aClass.findHaxeFieldByName(name);
    AbstractHaxeNamedComponent method = (AbstractHaxeNamedComponent)aClass.findHaxeMethodByName(name);
    if (method != null) {
      if (context.root == method) return null;
      return HaxeTypeResolver.getMethodFunctionType(method);
    }
    if (field != null) {
      if (context.root == field) return null;
      return HaxeTypeResolver.getFieldOrMethodReturnType(field);
    }
    return null;
  }
}
