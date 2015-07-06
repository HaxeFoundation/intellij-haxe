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
import com.intellij.lang.annotation.Annotation;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxeNamedComponent;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.fixer.*;
import com.intellij.plugins.haxe.util.HaxeJavaUtil;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.plugins.haxe.util.HaxeStringUtil;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaToken;
import com.intellij.psi.PsiReference;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class HaxeExpressionEvaluator {
  @NotNull
  static public HaxeExpressionEvaluatorContext evaluate(PsiElement element, HaxeExpressionEvaluatorContext context) {
    context.result = handle(element, context);
    return context;
  }

  @NotNull
  static private ResultHolder handle(final PsiElement element, final HaxeExpressionEvaluatorContext context) {
    try {
      return _handle(element, context);
    }
    catch (Throwable t) {
      t.printStackTrace();
      return SpecificHaxeClassReference.getUnknown(element).createHolder();
    }
  }

  @NotNull
  static private ResultHolder _handle(final PsiElement element, final HaxeExpressionEvaluatorContext context) {
    if (element == null) {
      System.out.println("getPsiElementType: " + element);
      return SpecificHaxeClassReference.getUnknown(element).createHolder();
    }
    //System.out.println("Handling element: " + element.getClass());
    if (element instanceof PsiCodeBlock) {
      context.beginScope();
      ResultHolder type = SpecificHaxeClassReference.getUnknown(element).createHolder();
      boolean deadCode = false;
      for (PsiElement childElement : element.getChildren()) {
        type = handle(childElement, context);
        if (deadCode) {
          //context.addWarning(childElement, "Unreachable statement");
          context.addUnreachable(childElement);
        }
        if (childElement instanceof HaxeReturnStatement) {
          deadCode = true;
        }
      }
      context.endScope();
      return type;
    }

    if (element instanceof HaxeReturnStatement) {
      PsiElement[] children = element.getChildren();
      ResultHolder result = SpecificHaxeClassReference.getVoid(element).createHolder();
      if (children.length >= 1) {
        result = handle(children[0], context);
      }
      context.addReturnType(result, element);
      return result;
    }

    if (element instanceof HaxeIterable) {
      return handle(((HaxeIterable)element).getExpression(), context);
    }

    if (element instanceof HaxeForStatement) {
      final HaxeComponentName name = ((HaxeForStatement)element).getComponentName();
      final HaxeIterable iterable = ((HaxeForStatement)element).getIterable();
      final PsiElement body = element.getLastChild();
      context.beginScope();
      try {
        final SpecificTypeReference iterableValue = handle(iterable, context).getType();
        SpecificTypeReference type = iterableValue.getIterableElementType(iterableValue).getType();
        if (iterableValue.isConstant()) {
          final Object constant = iterableValue.getConstant();
          if (constant instanceof HaxeRange) {
            type = type.withRangeConstraint((HaxeRange)constant);
          }
        }
        if (name != null) {
          context.setLocal(name.getText(), new ResultHolder(type));
        }
        return handle(body, context);
        //System.out.println(name);
        //System.out.println(iterable);
      }
      finally {
        context.endScope();
      }
    }

    if (element instanceof HaxeNewExpression) {
      ResultHolder typeHolder = HaxeTypeResolver.getTypeFromType(((HaxeNewExpression)element).getType());
      if (typeHolder.getType() instanceof SpecificHaxeClassReference) {
        final HaxeClassModel clazz = ((SpecificHaxeClassReference)typeHolder.getType()).getHaxeClassModel();
        if (clazz != null) {
          HaxeMethodModel constructor = clazz.getConstructor();
          if (constructor == null) {
            context.addError(element, "Class " + clazz.getName() + " doesn't have a constructor", new HaxeFixer("Create constructor") {
              @Override
              public void run() {
                // @TODO: Check arguments
                clazz.addMethod("new");
              }
            });
          }
          else {
            checkParameters(element, constructor, ((HaxeNewExpression)element).getExpressionList(), context);
          }
        }
      }
      return typeHolder.duplicate();
    }

    if (element instanceof HaxeThisExpression) {
      //PsiReference reference = element.getReference();
      //HaxeClassResolveResult result = HaxeResolveUtil.getHaxeClassResolveResult(element);
      HaxeClass ancestor = UsefulPsiTreeUtil.getAncestor(element, HaxeClass.class);
      if (ancestor == null) return SpecificTypeReference.getDynamic(element).createHolder();
      HaxeClassModel model = ancestor.getModel();
      if (model.isAbstract()) {
        HaxeTypeOrAnonymous type = model.getAbstractUnderlyingType();
        if (type != null) {
          HaxeClass aClass = HaxeResolveUtil.tryResolveClassByQName(type);
          if (aClass != null) {
            return SpecificHaxeClassReference.withoutGenerics(new HaxeClassReference(aClass.getModel(), element), element).createHolder();
          }
        }
      }
      return SpecificHaxeClassReference.primitive(ancestor.getQualifiedName(), element).createHolder();
    }

    if (element instanceof HaxeIdentifier) {
      //PsiReference reference = element.getReference();
      ResultHolder holder = context.get(element.getText());

      if (holder == null) {
        context.addError(element, "Unknown variable", new HaxeCreateLocalVariableFixer(element.getText(), element));

        return SpecificTypeReference.getDynamic(element).createHolder();
      }

      return holder;
      //System.out.println("HaxeIdentifier:" + reference);
    }

    if (element instanceof HaxeCastExpression) {
      handle(((HaxeCastExpression)element).getExpression(), context);
      HaxeTypeOrAnonymous anonymous = ((HaxeCastExpression)element).getTypeOrAnonymous();
      if (anonymous != null) {
        return HaxeTypeResolver.getTypeFromTypeOrAnonymous(anonymous);
      }
      else {
        return SpecificHaxeClassReference.getUnknown(element).createHolder();
      }
    }

    if (element instanceof HaxeWhileStatement) {
      List<HaxeExpression> list = ((HaxeWhileStatement)element).getExpressionList();
      SpecificTypeReference type = null;
      HaxeExpression lastExpression = null;
      for (HaxeExpression expression : list) {
        type = handle(expression, context).getType();
        lastExpression = expression;
      }
      if (type == null) {
        type = SpecificTypeReference.getDynamic(element);
      }
      if (!type.isBool() && lastExpression != null) {
        context.addError(
          lastExpression,
          "While expression must be boolean",
          new HaxeCastFixer(lastExpression, type, SpecificHaxeClassReference.getBool(element))
        );
      }

      PsiElement body = element.getLastChild();
      if (body != null) {
        //return SpecificHaxeClassReference.createArray(result); // @TODO: Check this
        return handle(body, context);
      }

      return SpecificHaxeClassReference.getUnknown(element).createHolder();
    }

    if (element instanceof HaxeLocalVarDeclaration) {
      for (HaxeLocalVarDeclarationPart part : ((HaxeLocalVarDeclaration)element).getLocalVarDeclarationPartList()) {
        handle(part, context);
      }
      return SpecificHaxeClassReference.getUnknown(element).createHolder();
    }

    if (element instanceof HaxeAssignExpression) {
      final PsiElement left = element.getFirstChild();
      final PsiElement right = element.getLastChild();
      if (left != null && right != null) {
        final ResultHolder leftResult = handle(left, context);
        final ResultHolder rightResult = handle(right, context);

        if (leftResult.isUnknown()) {
          leftResult.setType(rightResult.getType());
        }
        leftResult.removeConstant();

        final SpecificTypeReference leftValue = leftResult.getType();
        final SpecificTypeReference rightValue = rightResult.getType();

        //leftValue.mutateConstantValue(null);
        if (!leftResult.canAssign(rightResult)) {
          context.addError(element, "Can't assign " + rightValue + " to " + leftValue, new HaxeCastFixer(right, rightValue, leftValue));
        }

        if (leftResult.isImmutable()) {
          context.addError(element, "Trying to change an immutable value");
        }

        return rightResult;
      }
      return SpecificHaxeClassReference.getUnknown(element).createHolder();
    }

    if (element instanceof HaxeLocalVarDeclarationPart) {
      final HaxeComponentName name = ((HaxeLocalVarDeclarationPart)element).getComponentName();
      final HaxeVarInit init = ((HaxeLocalVarDeclarationPart)element).getVarInit();
      final HaxeTypeTag typeTag = ((HaxeLocalVarDeclarationPart)element).getTypeTag();
      ResultHolder result = SpecificHaxeClassReference.getUnknown(element).createHolder();
      if (init != null) {
        result = handle(init, context);
      }
      if (typeTag != null) {
        result = HaxeTypeResolver.getTypeFromTypeTag(typeTag, element);
      }

      if (typeTag != null) {
        final ResultHolder tag = HaxeTypeResolver.getTypeFromTypeTag(typeTag, element);
        if (!tag.canAssign(result)) {
          result = tag.duplicate();

          context.addError(
            element,
            "Can't assign " + result + " to " + tag,
            new HaxeTypeTagChangeFixer(typeTag, result.getType()),
            new HaxeTypeTagRemoveFixer(typeTag)
          );
        }
      }

      if (name != null) {
        context.setLocal(name.getText(), result);
      }

      return result;
    }

    if (element instanceof HaxeVarInit) {
      return handle(((HaxeVarInit)element).getExpression(), context);
    }

    if (element instanceof HaxeReferenceExpression) {
      PsiElement[] children = element.getChildren();
      ResultHolder typeHolder = handle(children[0], context);
      boolean resolved = true;
      for (int n = 1; n < children.length; n++) {
        String accessName = children[n].getText();
        if (typeHolder.getType().isString() && typeHolder.getType().isConstant() && accessName.equals("code")) {
          String str = (String)typeHolder.getType().getConstant();
          typeHolder = SpecificTypeReference.getInt(element, (str != null && str.length() >= 1) ? str.charAt(0) : -1).createHolder();
          if (str == null || str.length() != 1) {
            context.addError(element, "String must be a single UTF8 char");
          }
        }
        else {
          ResultHolder access = typeHolder.getType().access(accessName, context);
          if (access == null) {
            resolved = false;
            Annotation annotation = context.addError(children[n], "Can't resolve '" + accessName + "' in " + typeHolder.getType());
            if (children.length == 1) {
              annotation.registerFix(new HaxeCreateLocalVariableFixer(accessName, element));
            } else {
              annotation.registerFix(new HaxeCreateMethodFixer(accessName, element));
              annotation.registerFix(new HaxeCreateFieldFixer(accessName, element));
            }
          }
          typeHolder = access;
        }
      }

      // @TODO: this should be innecessary when code is working right!
      if (!resolved) {
        PsiReference reference = element.getReference();
        if (reference != null) {
          PsiElement subelement = reference.resolve();
          if (subelement instanceof AbstractHaxeNamedComponent) {
            typeHolder = HaxeTypeResolver.getFieldOrMethodReturnType((AbstractHaxeNamedComponent)subelement);
          }
        }
      }

      return (typeHolder != null) ? typeHolder : SpecificTypeReference.getDynamic(element).createHolder();
    }

    if (element instanceof HaxeCallExpression) {
      HaxeCallExpression callelement = (HaxeCallExpression)element;
      HaxeExpression callLeft = ((HaxeCallExpression)element).getExpression();
      SpecificTypeReference functionType = handle(callLeft, context).getType();

      // @TODO: this should be innecessary when code is working right!
      if (functionType.isUnknown()) {
        if (callLeft instanceof HaxeReference) {
          PsiReference reference = callLeft.getReference();
          if (reference != null) {
            PsiElement subelement = reference.resolve();
            if (subelement instanceof HaxeMethod) {
              functionType = ((HaxeMethod)subelement).getModel().getFunctionType();
            }
          }
        }
      }

      if (functionType.isUnknown()) {
        //System.out.println("Couldn't resolve " + callLeft.getText());
      }

      List<HaxeExpression> parameterExpressions = null;
      if (callelement.getExpressionList() != null) {
        parameterExpressions = callelement.getExpressionList().getExpressionList();
      }
      else {
        parameterExpressions = Collections.emptyList();
      }

      if (functionType instanceof SpecificFunctionReference) {
        SpecificFunctionReference ftype = (SpecificFunctionReference)functionType;
        HaxeExpressionEvaluator.checkParameters(callelement, ftype, parameterExpressions, context);

        return ftype.getReturnType().duplicate();
      }

      if (functionType.isDynamic()) {
        for (HaxeExpression expression : parameterExpressions) {
          handle(expression, context);
        }

        return functionType.withoutConstantValue().createHolder();
      }

      // @TODO: resolve the function type return type
      return SpecificHaxeClassReference.getUnknown(element).createHolder();
    }

    if (element instanceof HaxeLiteralExpression) {
      return handle(element.getFirstChild(), context);
    }

    if (element instanceof HaxeStringLiteralExpression) {
      // @TODO: check if it has string interpolation inside, in that case text is not constant
      return SpecificHaxeClassReference.primitive(
        "String",
        element,
        HaxeStringUtil.unescapeString(element.getText())
      ).createHolder();
    }

    if (element instanceof HaxeExpressionList) {
      ArrayList<ResultHolder> references = new ArrayList<ResultHolder>();
      for (HaxeExpression expression : ((HaxeExpressionList)element).getExpressionList()) {
        references.add(handle(expression, context));
      }
      return HaxeTypeUnifier.unifyHolders(references, element);
    }

    if (element instanceof HaxeArrayLiteral) {
      HaxeExpressionList list = ((HaxeArrayLiteral)element).getExpressionList();
      if (list != null) {
        final List<HaxeExpression> list1 = list.getExpressionList();
        if (list1.isEmpty()) {
          final PsiElement child = list.getFirstChild();
          if ((child instanceof HaxeForStatement) || (child instanceof HaxeWhileStatement)) {
            return SpecificTypeReference.createArray(handle(child, context)).createHolder();
          }
        }
      }
      ArrayList<SpecificTypeReference> references = new ArrayList<SpecificTypeReference>();
      ArrayList<Object> constants = new ArrayList<Object>();
      boolean allConstants = true;
      if (list != null) {
        for (HaxeExpression expression : list.getExpressionList()) {
          SpecificTypeReference type = handle(expression, context).getType();
          if (!type.isConstant()) {
            allConstants = false;
          }
          else {
            constants.add(type.getConstant());
          }
          references.add(type);
        }
      }

      ResultHolder elementTypeHolder = HaxeTypeUnifier.unify(references, element).withoutConstantValue().createHolder();

      SpecificTypeReference result = SpecificHaxeClassReference.createArray(elementTypeHolder);
      if (allConstants) result = result.withConstantValue(constants);
      ResultHolder holder = result.createHolder();
      return holder;
    }

    if (element instanceof PsiJavaToken) {
      IElementType type = ((PsiJavaToken)element).getTokenType();

      if (type == HaxeTokenTypes.LITINT || type == HaxeTokenTypes.LITHEX || type == HaxeTokenTypes.LITOCT) {
        return SpecificHaxeClassReference.primitive("Int", element, Integer.decode(element.getText())).createHolder();
      }
      else if (type == HaxeTokenTypes.LITFLOAT) {
        return SpecificHaxeClassReference.primitive("Float", element, Float.parseFloat(element.getText())).createHolder();
      }
      else if (type == HaxeTokenTypes.KFALSE || type == HaxeTokenTypes.KTRUE) {
        return SpecificHaxeClassReference.primitive("Bool", element, type == HaxeTokenTypes.KTRUE).createHolder();
      }
      else if (type == HaxeTokenTypes.KNULL) {
        return SpecificHaxeClassReference.primitive("Dynamic", element, HaxeNull.instance).createHolder();
      }
      else {
        //System.out.println("Unhandled token type: " + tokenType);
        return SpecificHaxeClassReference.getDynamic(element).createHolder();
      }
    }

    if (element instanceof HaxeSuperExpression) {
      /*
      System.out.println("-------------------------");
      final HaxeExpressionList list = HaxePsiUtils.getChildWithText(element, HaxeExpressionList.class);
      System.out.println(element);
      System.out.println(list);
      final List<HaxeExpression> parameters = (list != null) ? list.getExpressionList() : Collections.<HaxeExpression>emptyList();
      final HaxeMethodModel method = HaxeJavaUtil.cast(HaxeMethodModel.fromPsi(element), HaxeMethodModel.class);
      if (method == null) {
        context.addError(element, "Not in a method");
      }
      if (method != null) {
        final HaxeMethodModel parentMethod = method.getParentMethod();
        if (parentMethod == null) {
          context.addError(element, "Calling super without parent constructor");
        } else {
          System.out.println(element);
          System.out.println(parentMethod.getFunctionType());
          System.out.println(parameters);
          checkParameters(element, parentMethod.getFunctionType(), parameters, context);
          //System.out.println(method);
          //System.out.println(parentMethod);
        }
      }
      return SpecificHaxeClassReference.getVoid(element);
      */
      final HaxeMethodModel method = HaxeJavaUtil.cast(HaxeMethodModel.fromPsi(element), HaxeMethodModel.class);
      final HaxeMethodModel parentMethod = (method != null) ? method.getParentMethod() : null;
      if (parentMethod != null) {
        return parentMethod.getFunctionType().createHolder();
      }
      context.addError(element, "Calling super without parent constructor");
      return SpecificHaxeClassReference.getUnknown(element).createHolder();
    }

    if (element instanceof HaxeIteratorExpression) {
      final List<HaxeExpression> list = ((HaxeIteratorExpression)element).getExpressionList();
      if (list.size() >= 2) {
        final SpecificTypeReference left = handle(list.get(0), context).getType();
        final SpecificTypeReference right = handle(list.get(1), context).getType();
        Object constant = null;
        if (left.isConstant() && right.isConstant()) {
          constant = new HaxeRange(
            HaxeTypeUtils.getIntValue(left.getConstant()),
            HaxeTypeUtils.getIntValue(right.getConstant())
          );
        }
        return SpecificHaxeClassReference.getIterator(SpecificHaxeClassReference.getInt(element)).withConstantValue(constant)
          .createHolder();
      }
      return SpecificHaxeClassReference.getUnknown(element).createHolder();
    }

    if (element instanceof HaxeArrayAccessExpression) {
      final List<HaxeExpression> list = ((HaxeArrayAccessExpression)element).getExpressionList();
      if (list.size() >= 2) {
        final SpecificTypeReference left = handle(list.get(0), context).getType();
        final SpecificTypeReference right = handle(list.get(1), context).getType();
        if (left.isArray()) {
          Object constant = null;
          if (left.isConstant()) {
            List array = (List)left.getConstant();
            final HaxeRange constraint = right.getRangeConstraint();
            HaxeRange arrayBounds = new HaxeRange(0, array.size());
            if (right.isConstant()) {
              final int index = HaxeTypeUtils.getIntValue(right.getConstant());
              if (arrayBounds.contains(index)) {
                constant = array.get(index);
              }
              else {
                context.addWarning(element, "Out of bounds " + index + " not inside " + arrayBounds);
              }
            }
            else if (constraint != null) {
              if (!arrayBounds.contains(constraint)) {
                context.addWarning(element, "Out of bounds " + constraint + " not inside " + arrayBounds);
              }
            }
          }
          return left.getArrayElementType().getType().withConstantValue(constant).createHolder();
        }
      }
      return SpecificHaxeClassReference.getUnknown(element).createHolder();
    }

    if (element instanceof HaxeFunctionLiteral) {
      HaxeParameterList params = ((HaxeFunctionLiteral)element).getParameterList();
      if (params == null) {
        return SpecificHaxeClassReference.getInvalid(element).createHolder();
      }
      LinkedList<ResultHolder> results = new LinkedList<ResultHolder>();
      ResultHolder returnType = null;
      context.beginScope();
      try {
        for (HaxeParameter parameter : params.getParameterList()) {
          ResultHolder vartype = HaxeTypeResolver.getTypeFromTypeTag(parameter.getTypeTag(), element);
          String name = parameter.getName();
          if (name != null) {
            context.setLocal(name, vartype);
          }
          results.add(vartype);
        }
        context.addLambda(context.createChild(element.getLastChild()));
        returnType = HaxeTypeResolver.getTypeFromTypeTag(((HaxeFunctionLiteral)element).getTypeTag(), element);
      } finally {
        context.endScope();
      }

      return new SpecificFunctionReference(results, returnType, null, element).createHolder();
    }

    if (element instanceof HaxeIfStatement) {
      PsiElement[] children = element.getChildren();
      if (children.length >= 1) {
        SpecificTypeReference expr = handle(children[0], context).getType();
        if (!SpecificTypeReference.getBool(element).canAssign(expr)) {
          context.addError(
            children[0],
            "If expr " + expr + " should be bool",
            new HaxeCastFixer(children[0], expr, SpecificHaxeClassReference.getBool(element))
          );
        }

        if (expr.isConstant()) {
          context.addWarning(children[0], "If expression constant");
        }

        if (children.length < 2) return SpecificHaxeClassReference.getUnknown(element).createHolder();
        PsiElement eTrue = null;
        PsiElement eFalse = null;
        eTrue = children[1];
        if (children.length >= 3) {
          eFalse = children[2];
        }
        SpecificTypeReference tTrue = null;
        SpecificTypeReference tFalse = null;
        if (eTrue != null) tTrue = handle(eTrue, context).getType();
        if (eFalse != null) tFalse = handle(eFalse, context).getType();
        if (expr.isConstant()) {
          if (expr.getConstantAsBool()) {
            if (tFalse != null) {
              context.addUnreachable(eFalse);
            }
          }
          else {
            if (tTrue != null) {
              context.addUnreachable(eTrue);
            }
          }
        }
        return HaxeTypeUnifier.unify(tTrue, tFalse, element).createHolder();
      }
    }

    if (element instanceof HaxeParenthesizedExpression) {
      return handle(element.getChildren()[0], context);
    }

    if (element instanceof HaxeTernaryExpression) {
      HaxeExpression[] list = ((HaxeTernaryExpression)element).getExpressionList().toArray(new HaxeExpression[0]);
      return HaxeTypeUnifier.unify(handle(list[1], context).getType(), handle(list[2], context).getType(), element).createHolder();
    }

    if (element instanceof HaxePrefixExpression) {
      HaxeExpression expression = ((HaxePrefixExpression)element).getExpression();
      if (expression == null) {
        return handle(element.getFirstChild(), context);
      }
      else {
        ResultHolder typeHolder = handle(expression, context);
        SpecificTypeReference type = typeHolder.getType();
        if (type.getConstant() != null) {
          String operatorText = getOperator(element, HaxeTokenTypeSets.OPERATORS);
          return type.withConstantValue(HaxeTypeUtils.applyUnaryOperator(type.getConstant(), operatorText)).createHolder();
        }
        return typeHolder;
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
        return HaxeOperatorResolver.getBinaryOperatorResult(
          element, handle(children[0], context).getType(), handle(children[2], context).getType(),
          operatorText, context
        ).createHolder();
      }
      else {
        operatorText = getOperator(element, HaxeTokenTypeSets.OPERATORS);
        return HaxeOperatorResolver.getBinaryOperatorResult(
          element, handle(children[0], context).getType(), handle(children[1], context).getType(),
          operatorText, context
        ).createHolder();
      }
    }

    System.out.println("Unhandled " + element.getClass());
    return SpecificHaxeClassReference.getDynamic(element).createHolder();
  }

  static private void checkParameters(
    final PsiElement callelement,
    final HaxeMethodModel method,
    final List<HaxeExpression> arguments,
    final HaxeExpressionEvaluatorContext context
  ) {
    checkParameters(callelement, method.getFunctionType(), arguments, context);
  }

  static private void checkParameters(
    PsiElement callelement,
    SpecificFunctionReference ftype,
    List<HaxeExpression> parameterExpressions,
    HaxeExpressionEvaluatorContext context
  ) {
    List<ResultHolder> parameterTypes = ftype.getParameters();
    int len = Math.min(parameterTypes.size(), parameterExpressions.size());
    for (int n = 0; n < len; n++) {
      ResultHolder type = parameterTypes.get(n);
      HaxeExpression expression = parameterExpressions.get(n);
      ResultHolder value = handle(expression, context);

      if (!type.canAssign(value)) {
        context.addError(
          expression,
          "Can't assign " + value + " to " + type,
          new HaxeCastFixer(expression, value.getType(), type.getType())
        );
      }
    }

    //System.out.println(ftype.getDebugString());
    // More parameters than expected
    if (parameterExpressions.size() > parameterTypes.size()) {
      for (int n = parameterTypes.size(); n < parameterExpressions.size(); n++) {
        context.addError(parameterExpressions.get(n), "Unexpected argument");
      }
    }
    // Less parameters than expected
    else if (parameterExpressions.size() < ftype.getNonOptionalArgumentsCount()) {
      context.addError(callelement, "Less arguments than expected");
    }
  }

  static private String getOperator(PsiElement element, TokenSet set) {
    ASTNode operatorNode = element.getNode().findChildByType(set);
    if (operatorNode == null) return "";
    return operatorNode.getText();
  }
}
