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
package com.intellij.plugins.haxe.util;

import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxeNamedComponent;
import com.intellij.psi.PsiElement;

public class SpecificHaxeClassReference implements SpecificTypeReference {
  static public SpecificHaxeClassReference[] EMPTY = new SpecificHaxeClassReference[0];
  public HaxeClassReference clazz;
  public SpecificHaxeClassReference[] specifics;
  public Object constantValue = null;

  public SpecificHaxeClassReference(HaxeClassReference clazz, SpecificHaxeClassReference[] specifics, Object constantValue) {
    this.clazz = clazz;
    this.specifics = specifics;
    this.constantValue = constantValue;
  }

  public SpecificHaxeClassReference withConstantValue(Object constantValue) {
    return new SpecificHaxeClassReference(clazz, specifics.clone(), constantValue);
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

  static public SpecificTypeReference ensure(SpecificTypeReference clazz) {
    return (clazz != null) ? clazz : new SpecificHaxeClassReference(null, EMPTY, null);
  }

  static public SpecificHaxeClassReference withoutGenerics(HaxeClassReference clazz) {
    return new SpecificHaxeClassReference(clazz, EMPTY, null);
  }

  static public SpecificHaxeClassReference withoutGenerics(HaxeClassReference clazz, Object constantValue) {
    return new SpecificHaxeClassReference(clazz, EMPTY, constantValue);
  }

  static public SpecificHaxeClassReference withGenerics(HaxeClassReference clazz, SpecificHaxeClassReference[] specifics) {
    return new SpecificHaxeClassReference(clazz, specifics, null);
  }

  static public SpecificHaxeClassReference withGenerics(HaxeClassReference clazz, SpecificHaxeClassReference[] specifics, Object constantValue) {
    return new SpecificHaxeClassReference(clazz, specifics, constantValue);
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
        out += " = " + (int)HaxeTypeUtil.getDoubleValue(constantValue);
      } else if (out.equals("String")) {
        out += " = " + constantValue + "";
      } else {
        out += " = " + constantValue;
      }
    }
    return out;
  }

  @Override
  public String toString() {
    //return toStringWithoutConstant();
    return toStringWithConstant();
  }

  @Override
  public SpecificTypeReference access(String name) {
    HaxeClass aClass = this.clazz.getHaxeClass();
    if (aClass ==  null) {
      return null;
    }
    AbstractHaxeNamedComponent field = (AbstractHaxeNamedComponent)aClass.findHaxeFieldByName(name);
    AbstractHaxeNamedComponent method = (AbstractHaxeNamedComponent)aClass.findHaxeMethodByName(name);
    if (method != null) return HaxeTypeUtil.getMethodFunctionType(method);
    if (field != null) return HaxeTypeUtil.getFieldOrMethodReturnType(field);
    return null;
  }
}
