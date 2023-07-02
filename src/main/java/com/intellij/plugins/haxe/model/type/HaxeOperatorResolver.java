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

import com.intellij.plugins.haxe.model.HaxeMemberModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.HaxeParameterModel;
import com.intellij.psi.PsiElement;

import java.util.List;

public class HaxeOperatorResolver {
  static public SpecificTypeReference getBinaryOperatorResult(
    PsiElement elementContext,
    SpecificTypeReference left,
    SpecificTypeReference right,
    String operator,
    HaxeExpressionEvaluatorContext context
  ) {

    SpecificTypeReference result = SpecificHaxeClassReference.getUnknown(elementContext);

    // while normal abstracts should not be resolved to underlying type, there's an exception for Null<T>
    // in this case we just "unwrap"  without trying to resolve
    if (left.isNullType()) {
      left = ((SpecificHaxeClassReference)left).getSpecifics()[0].getType();
    }
    if (right.isNullType()) {
      right = ((SpecificHaxeClassReference)right).getSpecifics()[0].getType();
    }

    if (left.isNumeric() && right.isNumeric()) {
      result = HaxeTypeUnifier.unify(left, right, context.root);
      if (operator.equals("/")) result = SpecificHaxeClassReference.primitive("Float", elementContext, null);
    }

    if (operator.equals("+")) {
      if (left.toStringWithoutConstant().equals("String") || right.toStringWithoutConstant().equals("String")) {
        return SpecificHaxeClassReference.primitive("String", elementContext);
      }
    }

    if (
      operator.equals("==") || operator.equals("!=") ||
      operator.equals("<") || operator.equals("<=") ||
      operator.equals(">") || operator.equals(">=")
      ) {
        result = SpecificHaxeClassReference.primitive("Bool", elementContext, null);
    }

    if (operator.equals("&&") || operator.equals("||")) {
      if (left.isBool() && right.isBool()) {
        result = SpecificHaxeClassReference.primitive("Bool", elementContext, null);
      }
    }


    if (left instanceof  SpecificHaxeClassReference classReference) {
      List<HaxeMethodModel> overloads = classReference.getOperatorOverloads(operator);
      for (HaxeMethodModel overload : overloads) {
        if (overload.getParameters().size() == 1) {
          HaxeParameterModel param = overload.getParameters().get(0);
          boolean rightMatches = param.getType().canAssign(right.createHolder());
          if (rightMatches) {
            result =  overload.getReturnType(null).getType();
          }
        }
      }
    }

    if (left.getConstant() != null && right.getConstant() != null) {
      result = result.withConstantValue(HaxeTypeUtils.applyBinOperator(left.getConstant(), right.getConstant(), operator));
    }
    return result;
  }
}
