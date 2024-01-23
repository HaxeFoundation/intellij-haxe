/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018-2019 Eric Bishton
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
import com.intellij.plugins.haxe.lang.psi.impl.HaxeTypeParameterMultiType;
import com.intellij.plugins.haxe.model.type.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * HaxeGenericParamModel (genericParam) appears on type and method *declarations*.
 * {@link HaxeTypeParameterModel} appears on references (e.g. in inherit lists).
 */
public class HaxeGenericParamModel {
  final private HaxeGenericListPart part;
  final private String name;
  final private int index;

  public HaxeGenericParamModel(@NotNull HaxeGenericListPart part, int index) {
    this.index = index;
    this.part = part;
    this.name = part.getComponentName().getText();
  }

  public int getIndex() {
    return index;
  }

  public String getName() {
    return this.name;
  }

  public HaxeGenericListPart getPsi() { return part; }

  @Nullable
  public ResultHolder getConstraint(@Nullable HaxeGenericResolver resolver) {
    if (null == resolver) {
      resolver = new HaxeGenericResolver();
    }

    HaxeTypeListPart constraint = part.getTypeListPart();
    if (constraint == null) {
      HaxeTypeList list = part.getTypeList();
      if (list != null) {
        HaxeTypeParameterMultiType type = convertToMultiTypeParameter(list, resolver);
        return  new ResultHolder(SpecificHaxeClassReference.withoutGenerics(new HaxeClassReference(type.getModel(), part)));
      }
    }
    if (constraint != null)  {
      HaxeTypeOrAnonymous toa = constraint.getTypeOrAnonymous();
      if (toa != null) {
        if (null != toa.getType()) {
          HaxeReferenceExpression reference = toa.getType().getReferenceExpression();
          ResultHolder result = HaxeExpressionEvaluator.evaluate(reference, new HaxeExpressionEvaluatorContext(part), resolver).result;
          if (!result.isUnknown()) {
            return result;
          } else {
            if (HaxeTypeResolver.isTypeParameter(reference)) {
              return HaxeTypeResolver.getTypeFromTypeOrAnonymous(toa);
            }
          }
        }

        else {
          // Anonymous struct for a constraint.
          // TODO: Turn the anonymous structure into a ResolveResult.
          return HaxeTypeResolver.getPsiElementType(toa.getOriginalElement(), resolver); //temp solution
        }
      }
      HaxeFunctionType functionType = constraint.getFunctionType();
      if (functionType != null) {
        return HaxeTypeResolver.getTypeFromFunctionType(functionType);
      }
    }
    return null;
  }

  @NotNull
  private HaxeTypeParameterMultiType convertToMultiTypeParameter(HaxeTypeList list, @Nullable HaxeGenericResolver resolver) {
    List<HaxeTypeListPart> partList = list.getTypeListPartList();
    List<HaxeTypeOrAnonymous> mapped = partList.stream().map(HaxeTypeListPart::getTypeOrAnonymous).toList();

    List<HaxeAnonymousType> anonymousTypes = mapped.stream()
      .filter(t -> t.getAnonymousType() != null)
      .map(HaxeTypeOrAnonymous::getAnonymousType).toList();

    List<HaxeAnonymousTypeBody> anonymousTypeBodies = anonymousTypes.stream().flatMap(type -> type.getAnonymousTypeBodyList().stream())
      .toList();

    List<HaxeType> typeList = mapped.stream().filter(t -> t.getType() != null).map(HaxeTypeOrAnonymous::getType).toList();

    HaxeTypeParameterMultiType type = new HaxeTypeParameterMultiType(part.getNode(), typeList, anonymousTypeBodies);
    return type;
  }

  public String toString() {
    return name;
  }
}
