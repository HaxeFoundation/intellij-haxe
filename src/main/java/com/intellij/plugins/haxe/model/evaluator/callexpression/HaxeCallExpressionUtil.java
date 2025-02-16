package com.intellij.plugins.haxe.model.evaluator.callexpression;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluatorContext;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static java.util.function.Predicate.not;

public class HaxeCallExpressionUtil {


  public static boolean isValidMethodCall(@NotNull List<SpecificTypeReference> arguments,
                                        @NotNull HaxeMethodModel methodModel,
                                        HaxeGenericResolver genericResolver) {
    return createContextForMethodCall(arguments, methodModel, genericResolver).evaluate().isValid();
  }

  public static boolean isValidMethodCall(@NotNull HaxeCallExpression callExpression, @NotNull HaxeMethod method) {
    return createContextForMethodCall(callExpression, method).evaluate().isValid();
  }


  public static boolean isValidFunctionCall(@NotNull HaxeCallExpression callExpression,
                                          @NotNull SpecificFunctionReference functionReference) {
    return createContextForFunctionCall(callExpression, functionReference).evaluate().isValid();
  }

  public static HaxeCallExpressionContext createContextForMethodCall(@NotNull List<SpecificTypeReference> arguments,
                                                                     @NotNull HaxeMethodModel methodModel,
                                                                     @Nullable HaxeGenericResolver resolver
  ) {
    return createContextForMethodCall(arguments,methodModel,resolver, null);
  }

  @NotNull
  public static HaxeCallExpressionContext createContextForMethodCall(@NotNull List<SpecificTypeReference> arguments,
                                                                              @NotNull HaxeMethodModel methodModel,
                                                                              @Nullable HaxeGenericResolver resolver,
                                                                              @Nullable SpecificHaxeClassReference callie
  ) {

    List<CallExpressionArgumentModel> argumentList = getArgumentList(arguments);
    List<CallExpressionParameterModel> parameterList = getParameterList(methodModel);
    ResultHolder returnType = methodModel.getReturnType(null);
    HaxeGenericResolver methodGenericResolver = methodModel.getGenericResolver(resolver);

    HaxeClassModel declaringClass = methodModel.getDeclaringClass();
    if (declaringClass != null) {
      HaxeGenericResolver MethodsClassResolver = declaringClass.getGenericResolver(resolver);
      methodGenericResolver.addAll(MethodsClassResolver);
    }


    HaxeCallExpressionContext evaluation = new HaxeCallExpressionContext(argumentList, parameterList, returnType, resolver, methodGenericResolver);

    evaluation.isMacroFunction = methodModel.isMacro() && !methodModel.isStatic();
    evaluation.callie = callie;
    return evaluation;
  }

  @NotNull
  public static HaxeCallExpressionContext createContextForMethodCall(@NotNull HaxeCallExpression callExpression,
                                                                              @NotNull HaxeMethod method) {
    return createContextForMethodCall(callExpression, null, method);
  }
  @NotNull
  public static HaxeCallExpressionContext createContextForMethodCall(@NotNull HaxeCallExpression callExpression,
                                                                     @Nullable SpecificTypeReference assignHint,
                                                                    @NotNull HaxeMethod method
  ) {
    HaxeMethodModel methodModel = method.getModel();

    HaxeGenericResolver genericResolver = new HaxeGenericResolver();

    HaxeGenericResolver parentResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(callExpression);
    HaxeGenericResolver methodResolver = methodModel.getGenericResolver(parentResolver);

    genericResolver.addAll(parentResolver);
    genericResolver.addAll(methodResolver);

    HaxeClassModel declaringClass = methodModel.getDeclaringClass();
    if (declaringClass != null) {
      HaxeGenericResolver MethodsClassResolver = declaringClass.getGenericResolver(parentResolver);
      genericResolver.addAll(MethodsClassResolver);
    }

    List<CallExpressionArgumentModel> argumentList = getArgumentList(callExpression);
    List<CallExpressionParameterModel> parameterList = getParameterList(methodModel);
    ResultHolder returnType = methodModel.getReturnType(null);
    boolean isMacroFunction = methodModel.isMacro() && !methodModel.isStatic();
    boolean isStaticExtension = callExpression.resolveIsStaticExtension();

    SpecificHaxeClassReference callie = tryGetCallieType(callExpression, method, isStaticExtension);
    if(!callie.isUnknown()) genericResolver.addAll(callie.getGenericResolver());

    HaxeGenericResolver methodTranslatedResolver = translateResolverToMethodDeclaringClass(genericResolver, callie, method);

    HaxeCallExpressionContext evaluation = new HaxeCallExpressionContext(argumentList, parameterList, returnType, parentResolver, methodTranslatedResolver);
    evaluation.assignHint = tryCastAssignHintToReturnType(assignHint, returnType); // casting to returnType to make sure typeParams matches.
    evaluation.isStaticExtension = isStaticExtension;
    evaluation.isMacroFunction = isMacroFunction;
    evaluation.callie = callie;



    return evaluation;
  }

  private static @Nullable SpecificTypeReference tryCastAssignHintToReturnType(@Nullable SpecificTypeReference assignHint, ResultHolder returnType) {
    if (returnType != null && !returnType.isUnknown() && returnType.isClassType()) {
      if (assignHint instanceof  SpecificHaxeClassReference hintClassReference) {
        SpecificHaxeClassReference castedHint = hintClassReference.tryCastTo(returnType.getClassType());
        return castedHint != null ? castedHint : assignHint;
      }
    }
    return assignHint;
  }

  @NotNull
  public static HaxeCallExpressionContext createContextForFunctionCall(@NotNull HaxeCallExpression callExpression,
                                                                       @NotNull SpecificFunctionReference function) {

    HaxeGenericResolver genericResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(callExpression);

    List<CallExpressionArgumentModel> argumentList = getArgumentList(callExpression);
    List<CallExpressionParameterModel> parameterList = getParameterList(function);
    ResultHolder returnType = function.getReturnType();
    

    HaxeCallExpressionContext evaluation = new HaxeCallExpressionContext(argumentList, parameterList, returnType, genericResolver, null);
    evaluation.isStaticExtension = false;
    evaluation.isMacroFunction = false;
    evaluation.callie = tryGetCallieType(callExpression, null, evaluation.isStaticExtension);

    return evaluation;
  }

  @Nullable
  public static HaxeCallExpressionContext createContextForConstructorCall(@NotNull HaxeNewExpression newExpression) {
    return createContextForConstructorCall(newExpression, null);
  }
  public static HaxeCallExpressionContext createContextForConstructorCall(@NotNull HaxeNewExpression newExpression, @Nullable ResultHolder assignHint) {

    HaxeGenericResolver genericResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(newExpression);
    List<CallExpressionArgumentModel> argumentList = getArgumentList(newExpression);
    ResultHolder type = HaxeTypeResolver.getTypeFromType(newExpression.getType());
    SpecificHaxeClassReference classType = type.getClassType();
    if (classType != null) {
      SpecificTypeReference typeRef = classType.fullyResolveTypeDefAndUnwrapNullTypeReference();
      if (typeRef instanceof SpecificHaxeClassReference classReference ) {
        HaxeGenericResolver referenceGenericResolver = classReference.getGenericResolver();
        HaxeClassModel classModel = classReference.getHaxeClassModel();
        if (classModel != null) {
          HaxeMethodModel constructorModel = classModel.getConstructor(genericResolver);
          if (constructorModel != null) {
            List<CallExpressionParameterModel> parameterList = getParameterList(constructorModel);

            // we want the resolver to have constraints for any method typeParameters and any ClassParameters
            // this so that we can "inherit" types from arguments for our resolver
            HaxeGenericResolver constructorResolver = constructorModel.getGenericResolver(genericResolver);
            HaxeGenericResolver classResolver = classModel.getGenericResolver(genericResolver);
            constructorResolver.addAll(classResolver);

            constructorResolver.addAll(referenceGenericResolver);
            // Note we use "type" directly from new expression and not the fully resolved one, or the one from constructorModel
            // we do this to make sure  we return typedefs if thats what the input was.
            HaxeCallExpressionContext evaluation = new HaxeCallExpressionContext(argumentList, parameterList, type, constructorResolver, null);
            evaluation.assignHint = assignHint != null ? assignHint.getType() : null;
            evaluation.isStaticExtension = false;
            evaluation.isMacroFunction = false;
            evaluation.isConstructor = true;
            return evaluation;
          }
        }
      }
    }
    return null;
  }




  private static List<CallExpressionParameterModel> getParameterList(@NotNull SpecificFunctionReference function) {
    return function.getArguments().stream()
            .filter(not(HaxeArgument::isVoid))
            .map(CallExpressionParameterModel::fromFunctionArgument)
            .toList();
  }


  private static @NotNull List<CallExpressionParameterModel> getParameterList(HaxeMethodModel methodModel) {
    return methodModel.getParameters().stream()
      .map(CallExpressionParameterModel::fromParameter)
      .toList();
  }

  private static @NotNull List<CallExpressionArgumentModel> getArgumentList(@NotNull HaxeCallExpression callExpression) {
    List<CallExpressionArgumentModel> argumentList = new ArrayList<>();
    HaxeCallExpressionList expressionListPsi = callExpression.getExpressionList();
    if (expressionListPsi != null) {
      List<HaxeExpression> expressions = expressionListPsi.getExpressionList();
      for (HaxeExpression expression : expressions) {
        ResultHolder result = HaxeExpressionEvaluator.evaluateWithRecursionGuard(expression).result;
        CallExpressionArgumentModel model = CallExpressionArgumentModel.create(expression, result.getType());
        argumentList.add(model);
      }
    }
    return argumentList;
  }
  private static @NotNull List<CallExpressionArgumentModel> getArgumentList(@NotNull HaxeNewExpression newExpression) {
    List<CallExpressionArgumentModel> argumentList = new ArrayList<>();
      List<HaxeExpression> expressions = newExpression.getExpressionList();
      for (HaxeExpression expression : expressions) {
        ResultHolder result = HaxeExpressionEvaluator.evaluateWithRecursionGuard(expression).result;
        CallExpressionArgumentModel model = CallExpressionArgumentModel.create(expression, result.getType());
        argumentList.add(model);
      }
    return argumentList;
  }

  private static @NotNull List<CallExpressionArgumentModel> getArgumentList(@NotNull List<SpecificTypeReference> types) {
    List<CallExpressionArgumentModel> argumentList = new ArrayList<>();
    for (SpecificTypeReference type : types) {
      CallExpressionArgumentModel model = CallExpressionArgumentModel.create(type.getElementContext(), type);
      argumentList.add(model);
    }
    return argumentList;
  }

  private static @NotNull HaxeGenericResolver translateResolverToMethodDeclaringClass(HaxeGenericResolver callExpressionResolver,
                                                                                      SpecificHaxeClassReference callie,
                                                                                      @NotNull HaxeMethod method) {
    if (callie == null) return  callExpressionResolver;
    if (callie.getHaxeClass() == null) return  callExpressionResolver;

    HaxeClassModel declaringClass = method.getModel().getDeclaringClass();
    if (declaringClass == null)return  callExpressionResolver;

    HaxeClass callieClass = callie.getHaxeClass();
    HaxeClass methodOwnerClass = declaringClass.haxeClass;
    return callExpressionResolver.translateFromTo(callieClass, methodOwnerClass);
  }


  @NotNull
  public static SpecificHaxeClassReference tryGetCallieType(@NotNull HaxeCallExpression callExpression) {
    return tryGetCallieType(callExpression, null, false);
  }
  @NotNull
  public static SpecificHaxeClassReference tryGetCallieType(@NotNull HaxeCallExpression callExpression,  @Nullable HaxeMethod method, boolean extensionMethod) {

    HaxeExpression expression = callExpression.getExpression();
    if (expression != null) {
      @NotNull PsiElement[] children = expression.getChildren();
      // if we got more than one child we are a chain and need to resolve the chain to know correct class
      if (children.length > 1) {
        PsiElement child = children[children.length - 2];
        HaxeExpressionEvaluatorContext evaluatorContext = new HaxeExpressionEvaluatorContext(child);
        ResultHolder result = HaxeExpressionEvaluator.evaluateWithRecursionGuard(child, evaluatorContext, null).result;
        if (!result.isUnknown() && result.getClassType() != null) return result.getClassType();

      }else {
        // if only 1 child then we are calling on default "this" class reference
        HaxeClass type = PsiTreeUtil.getParentOfType(callExpression.getExpression(), HaxeClass.class);
        if(type != null) {
          HaxeClassModel model = type.getModel();
          SpecificHaxeClassReference classType = model.getInstanceType().getClassType();
          if(classType != null) return classType;
        }
      }
    }
    // fallback: if we know the method we know its class (but need to check if used as extension method)
    if (! extensionMethod && method !=  null) {
      HaxeClassModel classModel = method.getModel().getDeclaringClass();
      if(classModel != null) {
        SpecificHaxeClassReference classType = classModel.getInstanceType().getClassType();
        if(classType != null) return classType;
      }
    }

    return SpecificTypeReference.getUnknown(callExpression);
  }
}

