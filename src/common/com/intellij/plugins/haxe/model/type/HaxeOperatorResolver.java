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

import com.intellij.plugins.haxe.model.*;
import com.intellij.psi.PsiElement;

public class HaxeOperatorResolver {
  static public SpecificTypeReference getBinaryOperatorResult(
    PsiElement elementContext,
    SpecificTypeReference left,
    SpecificTypeReference right,
    String operator,
    HaxeExpressionEvaluatorContext context
  ) {
    SpecificTypeReference result = _getBinaryOperatorResult(elementContext, left, right, operator, context);

    // @TODO: Check operator overloading
    if (left.getConstant() != null && right.getConstant() != null) {
      result = result.withConstantValue(HaxeTypeUtils.applyBinOperator(left.getConstant(), right.getConstant(), operator));
    }

    return result;
  }

  static public SpecificTypeReference _getBinaryOperatorResult(
    PsiElement elementContext,
    SpecificTypeReference left,
    SpecificTypeReference right,
    String operator,
    HaxeExpressionEvaluatorContext context
  ) {
    final HaxeTypesModel types = context.project.getTypes();

    if (left instanceof SpecificFunctionReference || right instanceof SpecificFunctionReference) {
      context.addError(
        elementContext,
        "Can't apply operator " + operator + " for functional types " + left + " and " + right
      );
      return context.types.DYNAMIC;
    }

    final HaxeClassModel leftModel = ((SpecificHaxeClassReference)left).getHaxeClassModel();
    final HaxeClassModel rightModel = ((SpecificHaxeClassReference)right).getHaxeClassModel();
    if (leftModel != null && rightModel != null) {
      final HaxeCustomOperatorsModel operators = leftModel.getBinaryOperators();
      final HaxeCustomOperatorModel op = operators.get(operator);
      if (op.enabled()) {
        final ResultHolder type = op.getResultType(left, right);
        if (type != null) {
          return type.getType();
        }
      }
    }

    if (operator.equals("/")) {
      if (left.isNumeric() || right.isNumeric()) {
        return types.FLOAT;
      }
    }

    if (operator.equals("+")) {
      if (left.isString() || right.isString()) {
        return types.STRING;
      }
    }

    if (operator.equals("+") || operator.equals("-") || operator.equals("*")) {
      if (left.isNumeric() && right.isNumeric()) {
        if (left.isFloat() || right.isFloat()) {
          return types.FLOAT;
        } else {
          return types.INT;
        }
      }
    }

    if (operator.equals("&&") || operator.equals("||")) {
      if (left.isBool() && right.isBool()) {
        return types.BOOL;
      }
    }

    if (operator.equals("&") || operator.equals("|") || operator.equals("^") || operator.equals("<<") || operator.equals(">>") || operator.equals(">>>")) {
      if (left.isInt() && right.isInt()) {
        return types.INT;
      }
    }

    if (
      operator.equals("==") || operator.equals("!=") ||
      operator.equals("<") || operator.equals("<=") ||
      operator.equals(">") || operator.equals(">=")
      ) {
      return types.BOOL;
    }

    context.addError(
      elementContext,
      "Can't apply operator " + operator + " for types " + left + " and " + right
    );

    return types.DYNAMIC;
  }
}
