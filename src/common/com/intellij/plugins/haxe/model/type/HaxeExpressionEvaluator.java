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

import com.google.common.base.Joiner;
import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.Annotation;
import com.intellij.openapi.util.Key;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.build.HaxeMethodBuilder;
import com.intellij.plugins.haxe.lang.psi.impl.AbstractHaxeNamedComponent;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.fixer.*;
import com.intellij.plugins.haxe.model.util.HaxeNameUtils;
import com.intellij.plugins.haxe.util.HaxeJavaUtil;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.plugins.haxe.util.HaxeStringUtil;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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
      return SpecificHaxeClassReference.getUnknown(context.root).createHolder();
    }
  }

  static private String SWITCH_TARGET = " $SWITCH_TARGET$ ";
  static private Key<Set<HaxeMemberModel>> HAXE_SWITCH_MEMBER = new Key<Set<HaxeMemberModel>>("HAXE_SWITCH_MEMBER");
  static private Key<HaxeSwitchStatement> SWITCH_ELEMENT = new Key<HaxeSwitchStatement>("SWITCH_ELEMENT");
  static private Key<PsiElement> LOOP_ELEMENT = new Key<PsiElement>("LOOP_ELEMENT");

  static private ResultHolder setParameter(final HaxeExpressionEvaluatorContext context, final HaxeParameter param) {
    final HaxeComponentName name = param.getComponentName();
    final HaxeTypeTag tag = param.getTypeTag();
    final ResultHolder result = HaxeTypeResolver.getTypeFromTypeTag(tag, context.root);
    context.setLocal(name.getText(), result);
    return result;
  }

  @NotNull
  static private ResultHolder _handle(final PsiElement element, final HaxeExpressionEvaluatorContext context) {
    if (element == null) {
      return SpecificHaxeClassReference.getUnknown(context.root).createHolder();
    }
    //System.out.println("Handling element: " + element.getClass());
    // @TODO: Create a visitor from this
    if (element instanceof PsiCodeBlock) {
      return handlePsiCodeBlock((PsiCodeBlock)element, context);
    }
    else if (element instanceof HaxeThrowStatement) {
      return handleThrowStatement((HaxeThrowStatement)element, context);
    }
    else if (element instanceof HaxeBreakStatement) {
      return handleBreakStatement((HaxeBreakStatement)element, context);
    }
    else if (element instanceof HaxeContinueStatement) {
      return handleContinueStatement((HaxeContinueStatement)element, context);
    }
    else if (element instanceof HaxeReturnStatement) {
      return handleReturnStatement((HaxeReturnStatement)element, context);
    }
    else if (element instanceof HaxeIterable) {
      return handleIterable((HaxeIterable)element, context);
    }
    else if (element instanceof HaxeSwitchStatement) {
      return handleSwitchStatement((HaxeSwitchStatement)element, context);
    }
    else if (element instanceof HaxeSwitchBlock) {
      return handleSwitchBlock((HaxeSwitchBlock)element, context);
    }
    else if (element instanceof HaxeSwitchCase) {
      return handleSwitchCase((HaxeSwitchCase)element, context);
    }
    else if (element instanceof HaxeTryStatement) {
      return handleTryStatement((HaxeTryStatement)element, context);
    }
    else if (element instanceof HaxeCatchStatement) {
      return handleCatchStatement((HaxeCatchStatement)element, context);
    }
    else if (element instanceof HaxeSwitchCaseBlock) {
      return handleSwitchCaseBlock(element, context);
    }
    else if (element instanceof HaxeDefaultCase) {
      return handleDefaultCase((HaxeDefaultCase)element, context);
    }
    else if (element instanceof HaxeForStatement) {
      return handleForStatement((HaxeForStatement)element, context);
    }
    else if (element instanceof HaxeNewExpression) {
      return handleNewExpression((HaxeNewExpression)element, context);
    }
    else if (element instanceof HaxeThisExpression) {
      return handleThisExpression((HaxeThisExpression)element, context);
    }
    else if (element instanceof HaxeIdentifier) {
      return handleIdentifier((HaxeIdentifier)element, context);
    }
    else if (element instanceof HaxeCastExpression) {
      return handleCastExpression((HaxeCastExpression)element, context);
    }
    else if (element instanceof HaxeWhileStatement) {
      return handleWhileStatement((HaxeWhileStatement)element, context);
    }
    else if (element instanceof HaxeDoWhileStatement) {
      return handleDoWhileStatement((HaxeDoWhileStatement)element, context);
    }
    else if (element instanceof HaxeLocalVarDeclaration) {
      return handleLocalVarDeclaration((HaxeLocalVarDeclaration)element, context);
    }
    else if (element instanceof HaxeAssignExpression) {
      return handleAssignExpression((HaxeAssignExpression)element, context);
    }
    else if (element instanceof HaxeLocalVarDeclarationPart) {
      return handleLocalVarDeclarationPart((HaxeLocalVarDeclarationPart)element, context);
    }
    else if (element instanceof HaxeVarInit) {
      return handleVarInit((HaxeVarInit)element, context);
    }
    else if (element instanceof HaxeReferenceExpression) {
      return handleReferenceExpression((HaxeReferenceExpression)element, context);
    }
    else if (element instanceof HaxeCallExpression) {
      return handleCallExpression((HaxeCallExpression)element, context);
    }
    else if (element instanceof HaxeLiteralExpression) {
      return handleLiteralExpression((HaxeLiteralExpression)element, context);
    }
    else if (element instanceof HaxeStringLiteralExpression) {
      return handleStringLiteralExpression((HaxeStringLiteralExpression)element, context);
    }
    else if (element instanceof HaxeExpressionList) {
      return handleExpressionList((HaxeExpressionList)element, context);
    }
    else if (element instanceof HaxeArrayLiteral) {
      return handleArrayLiteral((HaxeArrayLiteral)element, context);
    }
    else if (element instanceof PsiJavaToken) {
      return handleJavaToken((PsiJavaToken)element, context);
    }
    else if (element instanceof HaxeSuperExpression) {
      return handleSuperExpression((HaxeSuperExpression)element, context);
    }
    else if (element instanceof HaxeIteratorExpression) {
      return handleIteratorExpression((HaxeIteratorExpression)element, context);
    }
    else if (element instanceof HaxeArrayAccessExpression) {
      return handleArrayAccessExpression((HaxeArrayAccessExpression)element, context);
    }
    else if (element instanceof HaxeLocalFunctionDeclaration) {
      return handleLocalFunctionDelcaration((HaxeLocalFunctionDeclaration)element, context);
    }
    else if (element instanceof HaxeFunctionLiteral) {
      return handleFunctionLiteral((HaxeFunctionLiteral)element, context);
    }
    else if (element instanceof HaxeIfStatement) {
      return handleIfStatement((HaxeIfStatement)element, context);
    }
    else if (element instanceof HaxeParenthesizedExpression) {
      return handleParenthesizedExpression((HaxeParenthesizedExpression)element, context);
    }
    else if (element instanceof HaxeTernaryExpression) {
      return handleTernaryExpression(element, context);
    }
    else if (element instanceof HaxePrefixExpression) {
      return handlePrefixExpression(element, context);
    }
    else if (
      (element instanceof HaxeAdditiveExpression) ||
      (element instanceof HaxeBitwiseExpression) ||
      (element instanceof HaxeShiftExpression) ||
      (element instanceof HaxeLogicAndExpression) ||
      (element instanceof HaxeLogicOrExpression) ||
      (element instanceof HaxeCompareExpression) ||
      (element instanceof HaxeMultiplicativeExpression)
      ) {
      return handleBinaryExpression((HaxeExpression)element, context);
    }
    else if (element instanceof HaxeSuffixExpression) {
      return handleSuffixExpression(element, context);
    }
    else {
      return handleUnhandled(element, context);
    }
  }

  private static ResultHolder handleUnhandled(PsiElement element, HaxeExpressionEvaluatorContext context) {
    System.out.println("Unhandled " + element.getClass());
    return context.types.DYNAMIC.createHolder();
  }

  @NotNull
  private static ResultHolder handleSuffixExpression(PsiElement element, HaxeExpressionEvaluatorContext context) {
    final PsiElement[] children = element.getChildren();
    return handle(children[0], context);
  }

  private static ResultHolder handleBinaryExpression(HaxeExpression element, HaxeExpressionEvaluatorContext context) {
    PsiElement[] children = element.getChildren();
    String operatorText;

    checkExpressionSemicolon(element, context);

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

  private static ResultHolder handlePrefixExpression(PsiElement element, HaxeExpressionEvaluatorContext context) {
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

  private static ResultHolder handleTernaryExpression(PsiElement element, HaxeExpressionEvaluatorContext context) {
    final List<HaxeExpression> expressionList = ((HaxeTernaryExpression)element).getExpressionList();
    HaxeExpression[] list = expressionList.toArray(new HaxeExpression[expressionList.size()]);
    return HaxeTypeUnifier.unify(handle(list[1], context).getType(), handle(list[2], context).getType(), element).createHolder();
  }

  private static ResultHolder handleParenthesizedExpression(HaxeParenthesizedExpression element, HaxeExpressionEvaluatorContext context) {
    return handle(element.getChildren()[0], context);
  }

  private static ResultHolder handleIfStatement(HaxeIfStatement element, HaxeExpressionEvaluatorContext context) {
    PsiElement[] children = element.getChildren();
    if (children.length >= 1) {
      SpecificTypeReference expr = handle(children[0], context).getType();
      if (!context.types.BOOL.canAssign(expr)) {
        context.addError(
          children[0],
          "If expr " + expr + " should be bool",
          new HaxeCastFixer(children[0], expr, context.types.BOOL)
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
    else {
      return context.types.VOID.createHolder();
    }
  }

  private static ResultHolder handleFunctionLiteral(HaxeFunctionLiteral element, HaxeExpressionEvaluatorContext context) {
    HaxeParameterList params = element.getParameterList();
    if (params == null) {
      return SpecificHaxeClassReference.getInvalid(element).createHolder();
    }
    ResultHolder holder = context.types.DYNAMIC.createHolder();
    LinkedList<ResultHolder> results = new LinkedList<ResultHolder>();
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
      HaxeExpressionEvaluatorContext childContext = context.createChild(element.getLastChild());
      ResultHolder returnType = HaxeTypeResolver.getTypeFromTypeTag(element.getTypeTag(), element);
      holder.setType(new SpecificFunctionReference(results, returnType, null, element));
      childContext.functionType = holder;
      context.addLambda(childContext);
    }
    finally {
      context.endScope();
    }

    return holder;
  }

  private static ResultHolder handleLocalFunctionDelcaration(HaxeLocalFunctionDeclaration element, HaxeExpressionEvaluatorContext context) {
    // This is a method too!

    HaxeParameterList params = element.getParameterList();
    HaxeComponentName functionName = element.getComponentName();
    if (params == null || functionName == null) {
      return SpecificHaxeClassReference.getInvalid(element).createHolder();
    }

    ResultHolder holder = context.types.DYNAMIC.createHolder();
    LinkedList<ResultHolder> results = new LinkedList<ResultHolder>();
    context.beginScope();
    try {
      for (HaxeParameter parameter : params.getParameterList()) {
        results.add(setParameter(context, parameter));
      }
      HaxeExpressionEvaluatorContext childContext = context.createChild(element.getLastChild());
      ResultHolder returnType = HaxeTypeResolver.getTypeFromTypeTag(((HaxeLocalFunctionDeclaration)element).getTypeTag(), element);
      holder.setType(new SpecificFunctionReference(results, returnType, null, element));
      childContext.functionType = holder;
      context.addLambda(childContext);
    }
    finally {
      context.endScope();
    }

    context.setLocal(functionName.getText(), holder);

    return holder;
  }

  private static ResultHolder handlePsiCodeBlock(PsiCodeBlock element, HaxeExpressionEvaluatorContext context) {
    context.beginScope();
    ResultHolder type = SpecificHaxeClassReference.getUnknown(context.root).createHolder();
    boolean deadCode = false;
    for (PsiElement childElement : element.getChildren()) {
      type = handle(childElement, context);
      if (deadCode) {
        //context.addWarning(childElement, "Unreachable statement");
        context.addUnreachable(childElement);
      }
      if (
        childElement instanceof HaxeReturnStatement ||
        childElement instanceof HaxeThrowStatement ||
        childElement instanceof HaxeBreakStatement ||
        childElement instanceof HaxeContinueStatement
        ) {
        deadCode = true;
      }
    }
    context.endScope();
    return type;
  }

  private static ResultHolder handleThrowStatement(HaxeThrowStatement element, HaxeExpressionEvaluatorContext context) {
    final HaxeExpression throwExpression = element.getExpression();
    final ResultHolder throwResult = handle(throwExpression, context);
    return context.types.VOID.createHolder();
  }

  private static ResultHolder handleBreakStatement(HaxeBreakStatement element, HaxeExpressionEvaluatorContext context) {
    return handleBreakOrContinueStatement(element, context);
  }

  private static ResultHolder handleContinueStatement(HaxeContinueStatement element, HaxeExpressionEvaluatorContext context) {
    return handleBreakOrContinueStatement(element, context);
  }

  private static ResultHolder handleBreakOrContinueStatement(PsiElement element, HaxeExpressionEvaluatorContext context) {
    final PsiElement loop = context.getInfo(LOOP_ELEMENT);
    if (loop == null) {
      context.addError(element, element.getText() + " is not inside a loop");
    }
    return context.types.VOID.createHolder();
  }

  private static ResultHolder handleReturnStatement(HaxeReturnStatement element, HaxeExpressionEvaluatorContext context) {
    PsiElement[] children = element.getChildren();
    ResultHolder result = context.types.VOID.createHolder();
    if (children.length >= 1) {
      result = handle(children[0], context);
    }

    if (!UsefulPsiTreeUtil.isToken(element.getLastChild(), ";")) {
      context.addErrorAfter(element, "Missing semicolon", new AddSemicolonFixer(element));
    }

    context.addReturnType(result, element);
    return result;
  }

  @NotNull
  private static ResultHolder handleIterable(HaxeIterable element, HaxeExpressionEvaluatorContext context) {
    return handle(element.getExpression(), context);
  }

  @NotNull
  private static ResultHolder handleSwitchStatement(HaxeSwitchStatement element, HaxeExpressionEvaluatorContext context) {
    final HaxeExpression targetExpression = element.getExpression();
    final ResultHolder target = handle(targetExpression, context);
    final SpecificHaxeClassReference targetType = target.getClassType();
    if (targetType == null) {
      context.addError(targetExpression, "Can't match functional types");
    }
    context.beginScope();
    try {
      context.putInfo(SWITCH_ELEMENT, element);
      context.setLocal(SWITCH_TARGET, target);
      return handle(element.getSwitchBlock(), context);
    }
    finally {
      context.endScope();
    }
  }

  private static ResultHolder handleSwitchBlock(HaxeSwitchBlock element, HaxeExpressionEvaluatorContext context) {
    ResultHolder result = null;
    final ResultHolder targetResult = context.get(SWITCH_TARGET);
    final HaxeDefaultCase defaultCase = element.getDefaultCase();
    if (defaultCase != null) {
      context.beginScope();
      try {
        final ResultHolder defaultResult = handle(defaultCase, context);
        result = HaxeTypeUnifier.unify(result, defaultResult, element);
      }
      finally {
        context.endScope();
      }
    }
    final SpecificHaxeClassReference targetValue = targetResult.getClassType();
    final HaxeClassModel targetClass = (targetValue != null) ? targetValue.getHaxeClassModel() : null;
    Set<HaxeMemberModel> missingMembers = null;
    if ((targetClass != null) && targetClass.isEnum()) {
      missingMembers = new HashSet<HaxeMemberModel>(targetClass.getMembersSelf());
    }
    for (HaxeSwitchCase aCase : element.getSwitchCaseList()) {
      context.beginScope();
      context.putInfo(HAXE_SWITCH_MEMBER, missingMembers);
      final ResultHolder caseResult = handle(aCase, context);
      result = HaxeTypeUnifier.unify(result, caseResult, element);
      context.endScope();
    }
    if (missingMembers != null) {
      if (!missingMembers.isEmpty() && defaultCase == null) {
        context.addError(context.getInfo(SWITCH_ELEMENT).getExpression(), "Not exhaustive members. Missing: " + missingMembers);
      }
    }
    else {
      if (defaultCase == null) {
        context.addError(context.getInfo(SWITCH_ELEMENT).getExpression(), "Not exhaustive members. Put a default.");
      }
    }
    if (result == null) return context.types.DYNAMIC.createHolder();
    return result;
  }

  @NotNull
  private static ResultHolder handleSwitchCase(HaxeSwitchCase element, HaxeExpressionEvaluatorContext context) {
    final ResultHolder targetResult = context.get(SWITCH_TARGET);
    final List<HaxeExpression> list = element.getExpressionList();
    final Set<HaxeMemberModel> missingMembers = context.getInfo(HAXE_SWITCH_MEMBER);
    for (HaxeExpression expression : list) {
      // Match and create scope!
      final ResultHolder exprType = handle(expression, context);
      if (!targetResult.canAssign(exprType)) {
        context.addError(expression, "Incompatible type. Found " + exprType + " but expected " + targetResult);
      }
      if (missingMembers != null) {
        final Object constant = exprType.getConstant();
        if (constant instanceof HaxeEnumMemberModel) {
          missingMembers.remove(constant);
        }
      }
    }
    return handle(element.getSwitchCaseBlock(), context);
  }

  @NotNull
  private static ResultHolder handleTryStatement(HaxeTryStatement element, HaxeExpressionEvaluatorContext context) {
    PsiElement code = element.getChildren()[0];
    final ResultHolder tryResult = handle(code, context);
    ResultHolder result = tryResult;

    for (HaxeCatchStatement statement : element.getCatchStatementList()) {
      final ResultHolder catchResult = handle(statement, context);
      result = HaxeTypeUnifier.unify(result, catchResult, context.root);
    }
    return result;
  }

  @NotNull
  private static ResultHolder handleCatchStatement(HaxeCatchStatement element, HaxeExpressionEvaluatorContext context) {
    context.beginScope();
    try {
      final HaxeParameter parameter = element.getParameter();
      if (parameter != null) {
        setParameter(context, parameter);
        if (parameter.getTypeTag() == null) {
          context.addError(parameter, "Required TypeTag");
        }
      }
      return handle(element.getLastChild(), context);
    }
    finally {
      context.endScope();
    }
  }

  private static ResultHolder handleSwitchCaseBlock(PsiElement element, HaxeExpressionEvaluatorContext context) {
    ResultHolder holder = context.types.DYNAMIC.createHolder();
    for (PsiElement psiElement : element.getChildren()) {
      holder = handle(psiElement, context);
    }
    return holder;
  }

  @NotNull
  private static ResultHolder handleDefaultCase(HaxeDefaultCase element, HaxeExpressionEvaluatorContext context) {
    return handle(element.getSwitchCaseBlock(), context);
  }

  @NotNull
  private static ResultHolder handleForStatement(HaxeForStatement element, HaxeExpressionEvaluatorContext context) {
    final HaxeComponentName name = element.getComponentName();
    final HaxeIterable iterable = element.getIterable();
    final PsiElement body = element.getLastChild();
    context.beginScope();
    context.putInfo(LOOP_ELEMENT, element);
    try {
      final SpecificTypeReference iterableValue = handle(iterable, context).getType();
      if (!iterableValue.isIterable(context)) {
        context.addError(iterable, "Can't iterate " + iterableValue);
      }
      SpecificTypeReference type = iterableValue.getIterableElementType(context).getType();
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

  private static ResultHolder handleNewExpression(HaxeNewExpression element, HaxeExpressionEvaluatorContext context) {
    ResultHolder typeHolder = HaxeTypeResolver.getTypeFromType(element.getType());
    if (typeHolder.getType() instanceof SpecificHaxeClassReference) {
      final HaxeClassModel clazz = ((SpecificHaxeClassReference)typeHolder.getType()).getHaxeClassModel();
      if (clazz == null) {
        context.addError(element.getType(), "Can't find type");
      }
      else {
        HaxeMethodModel constructor = clazz.getConstructor();
        if (constructor == null) {
          context.addError(
            element,
            "Class " + clazz.getName() + " doesn't have a constructor",
            new HaxeCreateMethodsFixer(clazz.getAliasOrSelf(), new HaxeMethodBuilder("new", null))
          );
        }
        else {
          checkParameters(element, constructor, element.getExpressionList(), context);
        }
      }
    }
    return typeHolder.duplicate();
  }

  private static ResultHolder handleThisExpression(HaxeThisExpression element, HaxeExpressionEvaluatorContext context) {
    //PsiReference reference = element.getReference();
    //HaxeClassResolveResult result = HaxeResolveUtil.getHaxeClassResolveResult(element);
    if (context.isInStaticContext()) {
      context.addError(element, "Using this in a static context");
    }
    HaxeClass ancestor = UsefulPsiTreeUtil.getAncestor(element, HaxeClass.class);
    if (ancestor == null) return context.types.DYNAMIC.createHolder();
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

  private static ResultHolder handleIdentifier(HaxeIdentifier element, HaxeExpressionEvaluatorContext context) {
    //PsiReference reference = element.getReference();
    ResultHolder holder = context.get(element.getText());

    if (holder == null) {
      context.addError(element, "Unknown symbol", new HaxeCreateLocalVariableFixer(element.getText(), element));

      return context.types.DYNAMIC.createHolder();
    }

    return holder;
    //System.out.println("HaxeIdentifier:" + reference);
  }

  private static ResultHolder handleCastExpression(HaxeCastExpression element, HaxeExpressionEvaluatorContext context) {
    handle(element.getExpression(), context);
    HaxeTypeOrAnonymous anonymous = element.getTypeOrAnonymous();
    if (anonymous != null) {
      return HaxeTypeResolver.getTypeFromTypeOrAnonymous(anonymous);
    }
    else {
      return SpecificHaxeClassReference.getUnknown(element).createHolder();
    }
  }

  private static ResultHolder handleWhileOrDoWhileStatement(PsiElement element, HaxeExpressionEvaluatorContext context, boolean isDoWhile) {
    PsiElement[] children = element.getChildren();

    context.beginScope();
    context.putInfo(LOOP_ELEMENT, element);
    for (int i = 0; i < children.length; i++) {
      PsiElement child = children[i];
      SpecificTypeReference result = handle(child, context).getType();
      boolean isFirst = i == 0;
      boolean isLast = i == (children.length - 1);
      if ((isDoWhile && isLast) || (!isDoWhile && isFirst)) {
        if (!result.isBool()) {
          context.addError(
            child,
            "While expression must be Bool but was " + result,
            new HaxeCastFixer(child, result, context.types.BOOL)
          );
        }
      }
    }
    context.endScope();

    return SpecificHaxeClassReference.getUnknown(element).createHolder();
  }

  private static ResultHolder handleWhileStatement(HaxeWhileStatement element, HaxeExpressionEvaluatorContext context) {
    return handleWhileOrDoWhileStatement(element, context, false);
  }

  private static ResultHolder handleDoWhileStatement(HaxeDoWhileStatement element, HaxeExpressionEvaluatorContext context) {
    return handleWhileOrDoWhileStatement(element, context, true);
  }

  private static ResultHolder handleArrayAccessExpression(HaxeArrayAccessExpression element, HaxeExpressionEvaluatorContext context) {
    final List<HaxeExpression> list = element.getExpressionList();
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

  private static ResultHolder handleIteratorExpression(HaxeIteratorExpression element, HaxeExpressionEvaluatorContext context) {
    final List<HaxeExpression> list = element.getExpressionList();
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

      return SpecificHaxeClassReference.getIterator(context.types.INT).withConstantValue(constant)
        .createHolder();
    }
    return SpecificHaxeClassReference.getUnknown(element).createHolder();
  }

  private static ResultHolder handleSuperExpression(HaxeSuperExpression element, HaxeExpressionEvaluatorContext context) {
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

  private static ResultHolder handleJavaToken(PsiJavaToken element, HaxeExpressionEvaluatorContext context) {
    IElementType type = element.getTokenType();

    if (type == HaxeTokenTypes.LITINT || type == HaxeTokenTypes.LITHEX || type == HaxeTokenTypes.LITOCT) {
      return SpecificHaxeClassReference.primitive("Int", element, Long.decode(element.getText())).createHolder();
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
    else if (type == HaxeTokenTypes.REG_EXP) {
      return SpecificHaxeClassReference.primitive("EReg", element).createHolder();
    }
    else if (type == HaxeTokenTypes.OSEMI) {
      return context.types.VOID.createHolder();
    } else {
      System.out.println("Unhandled literal type: " + type);
      return context.types.DYNAMIC.createHolder();
    }
  }

  private static ResultHolder handleArrayLiteral(HaxeArrayLiteral element, HaxeExpressionEvaluatorContext context) {

    checkExpressionSemicolon(element, context);

    HaxeExpressionList list = element.getExpressionList();
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

  @NotNull
  private static ResultHolder handleExpressionList(HaxeExpressionList element, HaxeExpressionEvaluatorContext context) {
    ArrayList<ResultHolder> references = new ArrayList<ResultHolder>();
    for (HaxeExpression expression : element.getExpressionList()) {
      references.add(handle(expression, context));
    }
    return HaxeTypeUnifier.unifyHolders(references, element);
  }

  private static ResultHolder handleStringLiteralExpression(HaxeStringLiteralExpression element, HaxeExpressionEvaluatorContext context) {
    // @TODO: check if it has string interpolation inside, in that case text is not constant

    checkExpressionSemicolon(element, context);

    return SpecificHaxeClassReference.primitive(
      "String",
      element,
      HaxeStringUtil.unescapeString(element.getText())
    ).createHolder();
  }

  private static ResultHolder handleLocalVarDeclaration(HaxeLocalVarDeclaration element, HaxeExpressionEvaluatorContext context) {
    for (HaxeLocalVarDeclarationPart part : element.getLocalVarDeclarationPartList()) {
      handle(part, context);
    }
    if (!UsefulPsiTreeUtil.isToken(element.getLastChild(), ";")) {
      context.addErrorAfter(element, "Missing semicolon", new AddSemicolonFixer(element));
    }
    return SpecificHaxeClassReference.getUnknown(element).createHolder();
  }

  // @TODO: Maybe this could be "epxression is statement" that would allow for example marking an arithmetic expression as dummy.
  // @TODO: Also, all this work should be done in the BnF file. Also a better BnF would make this much easier, clean and less prone to errors.
  private static boolean expressionRequireSemicolon(PsiElement element) {
    PsiElement parent = element.getParent();
    if (parent == null) return false;

    PsiElement[] children = parent.getChildren();

    // First position in while/if is a expression not a statement!
    if (parent instanceof HaxeWhileStatement || parent instanceof HaxeIfStatement) {
      if (children[0] == element) return false;
    }

    if (parent instanceof HaxeDoWhileStatement) {
      if (children[children.length - 1] == element) return false;
    }

    if (parent instanceof PsiStatement) {
      return true;
    }

    if (parent instanceof PsiMember) {
      return true;
    }

    return false;
  }

  private static void checkExpressionSemicolon(@Nullable PsiElement element, HaxeExpressionEvaluatorContext context) {
    if (element == null) return;

    if (expressionRequireSemicolon(element)) {
      boolean hasSemicolon = false;
      if (UsefulPsiTreeUtil.isToken(element.getLastChild(), ";")) hasSemicolon = true;
      if (UsefulPsiTreeUtil.isToken(UsefulPsiTreeUtil.getNextSiblingNoSpaces(element), ";")) hasSemicolon = true;
      if (!hasSemicolon) {
        context.addErrorAfter(element, "Missing semicolon", new AddSemicolonFixer(element));
      }
    }
  }

  private static ResultHolder handleAssignExpression(HaxeAssignExpression element, HaxeExpressionEvaluatorContext context) {
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
      
      if (!leftResult.canAssign(rightResult)) {
        context.addError(element, "Can't assign " + rightValue + " to " + leftValue, new HaxeCastFixer(right, rightValue, leftValue));
      }

      if (leftResult.isImmutable()) {
        context.addError(element, "Trying to change an immutable value");
      }

      checkExpressionSemicolon(element, context);

      return rightResult;
    }
    return SpecificHaxeClassReference.getUnknown(element).createHolder();
  }

  private static ResultHolder handleLocalVarDeclarationPart(HaxeLocalVarDeclarationPart element, HaxeExpressionEvaluatorContext context) {
    final HaxeComponentName name = element.getComponentName();
    final HaxeVarInit init = element.getVarInit();
    final HaxeTypeTag typeTag = element.getTypeTag();
    ResultHolder result = SpecificHaxeClassReference.getUnknown(element).createHolder();

    if (init != null) {
      result = handle(init, context);
    }

    if (typeTag != null) {
      final ResultHolder left = result;
      final ResultHolder tag = HaxeTypeResolver.getTypeFromTypeTag(typeTag, element);

      if (!tag.canAssign(left)) {
        result = tag.duplicate();

        context.addError(
          element,
          "Can't assign " + result + " to " + tag,
          new HaxeTypeTagChangeFixer(typeTag, left.getType()),
          new HaxeTypeTagRemoveFixer(typeTag)
        );
      }
    }

    context.setLocal(name.getText(), result);

    return result;
  }

  @NotNull
  private static ResultHolder handleLiteralExpression(HaxeLiteralExpression element, HaxeExpressionEvaluatorContext context) {
    checkExpressionSemicolon(element, context);
    return handle(element.getFirstChild(), context);
  }

  private static ResultHolder handleCallExpression(HaxeCallExpression call, HaxeExpressionEvaluatorContext context) {
    HaxeExpression callLeft = call.getExpression();
    SpecificTypeReference functionType = handle(callLeft, context).getType();

    checkExpressionSemicolon(call, context);

    if (functionType.isUnknown()) {
      //System.out.println("Couldn't resolve " + callLeft.getText());
    }

    List<HaxeExpression> parameterExpressions = null;
    if (call.getExpressionList() != null) {
      parameterExpressions = call.getExpressionList().getExpressionList();
    }
    else {
      parameterExpressions = Collections.emptyList();
    }

    if (functionType instanceof SpecificFunctionReference) {
      SpecificFunctionReference ftype = (SpecificFunctionReference)functionType;
      HaxeExpressionEvaluator.checkParameters(call, ftype, parameterExpressions, context);

      return ftype.getReturnType().duplicate();
    }

    if (functionType.isDynamic()) {
      for (HaxeExpression expression : parameterExpressions) {
        handle(expression, context);
      }

      return functionType.withoutConstantValue().createHolder();
    }

    // @TODO: resolve the function type return type
    return SpecificHaxeClassReference.getUnknown(call).createHolder();
  }

  @NotNull
  private static List<HaxeIdentifier> getAccessIdentifiers(@NotNull HaxeReferenceExpression element) {
    final LinkedList<HaxeIdentifier> identifiers = new LinkedList<HaxeIdentifier>();
    if (!getAccessIdentifiers(element, identifiers)) {
      return new LinkedList<HaxeIdentifier>();
    }
    return identifiers;
  }

  private static boolean getAccessIdentifiers(@NotNull HaxeReferenceExpression element, @NotNull List<HaxeIdentifier> out) {
    for (PsiElement child : element.getChildren()) {
      if (child instanceof HaxeReferenceExpression) {
        if (!getAccessIdentifiers((HaxeReferenceExpression)child, out)) {
          return false;
        }
      }
      else if (child instanceof HaxeIdentifier) {
        out.add((HaxeIdentifier)child);
      }
      else {
        return false;
      }
    }
    return true;
  }

  private static List<String> convertIdentifierList(List<HaxeIdentifier> idents) {
    ArrayList<String> out = new ArrayList<String>();
    for (HaxeIdentifier ident : idents) {
      out.add(ident.getText());
    }

    return out;
  }

  @Nullable
  private static ResultHolder tryToGetFullyQualifiedName(
    @NotNull HaxeReferenceExpression element,
    @NotNull HaxeExpressionEvaluatorContext context
  ) {
    final List<HaxeIdentifier> identifiers = getAccessIdentifiers(element);

    if (identifiers.size() > 0) {
      String path = Joiner.on('.').join(convertIdentifierList(identifiers));
      FqInfo fqInfo = FqInfo.parse(path);
      if (fqInfo == null) return null;

      HaxeProjectModel project = context.types.project;
      HaxePackageModel rootPackage = project.rootPackage;
      HaxePackageModel currentPackage = HaxeFileModel.fromElement(element).getDetectedPackage();

      HaxeClassModel type = null;
      if (currentPackage != null) {
        type = currentPackage.accessClass(fqInfo);
      }
      if (type == null) {
        type = rootPackage.accessClass(fqInfo);
      }

      return type != null ? type.getClassType() : null;
    }

    return null;
  }

  private static ResultHolder handleReferenceExpression(HaxeReferenceExpression element, HaxeExpressionEvaluatorContext context) {
    final HaxeProjectModel project = HaxeProjectModel.fromElement(element);

    checkExpressionSemicolon(element, context);

    // @TODO: We should be able to check right if we priorize locals first
    final ResultHolder fqClass = tryToGetFullyQualifiedName(element, context);
    if (fqClass != null) {
      return fqClass;
    }

    PsiElement[] children = element.getChildren();
    ResultHolder typeHolder = handle(children[0], context);

    for (int n = 1; n < children.length; n++) {
      PsiElement accessElement = children[n];
      String accessName = accessElement.getText();
      if (typeHolder.getType().isString() && typeHolder.getType().isConstant() && accessName.equals("code")) {
        String str = (String)typeHolder.getType().getConstant();
        typeHolder = context.types.INT.withConstantValue((str != null && str.length() >= 1) ? str.charAt(0) : -1).createHolder();
        if (str == null || str.length() != 1) {
          context.addError(element, "String must be a single UTF8 char");
        }
      }
      else {
        ResultHolder access = typeHolder.getType().access(accessName, accessElement, context);
        if (access == null) {
          Annotation annotation = context.addUnresolvedError(accessElement, "Can't resolve '" + accessName + "' in " + typeHolder.getType());
          if (children.length == 1) {
            annotation.registerFix(new HaxeCreateLocalVariableFixer(accessName, element));
          }
          else {
            SpecificHaxeClassReference classType = typeHolder.getClassType();
            if (classType != null) {
              HaxeClassModel classModel = classType.getHaxeClassModel();
              final HaxeCallExpression callExpr = UsefulPsiTreeUtil.getAncestorWithAcceptableParents(
                element,
                HaxeCallExpression.class,
                HaxeReferenceExpression.class
              );
              if (callExpr != null) {
                final HaxeAssignExpression assign = UsefulPsiTreeUtil.getAncestorWithAcceptableParents(
                  callExpr, HaxeAssignExpression.class,
                  HaxeCallExpression.class, HaxeReferenceExpression.class
                );
                final HaxeLocalVarDeclarationPart varInit = UsefulPsiTreeUtil.getAncestorWithAcceptableParents(
                  callExpr, HaxeLocalVarDeclarationPart.class,
                  HaxeVarInit.class, HaxeCallExpression.class, HaxeReferenceExpression.class
                );
                ResultHolder returnType = null;
                if (assign != null) {
                  final PsiElement[] children1 = assign.getChildren();
                  if (children1.length >= 1) {
                    returnType = handle(children1[0], context).withoutConstantValue();
                  }
                }
                if (varInit != null) {
                  final HaxeTypeTag tag = varInit.getTypeTag();
                  if (tag != null) {
                    returnType = HaxeTypeResolver.getTypeFromTypeTag(tag, element);
                  }
                }
                final HaxeExpressionList list = callExpr.getExpressionList();
                final HaxeMethodBuilder methodBuilder = new HaxeMethodBuilder(accessName, returnType);
                if (list != null) {
                  for (HaxeExpression expression : list.getExpressionList()) {
                    final ResultHolder handle = handle(expression, context);
                    methodBuilder.addArgument(
                      HaxeNameUtils.getValidIdentifierFromExpression(expression, handle),
                      handle.withoutConstantValue()
                    );
                  }
                }
                annotation.registerFix(new HaxeCreateMethodsFixer(classModel, methodBuilder));
              }
              annotation.registerFix(new HaxeCreateFieldFixer(classModel, accessName));
            }
          }
        }
        typeHolder = access;
      }
    }

    return (typeHolder != null) ? typeHolder : context.types.DYNAMIC.createHolder();
  }

  @NotNull
  private static ResultHolder handleVarInit(HaxeVarInit element, HaxeExpressionEvaluatorContext context) {
    return handle(element.getExpression(), context);
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
      context.addError(
        callelement,
        "Less arguments than expected " + parameterExpressions.size() + " != " + ftype.getNonOptionalArgumentsCount()
      );
    }
  }

  static private String getOperator(PsiElement element, TokenSet set) {
    ASTNode operatorNode = element.getNode().findChildByType(set);
    if (operatorNode == null) return "";
    return operatorNode.getText();
  }
}
