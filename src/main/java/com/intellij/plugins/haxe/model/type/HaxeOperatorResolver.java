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

    SpecificTypeReference result = null;

    // while normal abstracts should not be resolved to underlying type, there's an exception for Null<T>
    // in this case we just "unwrap"  without trying to resolve
    if (left.isNullType()) {
      left = ((SpecificHaxeClassReference)left).getSpecifics()[0].getType();
    }
    if (right.isNullType()) {
      right = ((SpecificHaxeClassReference)right).getSpecifics()[0].getType();
    }

    boolean canAssignLeftToInt = HaxeTypeCompatible.canAssignToFrom(left, SpecificHaxeClassReference.getInt(left.context));
    boolean canAssignRightToInt = HaxeTypeCompatible.canAssignToFrom(right, SpecificHaxeClassReference.getInt(right.context));

    if (left.isNumeric() && right.isNumeric()) {
      result = HaxeTypeUnifier.unify(left, right, context.root);
      if (operator.equals("/")) result = SpecificHaxeClassReference.getFloat( elementContext);
    }


    if (canAssignLeftToInt && canAssignRightToInt) {
      if (operator.equals("<<")
      || operator.equals(">>")
      || operator.equals("&")
      || operator.equals("|")) {
        result =  SpecificHaxeClassReference.getInt(elementContext);
      }
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


    // check overloads for left argument
    SpecificTypeReference overloadResult = checkOverloads(left, right, operator);
    if (overloadResult == null) {
      // if no match for left,  check for overloads in right argument
      overloadResult = checkOverloads(right, left, operator);
    }
    // if overload matched use result
    if (overloadResult != null) {
      result = overloadResult;
    }


    if (left.getConstant() != null && right.getConstant() != null) {
      result = result.withConstantValue(HaxeTypeUtils.applyBinOperator(left.getConstant(), right.getConstant(), operator));
    }

    return result != null ? result : SpecificHaxeClassReference.getUnknown(elementContext);
  }

  private static SpecificTypeReference checkOverloads(SpecificTypeReference type1,
                                                    SpecificTypeReference type2,
                                                      String operator) {
    if (type1 instanceof  SpecificHaxeClassReference classReference) {
      List<HaxeMethodModel> overloads = classReference.getOperatorOverloads(operator);
      for (HaxeMethodModel overload : overloads) {
        // non-static methods takes 1 arg "this" is left, parameter is right
        if (overload.getParameters().size() == 1) {
          HaxeParameterModel param = overload.getParameters().get(0);
          boolean rightMatches = param.getType().canAssign(type2.createHolder());
          if (rightMatches) {
            return overload.getReturnType(null).getType();
          }
          // static methods takes 2 args (Left  operator Right)
        }else if (overload.getParameters().size() == 2){
          HaxeParameterModel param = overload.getParameters().get(1);
          boolean rightMatches = param.getType().canAssign(type2.createHolder());
          if (rightMatches) {
            return  overload.getReturnType(null).getType();
          }
        }
      }
    }
    return null;
  }
}
