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

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;

public class HaxeOperatorResolver {
  static public SpecificTypeReference getBinaryOperatorResult(
    PsiElement elementContext,
    SpecificTypeReference left,
    SpecificTypeReference right,
    String operator,
    HaxeExpressionEvaluatorContext context
  ) {
    if (!HaxeTypeCompatible.canApplyBinaryOperator(left, right, operator)) {
      context.addError(
        elementContext,
        "Can't apply operator " + operator + " for types " + left + " and " + right
      );
    }

    SpecificTypeReference result = result = HaxeTypeUnifier.unify(left, right, context.root);
    if (operator.equals("/")) result = SpecificHaxeClassReference.primitive("Float", elementContext, null);

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
    // @TODO: Check operator overloading
    if (left.getConstant() != null && right.getConstant() != null) {
      result = result.withConstantValue(HaxeTypeUtils.applyBinOperator(left.getConstant(), right.getConstant(), operator));
    }
    return result;
  }
}
