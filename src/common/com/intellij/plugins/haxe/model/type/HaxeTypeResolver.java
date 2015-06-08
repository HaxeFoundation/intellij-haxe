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
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxeNamedComponent;
import com.intellij.plugins.haxe.util.HaxePsiUtils;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class HaxeTypeResolver {
  // @TODO: Check if cache works
  static public SpecificTypeReference getFieldOrMethodReturnType(AbstractHaxeNamedComponent comp) {
    // @TODO: cache should check if any related type has changed, which return depends
    if (comp == null) return createPrimitiveType("Unknown", comp, null);
    long stamp = comp.getContainingFile().getModificationStamp();
    if (comp._cachedType == null || comp._cachedTypeStamp != stamp) {
      comp._cachedType = _getFieldOrMethodReturnType(comp);
      comp._cachedTypeStamp = stamp;
    }

    return comp._cachedType;
  }

  static public SpecificFunctionReference getMethodFunctionType(PsiElement comp) {
    if (comp instanceof HaxeMethod) {
      HaxeParameterList parameterList = HaxePsiUtils.getChild(comp, HaxeParameterList.class);
      ArrayList<SpecificTypeReference> arguments = new ArrayList<SpecificTypeReference>();
      if (parameterList.getParameterList().size() == 0) {
        arguments.add(createPrimitiveType("Void", comp, null));
      } else {
        for (HaxeParameter parameter : parameterList.getParameterList()) {
          arguments.add(SpecificHaxeClassReference.ensure(getTypeFromTypeTag((AbstractHaxeNamedComponent)parameter)));
        }
      }
      arguments.add(getFieldOrMethodReturnType((AbstractHaxeNamedComponent)comp));

      return new SpecificFunctionReference(arguments.toArray(new SpecificTypeReference[0]));
    }
    return null;
  }

  static private SpecificTypeReference _getFieldOrMethodReturnType(AbstractHaxeNamedComponent comp) {
    try {
      if (comp instanceof PsiMethod) {
        return SpecificHaxeClassReference.ensure(getFunctionReturnType(comp));
      } else {
        return SpecificHaxeClassReference.ensure(getFieldType(comp));
      }
    } catch (Throwable e) {
      e.printStackTrace();
      return createPrimitiveType("Unknown", comp, null);
    }
  }

  static private SpecificTypeReference getFieldType(AbstractHaxeNamedComponent comp) {
    SpecificTypeReference type = getTypeFromTypeTag(comp);
    if (type != null) return type;
    // Here detect assignment
    if (comp instanceof HaxeVarDeclarationPart) {
      HaxeVarInit init = ((HaxeVarDeclarationPart)comp).getVarInit();
      if (init != null) {
        PsiElement child = init.getExpression();
        SpecificTypeReference type1 = HaxeTypeResolver.getPsiElementType(child);
        HaxeVarDeclaration decl = ((HaxeVarDeclaration)comp.getParent());
        boolean isConstant = false;
        if (decl != null) {
          isConstant = decl.hasModifierProperty(HaxePsiModifier.INLINE);
          PsiModifierList modifierList = decl.getModifierList();
          //System.out.println(decl.getText());
        }
        return isConstant ? type1 : type1.withConstantValue(null);
      }
    }

    return null;
  }

  static private SpecificTypeReference getFunctionReturnType(AbstractHaxeNamedComponent comp) {
    SpecificTypeReference type = getTypeFromTypeTag(comp);
    if (type != null) return type;

    if (comp instanceof HaxeMethod) {
      return getPsiElementType(comp.getLastChild());
    } else {
      throw new RuntimeException("Can't get the body of a no PsiMethod");
    }
  }

  static public SpecificTypeReference getTypeFromTypeTag(final HaxeTypeTag typeTag) {
    if (typeTag == null) return null;
    final HaxeTypeOrAnonymous typeOrAnonymous = typeTag.getTypeOrAnonymous();
    final HaxeFunctionType functionType = typeTag.getFunctionType();

    if (typeOrAnonymous != null) {
      return getTypeFromTypeOrAnonymous(typeOrAnonymous);
    }

    //comp.getContainingFile().getNode().putUserData();

    if (functionType != null) {
      return getTypeFromFunctionType(functionType);
    }

    return null;

  }

  static public SpecificTypeReference getTypeFromTypeTag(AbstractHaxeNamedComponent comp) {
    return getTypeFromTypeTag(PsiTreeUtil.getChildOfType(comp, HaxeTypeTag.class));
  }

  private static SpecificTypeReference getTypeFromFunctionType(HaxeFunctionType type) {
    ArrayList<SpecificTypeReference> references = new ArrayList<SpecificTypeReference>();
    for (HaxeTypeOrAnonymous anonymous : type.getTypeOrAnonymousList()) {
      references.add(getTypeFromTypeOrAnonymous(anonymous));
    }
    return new SpecificFunctionReference(references.toArray(new SpecificTypeReference[0]));
  }

  static private SpecificTypeReference getTypeFromType(@NotNull HaxeType type) {
    //System.out.println("Type:" + type);
    //System.out.println("Type:" + type.getText());
    HaxeReferenceExpression expression = type.getReferenceExpression();
    HaxeClassReference reference = new HaxeClassReference(expression.getText(), expression);
    HaxeTypeParam param = type.getTypeParam();
    ArrayList<SpecificTypeReference> references = new ArrayList<SpecificTypeReference>();
    if (param != null) {
      for (HaxeTypeListPart part : param.getTypeList().getTypeListPartList()) {
        for (HaxeTypeOrAnonymous anonymous : part.getTypeOrAnonymousList()) {
          references.add(getTypeFromTypeOrAnonymous(anonymous));
        }
      }
    }
    //type.getTypeParam();
    return SpecificHaxeClassReference.withGenerics(reference, references.toArray(SpecificHaxeClassReference.EMPTY));
  }

  static private SpecificTypeReference getTypeFromTypeOrAnonymous(@NotNull HaxeTypeOrAnonymous typeOrAnonymous) {
    // @TODO: Do a proper type resolving
    HaxeType type = typeOrAnonymous.getType();
    if (type != null) {
      return getTypeFromType(type);
    }
    return null;
  }

  @NotNull
  static public SpecificTypeReference getPsiElementType(PsiElement element) {
    if (element == null) {
      System.out.println("getPsiElementType: " + element);
      return createPrimitiveType("Unknown", element, null);
    }
    //System.out.println("Handling element: " + element.getClass());
    if (element instanceof PsiCodeBlock) {
      SpecificTypeReference type = null;
      for (PsiElement childElement : element.getChildren()) {
        type = getPsiElementType(childElement);
        if (childElement instanceof HaxeReturnStatement) {
          //System.out.println("HaxeReturnStatement:" + type);
          return type;
        }
      }
      return type;
    }

    if (element instanceof HaxeReturnStatement) {
      return getPsiElementType(element.getChildren()[0]);
    }

    if (element instanceof HaxeNewExpression) {
      return getTypeFromType(((HaxeNewExpression)element).getType());
    }

    if (element instanceof HaxeThisExpression) {
      PsiReference reference = element.getReference();
      HaxeClassResolveResult result = HaxeResolveUtil.getHaxeClassResolveResult(element);
      HaxeClass ancestor = HaxePsiUtils.getAncestor(element, HaxeClass.class);
      return createPrimitiveType(ancestor.getQualifiedName(), element, null);
    }

    if (element instanceof HaxeIdentifier) {
      PsiReference reference = element.getReference();
      System.out.println(reference);
    }

    if (element instanceof HaxeReferenceExpression) {
      PsiElement[] children = element.getChildren();
      SpecificTypeReference type = getPsiElementType(children[0]);
      for (int n = 1; n < children.length; n++) {
        type = type.access(children[n].getText());
      }
      return type;
    }

    if (element instanceof HaxeCallExpression) {
      SpecificTypeReference functionType = getPsiElementType(((HaxeCallExpression)element).getExpression());
      if (functionType instanceof SpecificFunctionReference) {
        return ((SpecificFunctionReference)functionType).getReturnType();
      }
      // @TODO: resolve the function type return type
      return createPrimitiveType("Unknown", element, null);
    }

    if (element instanceof HaxeLiteralExpression) {
      return getPsiElementType(element.getFirstChild());
    }

    if (element instanceof HaxeStringLiteralExpression) {
      // @TODO: check if it has string interpolation inside, in that case text is not constant
      return createPrimitiveType("String", element, ((HaxeStringLiteralExpression)element).getCanonicalText());
    }

    if (element instanceof PsiJavaToken) {
      IElementType type = ((PsiJavaToken)element).getTokenType();

      if (type == HaxeTokenTypes.LITINT || type == HaxeTokenTypes.LITHEX || type == HaxeTokenTypes.LITOCT) {
        return createPrimitiveType("Int", element, Integer.decode(element.getText()));
      } else if (type == HaxeTokenTypes.LITFLOAT) {
        return createPrimitiveType("Float", element, Float.parseFloat(element.getText()));
      } else if (type == HaxeTokenTypes.KFALSE || type == HaxeTokenTypes.KTRUE) {
        return createPrimitiveType("Bool", element, type == HaxeTokenTypes.KTRUE);
      } else if (type == HaxeTokenTypes.KNULL) {
        return createPrimitiveType("Dynamic", element, HaxeNull.instance);
      } else {
        //System.out.println("Unhandled token type: " + tokenType);
        return createPrimitiveType("Dynamic", element, null);
      }
    }

    if (element instanceof HaxeIfStatement) {
      PsiElement[] children = element.getChildren();
      if (children.length >= 3) {
        return HaxeTypeUnifier.unify(getPsiElementType(children[1]), getPsiElementType(children[2]));
      } else {
        return getPsiElementType(children[1]);
      }
    }

    if (element instanceof HaxeParenthesizedExpression) {
      return getPsiElementType(element.getChildren()[0]);
    }

    if (element instanceof HaxeTernaryExpression) {
      HaxeExpression[] list = ((HaxeTernaryExpression)element).getExpressionList().toArray(new HaxeExpression[0]);
      return HaxeTypeUnifier.unify(getPsiElementType(list[1]), getPsiElementType(list[2]));
    }

    if (element instanceof HaxePrefixExpression) {
      HaxeExpression expression = ((HaxePrefixExpression)element).getExpression();
      if (expression == null) {
        return getPsiElementType(element.getFirstChild());
      } else {
        SpecificTypeReference type = getPsiElementType(expression);
        if (type.getConstant() != null) {
          String operatorText = getOperator(element, HaxeTokenTypeSets.OPERATORS);
          return type.withConstantValue(applyUnaryOperator(type.getConstant(), operatorText));
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
        return getBinaryOperatorResult(getPsiElementType(children[0]), getPsiElementType(children[2]), operatorText);
      } else {
        operatorText = getOperator(element, HaxeTokenTypeSets.OPERATORS);
        return getBinaryOperatorResult(getPsiElementType(children[0]), getPsiElementType(children[1]), operatorText);
      }
    }

    System.out.println("Unhandled " + element.getClass());
    return createPrimitiveType("Dynamic", element, null);
  }

  static private SpecificHaxeClassReference createPrimitiveType(String type, PsiElement element, Object constant) {
    return SpecificHaxeClassReference.withoutGenerics(new HaxeClassReference(type, element), constant);
  }

  static private SpecificTypeReference getBinaryOperatorResult(SpecificTypeReference left, SpecificTypeReference right, String operator) {
    PsiElement elementContext = left.getElementContext();
    SpecificTypeReference result = HaxeTypeUnifier.unify(left, right);
    if (operator.equals("/")) result = createPrimitiveType("Float", elementContext, null);
    if (
      operator.equals("==") || operator.equals("!=") ||
      operator.equals("<") || operator.equals("<=") ||
      operator.equals(">") || operator.equals(">=")
    ) {
      result = createPrimitiveType("Bool", elementContext, null);
    }
    // @TODO: Check operator overloading
    if (left.getConstant() != null && right.getConstant() != null) {
      result = result.withConstantValue(applyBinOperator(left.getConstant(), right.getConstant(), operator));
    }
    return result;
  }

  static public double getDoubleValue(Object value) {
    if (value instanceof Long) return (Long)value;
    if (value instanceof Integer) return (Integer)value;
    if (value instanceof Double) return (Double)value;
    if (value instanceof Float) return (Float)value;
    return Double.NaN;
  }

  private static Object applyUnaryOperator(Object right, String operator) {
    double rightv = getDoubleValue(right);
    if (operator.equals("-")) return -rightv;
    if (operator.equals("~")) return ~(int)rightv;
    if (operator.equals("")) return rightv;
    throw new RuntimeException("Unsupporteed operator '" + operator + "'");
  }

  static public Object applyBinOperator(Object left, Object right, String operator) {
    double leftv = getDoubleValue(left);
    double rightv = getDoubleValue(right);
    if (operator.equals("+")) return leftv + rightv;
    if (operator.equals("-")) return leftv - rightv;
    if (operator.equals("*")) return leftv * rightv;
    if (operator.equals("/")) return leftv / rightv;
    if (operator.equals("%")) return leftv % rightv;
    if (operator.equals("==")) return leftv == rightv;
    if (operator.equals("!=")) return leftv != rightv;
    if (operator.equals("<")) return leftv < rightv;
    if (operator.equals("<=")) return leftv <= rightv;
    if (operator.equals(">")) return leftv > rightv;
    if (operator.equals(">=")) return leftv >= rightv;
    if (operator.equals("<<")) return (int)leftv << (int)rightv;
    if (operator.equals(">>")) return (int)leftv >> (int)rightv;
    if (operator.equals("&")) return (int)leftv & (int)rightv;
    if (operator.equals("|")) return (int)leftv | (int)rightv;
    throw new RuntimeException("Unsupporteed operator '" + operator + "'");
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
