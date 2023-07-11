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

import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.HaxeParameterModel;
import com.intellij.psi.PsiElement;

import java.util.ArrayList;
import java.util.Comparator;
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

    SpecificHaxeClassReference intRef = SpecificHaxeClassReference.getInt(elementContext);
    SpecificHaxeClassReference boolRef = SpecificHaxeClassReference.getBool(elementContext);

    boolean canAssignLeftToInt = HaxeTypeCompatible.canAssignToFrom(intRef, left);
    boolean canAssignRightToInt = HaxeTypeCompatible.canAssignToFrom(intRef, right);

    boolean canAssignLeftToBool = HaxeTypeCompatible.canAssignToFrom(boolRef, left);
    boolean canAssignRightToBool = HaxeTypeCompatible.canAssignToFrom(boolRef, right);

    if (left.isNumeric() || right.isNumeric()) {
      if (operator.equals("+")
          || operator.equals("-")
          || operator.equals("*")
          || operator.equals("%")) {
        // unify should handle any abstracts with to/from  if combined with number
        result = HaxeTypeUnifier.unify(left, right, context.root);
      }

      if (operator.equals("/")) result = SpecificHaxeClassReference.getFloat( elementContext);
    }


    if (canAssignLeftToInt && canAssignRightToInt) {
      if (operator.equals("<<")
      || operator.equals(">>")
      || operator.equals(">>>")
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

    if (operator.equals("==") || operator.equals("!=")) {
        result = SpecificHaxeClassReference.primitive("Bool", elementContext, null);
    }

    if (operator.equals("<") || operator.equals("<=") ||
      operator.equals(">") || operator.equals(">=")) {
      if ((left.isNumeric() && right.isNumeric() )
          || left.isString() && right.isString()) {
        result = SpecificHaxeClassReference.primitive("Bool", elementContext, null);
      }else {
        result = SpecificHaxeClassReference.getUnknown(elementContext);
      }
    }

    if (operator.equals("&&") || operator.equals("||")) {
      if (canAssignLeftToBool && canAssignRightToBool) {
        result = SpecificHaxeClassReference.primitive("Bool", elementContext, null);
      }else {
        result = SpecificHaxeClassReference.getUnknown(elementContext);
      }
    }

    if (operator.equals("??")) {
      result = HaxeTypeUnifier.unify(left, right, context.root);
    }

    // check overloads
    SpecificTypeReference overloadResult = checkOverloads(left, right, operator);
    // if overload matched use result
    if (overloadResult != null) {
      result = overloadResult;
    }


    if (left.getConstant() != null && right.getConstant() != null) {
      if (operator.equals("&&")) {
        if(left.getConstant() instanceof  HaxeNull) {
          result.withConstantValue(left.getConstant());
        }else {
          result.withConstantValue(right.getConstant());
        }
      }
      result = result.withConstantValue(HaxeTypeUtils.applyBinOperator(left.getConstant(), right.getConstant(), operator));
    }

    return result != null ? result : SpecificHaxeClassReference.getUnknown(elementContext);
  }

  private static SpecificTypeReference checkOverloads(SpecificTypeReference type1,
                                                    SpecificTypeReference type2,
                                                      String operator) {
    List<HaxeMethodModel> overloads = new ArrayList<>();
    if (type1 instanceof  SpecificHaxeClassReference classReference) {
      overloads.addAll(classReference.getOperatorOverloads(operator));
    }
    if (type2 instanceof  SpecificHaxeClassReference classReference) {
      overloads.addAll(classReference.getOperatorOverloads(operator));
    }
    // TODO we can have multiple overloads that are applicable but we neeed to chose the one that is the most specific
    // ex. int can be cast to float and used as a float arg, but if we have  both a float and int overload we should pick int.
    overloads = sortByMostSpecificType(overloads, type1, type2);
      for (HaxeMethodModel overload : overloads) {
        // non-static methods takes 1 arg "this" is left, parameter is right
        if (overload.getParameters().size() == 1) {
          HaxeParameterModel param = overload.getParameters().get(0);
          boolean rightMatches = param.getType().canAssign(type2.createHolder());
          if (rightMatches) {
            return overload.getReturnType(null).getType();
          }
          // static methods takes 2 args (Left operator Right)
        }else if (overload.getParameters().size() == 2){
          HaxeParameterModel leftParam = overload.getParameters().get(0);
          HaxeParameterModel rightParam = overload.getParameters().get(1);
          boolean leftMatches = leftParam.getType().canAssign(type1.createHolder());
          boolean rightMatches = rightParam.getType().canAssign(type2.createHolder());
          if (leftMatches && rightMatches) {
            return  overload.getReturnType(null).getType();
          }
        }
      }
    return null;
  }

  private static List<HaxeMethodModel> sortByMostSpecificType(List<HaxeMethodModel> overloads,
                                                              SpecificTypeReference type1,
                                                              SpecificTypeReference type2) {
    //TODO mlo: implement properly
    //This is not really sorted by Most Specific type, it just looks for exact type match and move those to top of list.
    //hopefully this will work in most cases
    return overloads.stream().sorted(compareParam(type1, 0).thenComparing(compareParam(type2, 1))).toList();
  }

  private static  Comparator<HaxeMethodModel> compareParam(SpecificTypeReference param, int paramIndex) {
    return  (modelA, modelB) -> {

      boolean singleArgA = modelA.getParameters().size() == 1;
      boolean singleArgB = modelB.getParameters().size() == 1;
      if (singleArgA && singleArgB) {
        return 0;// both have 1 argument overloads, so "first" argument is "this" and therefore the correct type.
      }else if (singleArgA) {
        return -1;
      }else if (singleArgB) {
        return 1;
      }

      ResultHolder typeA = modelA.getParameters().get(paramIndex).getType();
      ResultHolder typeB = modelB.getParameters().get(paramIndex).getType();

      // if model A matches but model B  does not, move A upwards
      if (typeA.getType().isSameType(param) && !typeB.getType().isSameType(param)) {
        return -1;
      }
      // if model A does not matches while model B  does, move B upwards
      else if (!typeA.getType().isSameType(param) && typeB.getType().isSameType(param)) {
        return 1;
      }else {
        // both either matches or both does not match param
        return 0;
      }
    };
  }
}
