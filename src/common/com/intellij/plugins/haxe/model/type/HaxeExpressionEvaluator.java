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
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxePsiUtils;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaToken;
import com.intellij.psi.PsiReference;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

public class HaxeExpressionEvaluator {
  static private SpecificHaxeClassReference getUnknown(PsiElement element) {
    return SpecificHaxeClassReference.primitive("Unknown", element);
  }

  static private SpecificHaxeClassReference getVoid(PsiElement element) {
    return SpecificHaxeClassReference.primitive("Void", element);
  }

  static private SpecificHaxeClassReference getDynamic(PsiElement element) {
    return SpecificHaxeClassReference.primitive("Dynamic", element);
  }

  static public HaxeExpressionEvaluatorContext evaluate(PsiElement element, HaxeExpressionEvaluatorContext context) {
    context.result = handle(element, context);
    return context;
  }

  static private SpecificTypeReference handle(PsiElement element, HaxeExpressionEvaluatorContext context) {
    if (element == null) {
      System.out.println("getPsiElementType: " + element);
      return getUnknown(element);
    }
    //System.out.println("Handling element: " + element.getClass());
    if (element instanceof PsiCodeBlock) {
      SpecificTypeReference type = getUnknown(element);
      for (PsiElement childElement : element.getChildren()) {
        type = handle(childElement, context);
      }
      return type;
    }

    if (element instanceof HaxeReturnStatement) {
      PsiElement[] children = element.getChildren();
      if (children.length == 0) {
        return getVoid(element);
      }
      SpecificTypeReference result = handle(children[0], context);
      context.returns.add(result);
      return result;
    }

    if (element instanceof HaxeNewExpression) {
      return HaxeTypeResolver.getTypeFromType(((HaxeNewExpression)element).getType());
    }

    if (element instanceof HaxeThisExpression) {
      PsiReference reference = element.getReference();
      HaxeClassResolveResult result = HaxeResolveUtil.getHaxeClassResolveResult(element);
      HaxeClass ancestor = HaxePsiUtils.getAncestor(element, HaxeClass.class);
      return SpecificHaxeClassReference.primitive(ancestor.getQualifiedName(), element);
    }

    if (element instanceof HaxeIdentifier) {
      PsiReference reference = element.getReference();
      System.out.println("HaxeIdentifier:" + reference);
    }

    if (element instanceof HaxeLocalVarDeclaration) {
      for (HaxeLocalVarDeclarationPart part : ((HaxeLocalVarDeclaration)element).getLocalVarDeclarationPartList()) {
        handle(part, context);
      }
      return getUnknown(element);
    }

    if (element instanceof HaxeLocalVarDeclarationPart) {
      SpecificTypeReference result = handle(((HaxeLocalVarDeclarationPart)element).getVarInit(), context);
      HaxeTypeTag typeTag = ((HaxeLocalVarDeclarationPart)element).getTypeTag();
      if (typeTag != null) {
        SpecificTypeReference tag = HaxeTypeResolver.getTypeFromTypeTag(typeTag);
        if (!tag.isAssignableFrom(result)) {
          context.addError(element, "Can't assign " + result + " to " + tag);
        }
      }
      return result;
    }

    if (element instanceof HaxeVarInit) {
      return handle(((HaxeVarInit)element).getExpression(), context);
    }

    if (element instanceof HaxeReferenceExpression) {
      PsiElement[] children = element.getChildren();
      SpecificTypeReference type = handle(children[0], context);
      for (int n = 1; n < children.length; n++) {
        if (type != null) type = type.access(children[n].getText());
      }
      return SpecificHaxeClassReference.ensure(type);
    }

    if (element instanceof HaxeCallExpression) {
      SpecificTypeReference functionType = handle(((HaxeCallExpression)element).getExpression(), context);
      if (functionType instanceof SpecificFunctionReference) {
        return ((SpecificFunctionReference)functionType).getReturnType();
      }
      // @TODO: resolve the function type return type
      return getUnknown(element);
    }

    if (element instanceof HaxeLiteralExpression) {
      return handle(element.getFirstChild(), context);
    }

    if (element instanceof HaxeStringLiteralExpression) {
      // @TODO: check if it has string interpolation inside, in that case text is not constant
      return SpecificHaxeClassReference.primitive("String", element, ((HaxeStringLiteralExpression)element).getCanonicalText());
    }

    if (element instanceof HaxeExpressionList) {
      ArrayList<SpecificTypeReference> references = new ArrayList<SpecificTypeReference>();
      for (HaxeExpression expression : ((HaxeExpressionList)element).getExpressionList()) {
        references.add(handle(expression, context));
      }
      return HaxeTypeUnifier.unify(references);
    }

    if (element instanceof HaxeArrayLiteral) {
      HaxeExpressionList list = ((HaxeArrayLiteral)element).getExpressionList();
      ArrayList<SpecificTypeReference> references = new ArrayList<SpecificTypeReference>();
      ArrayList<Object> constants = new ArrayList<Object>();
      boolean allConstants = true;
      if (list != null) {
        for (HaxeExpression expression : list.getExpressionList()) {
          SpecificTypeReference type = handle(expression, context);
          if (!type.hasConstant()) {
            allConstants = false;
          } else {
            constants.add(type.getConstant());
          }
          references.add(type);
        }
      }
      SpecificTypeReference result = SpecificHaxeClassReference.createArray(HaxeTypeUnifier.unify(references).withoutConstantValue());
      if (allConstants) result = result.withConstantValue(constants);
      return result;
    }

    if (element instanceof PsiJavaToken) {
      IElementType type = ((PsiJavaToken)element).getTokenType();

      if (type == HaxeTokenTypes.LITINT || type == HaxeTokenTypes.LITHEX || type == HaxeTokenTypes.LITOCT) {
        return SpecificHaxeClassReference.primitive("Int", element, Integer.decode(element.getText()));
      } else if (type == HaxeTokenTypes.LITFLOAT) {
        return SpecificHaxeClassReference.primitive("Float", element, Float.parseFloat(element.getText()));
      } else if (type == HaxeTokenTypes.KFALSE || type == HaxeTokenTypes.KTRUE) {
        return SpecificHaxeClassReference.primitive("Bool", element, type == HaxeTokenTypes.KTRUE);
      } else if (type == HaxeTokenTypes.KNULL) {
        return SpecificHaxeClassReference.primitive("Dynamic", element, HaxeNull.instance);
      } else {
        //System.out.println("Unhandled token type: " + tokenType);
        return SpecificHaxeClassReference.primitive("Dynamic", element, null);
      }
    }

    if (element instanceof HaxeIfStatement) {
      PsiElement[] children = element.getChildren();
      if (children.length >= 3) {
        return HaxeTypeUnifier.unify(handle(children[1], context), handle(children[2], context));
      } else {
        return handle(children[1], context);
      }
    }

    if (element instanceof HaxeParenthesizedExpression) {
      return handle(element.getChildren()[0], context);
    }

    if (element instanceof HaxeTernaryExpression) {
      HaxeExpression[] list = ((HaxeTernaryExpression)element).getExpressionList().toArray(new HaxeExpression[0]);
      return HaxeTypeUnifier.unify(handle(list[1], context), handle(list[2], context));
    }

    if (element instanceof HaxePrefixExpression) {
      HaxeExpression expression = ((HaxePrefixExpression)element).getExpression();
      if (expression == null) {
        return handle(element.getFirstChild(), context);
      } else {
        SpecificTypeReference type = handle(expression, context);
        if (type.getConstant() != null) {
          String operatorText = getOperator(element, HaxeTokenTypeSets.OPERATORS);
          return type.withConstantValue(HaxeTypeUtils.applyUnaryOperator(type.getConstant(), operatorText));
        }
        return type;
      }
    }

    if (
      (element instanceof HaxeAdditiveExpression) ||
      (element instanceof HaxeBitwiseExpression) ||
      (element instanceof HaxeShiftExpression) ||
      (element instanceof HaxeLogicAndExpression) ||
      (element instanceof HaxeLogicOrExpression) ||
      (element instanceof HaxeCompareExpression) ||
      (element instanceof HaxeMultiplicativeExpression)
      ) {
      PsiElement[] children = element.getChildren();
      String operatorText;
      if (children.length == 3) {
        operatorText = children[1].getText();
        return getBinaryOperatorResult(element, handle(children[0], context), handle(children[2], context), operatorText, context);
      } else {
        operatorText = getOperator(element, HaxeTokenTypeSets.OPERATORS);
        return getBinaryOperatorResult(element, handle(children[0], context), handle(children[1], context), operatorText, context);
      }
    }

    System.out.println("Unhandled " + element.getClass());
    return getDynamic(element);
  }


  static private SpecificTypeReference getBinaryOperatorResult(PsiElement element, SpecificTypeReference left, SpecificTypeReference right, String operator, HaxeExpressionEvaluatorContext context) {
    if (!HaxeTypeCompatible.canApplyBinaryOperator(left, right, operator)) {
      context.addError(element, "Can't apply operator " + operator + " for types " + left + " and " + right);
    }

    PsiElement elementContext = left.getElementContext();
    SpecificTypeReference result = HaxeTypeUnifier.unify(left, right);
    if (operator.equals("/")) result = SpecificHaxeClassReference.primitive("Float", elementContext, null);

    if (operator.equals("+")) {
      if (left.toStringWithoutConstant().equals("String") || right.toStringWithoutConstant().equals("String")) {
        return SpecificHaxeClassReference.primitive("String", element);
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

  static private String getOperator(PsiElement element, TokenSet set) {
    ASTNode operatorNode = element.getNode().findChildByType(set);
    if (operatorNode == null) return "";
    return operatorNode.getText();
  }

  static private String getOperator(PsiElement element, IElementType... operators) {
    return getOperator(element, TokenSet.create(operators));
  }
}
