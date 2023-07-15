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
import com.intellij.plugins.haxe.model.type.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    if (null != constraint) {
      HaxeTypeOrAnonymous toa = constraint.getTypeOrAnonymous();
      if (null != toa.getType()) {
        HaxeReferenceExpression reference = toa.getType().getReferenceExpression();
        if (null != reference) {

          ResultHolder result =
            HaxeExpressionEvaluator.evaluate(reference, new HaxeExpressionEvaluatorContext(part), resolver).result;
          return result;
        }
        else {

          // TODO: Deal with function return types.

        }
      }
      else {
        // Anonymous struct for a constraint.
        // TODO: Turn the anonymous structure into a ResolveResult.
        return HaxeTypeResolver.getPsiElementType(toa.getOriginalElement(),  resolver); //temp solution
      }
    }
    return null;
  }

  public String toString() {
    return name;
  }
}
