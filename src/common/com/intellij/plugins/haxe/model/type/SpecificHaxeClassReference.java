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
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxeNamedComponent;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeGenericParamModel;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SpecificHaxeClassReference extends SpecificTypeReference {
  static public SpecificHaxeClassReference[] EMPTY = new SpecificHaxeClassReference[0];
  @NotNull final public HaxeClassReference clazz;

  // @TODO: Change specifics with generics + generic resolver?
  final public ResultHolder[] specifics;
  final public Object constantValue;
  final public HaxeRange rangeConstraint;

  public SpecificHaxeClassReference(
    @NotNull HaxeClassReference clazz,
    ResultHolder[] specifics,
    Object constantValue,
    HaxeRange rangeConstraint,
    @NotNull PsiElement context
  ) {
    super(context);
    this.clazz = clazz;
    this.specifics = specifics;
    this.constantValue = constantValue;
    this.rangeConstraint = rangeConstraint;
  }

  public HaxeClassReference getHaxeClassRef() {
    return this.clazz;
  }

  public HaxeClass getHaxeClass() {
    return this.clazz.getHaxeClass();
  }

  public HaxeClassModel getHaxeClassModel() {
    final HaxeClass aClass = getHaxeClass();
    ;
    return (aClass != null) ? aClass.getModel() : null;
  }

  public SpecificHaxeClassReference withConstantValue(Object constantValue) {
    //if (this.constantValue == constantValue) return this;
    return new SpecificHaxeClassReference(clazz, specifics.clone(), constantValue, null, context);
  }

  //@Override
  //public void mutateConstantValue(Object constantValue) {
  //  this.constantValue = constantValue;
  //}

  @Override
  public SpecificTypeReference withRangeConstraint(HaxeRange range) {
    if (this.rangeConstraint == range) return this;
    return new SpecificHaxeClassReference(clazz, specifics.clone(), constantValue, range, context);
  }

  @Override
  public HaxeRange getRangeConstraint() {
    return this.rangeConstraint;
  }

  @Override
  public Object getConstant() {
    return this.constantValue;
  }

  static public SpecificHaxeClassReference withoutGenerics(@NotNull HaxeClassReference clazz) {
    return new SpecificHaxeClassReference(clazz, ResultHolder.EMPTY, null, null, clazz.elementContext);
  }

  static public SpecificHaxeClassReference withoutGenerics(@NotNull HaxeClassReference clazz, Object constantValue) {
    return new SpecificHaxeClassReference(clazz, ResultHolder.EMPTY, constantValue, null, clazz.elementContext);
  }

  static public SpecificHaxeClassReference withGenerics(@NotNull HaxeClassReference clazz, ResultHolder[] specifics) {
    return new SpecificHaxeClassReference(clazz, specifics, null, null, clazz.elementContext);
  }

  static public SpecificHaxeClassReference withGenerics(@NotNull HaxeClassReference clazz, ResultHolder[] specifics, Object constantValue) {
    return new SpecificHaxeClassReference(clazz, specifics, constantValue, null, clazz.elementContext);
  }

  public String toStringWithoutConstant() {
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
      }
      else if (out.equals("String")) {
        out += " = " + constantValue + "";
      }
      else {
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

  public HaxeGenericResolver getGenericResolver() {
    HaxeGenericResolver resolver = new HaxeGenericResolver();
    HaxeClassModel model = getHaxeClassModel();
    if (model != null) {
      List<HaxeGenericParamModel> params = model.getGenericParams();
      for (int n = 0; n < params.size(); n++) {
        HaxeGenericParamModel paramModel = params.get(n);
        ResultHolder specific = (n < specifics.length) ? this.specifics[n] : getUnknown(context).createHolder();
        resolver.resolvers.put(paramModel.getName(), specific);
      }
    }
    return resolver;
  }

  @Nullable
  @Override
  public ResultHolder access(String name, HaxeExpressionEvaluatorContext context) {
    if (this.isDynamic()) return this.withoutConstantValue().createHolder();

    if (name == null) {
      return null;
    }
    HaxeClass aClass = this.clazz.getHaxeClass();
    if (aClass == null) {
      return null;
    }
    AbstractHaxeNamedComponent field = (AbstractHaxeNamedComponent)aClass.findHaxeFieldByName(name);
    AbstractHaxeNamedComponent method = (AbstractHaxeNamedComponent)aClass.findHaxeMethodByName(name);
    if (method != null) {
      if (context.root == method) return null;
      return HaxeTypeResolver.getMethodFunctionType(method, getGenericResolver());
    }
    if (field != null) {
      if (context.root == field) return null;
      return HaxeTypeResolver.getFieldOrMethodReturnType(field, getGenericResolver());
    }
    return null;
  }
}
