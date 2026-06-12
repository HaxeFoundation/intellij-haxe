package com.intellij.plugins.haxe.model.evaluator.callexpression;

import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.HaxeParameterModel;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluatorContext;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.intellij.plugins.haxe.lang.psi.impl.HaxeReferenceUtil.wrapTypeInClassOrEnum;
import static java.util.function.Predicate.not;

public class HaxeCallExpressionUtil {

  /**
   * Picks the extension-method overload whose first parameter fits the receiver type best
   * (ex. a Map&lt;String, Int&gt; receiver prefers ReadOnlyMap&lt;K, Int&gt; over ReadOnlyMap&lt;K, Float&gt;).
   * Ties keep the earliest declared candidate.
   */
  public static HaxeMethodModel pickBestExtensionOverload(@NotNull List<HaxeMethodModel> candidates,
                                                          @NotNull SpecificTypeReference receiverType) {
    ResultHolder receiver = receiverType.createHolder();
    HaxeMethodModel best = candidates.getFirst();
    int bestScore = -1;
    for (HaxeMethodModel candidate : candidates) {
      List<HaxeParameterModel> parameters = candidate.getParameters();
      if (parameters.isEmpty()) continue;
      int score = argumentFitScore(parameters.getFirst().getType(null), receiver);
      if (score > bestScore) {
        best = candidate;
        bestScore = score;
      }
    }
    return best;
  }

  /**
   * Scores how well a call evaluation's arguments fit the method's parameters, for choosing
   * between overloads the way the compiler does: an exact (deep) type match beats a parameter
   * that merely accepts the argument through widening (ex. an Int argument prefers an Int
   * parameter over a Float one). Callers comparing candidates keep the earliest declared one
   * on ties.
   */
  public static int evaluationFitScore(@NotNull HaxeCallExpressionEvaluation evaluation) {
    int score = 0;
    for (Map.Entry<Integer, Integer> entry : evaluation.getArgumentToParameterMapping().entrySet()) {
      ResultHolder argumentType = evaluation.getArgumentType(entry.getKey());
      ResultHolder parameterType = evaluation.getParameterType(entry.getValue());
      score += argumentFitScore(parameterType, argumentType);
    }
    return score;
  }

  // how well an argument fits a parameter: 2 = exact deep match, 1 = assignable through widening, 0 = no fit
  private static int argumentFitScore(@Nullable ResultHolder parameterType, @Nullable ResultHolder argumentType) {
    if (parameterType == null || argumentType == null) return 0;
    if (typesMatchDeep(parameterType.getType(), argumentType.getType(), 0)) return 2;
    return parameterType.canAssign(argumentType) ? 1 : 0;
  }

  // compares types structurally, descending into type parameters and unwrapping abstracts and
  // typedefs so that ex. ReadOnlyMap<K, Int> matches Map<String, Int> but not Map<String, Float>;
  // unresolved slots and free type parameters are treated as matching anything
  private static boolean typesMatchDeep(@Nullable SpecificTypeReference parameterType,
                                        @Nullable SpecificTypeReference argumentType,
                                        int depth) {
    if (depth > 8) return true;
    if (parameterType == null || argumentType == null) return false;
    parameterType = unwrapNullWrapper(parameterType);
    argumentType = unwrapNullWrapper(argumentType);
    if (parameterType.isUnknown() || argumentType.isUnknown()) return true;
    if (parameterType.isTypeParameter() || argumentType.isTypeParameter()) return true;

    if (!(parameterType instanceof SpecificHaxeClassReference) || !(argumentType instanceof SpecificHaxeClassReference)) {
      // function types etc: require mutual assignability for an exact match
      ResultHolder param = parameterType.createHolder();
      ResultHolder arg = argumentType.createHolder();
      return param.canAssign(arg) && arg.canAssign(param);
    }

    SpecificHaxeClassReference param = (SpecificHaxeClassReference)parameterType;
    SpecificHaxeClassReference arg = (SpecificHaxeClassReference)argumentType;

    // align both sides on the same class by unwrapping abstract underlying types and typedefs
    for (int i = 0; i < 4 && !referencesSameClass(param, arg); i++) {
      SpecificHaxeClassReference unwrappedParam = unwrapToUnderlying(param);
      if (unwrappedParam != null) {
        param = unwrappedParam;
        continue;
      }
      SpecificHaxeClassReference unwrappedArg = unwrapToUnderlying(arg);
      if (unwrappedArg != null) {
        arg = unwrappedArg;
        continue;
      }
      break;
    }
    if (!referencesSameClass(param, arg)) return false;

    ResultHolder[] paramSpecifics = param.getSpecifics();
    ResultHolder[] argSpecifics = arg.getSpecifics();
    // raw usage on either side leaves the type parameters unconstrained
    if (paramSpecifics.length != argSpecifics.length) return paramSpecifics.length == 0 || argSpecifics.length == 0;
    for (int i = 0; i < paramSpecifics.length; i++) {
      if (!typesMatchDeep(paramSpecifics[i].getType(), argSpecifics[i].getType(), depth + 1)) return false;
    }
    return true;
  }

  // Null<T> is transparent for matching purposes (a Map.get result is Null<V> but unifies with V)
  private static SpecificTypeReference unwrapNullWrapper(SpecificTypeReference type) {
    int guard = 0;
    while (type instanceof SpecificHaxeClassReference classReference
           && classReference.isNullType()
           && classReference.getSpecifics().length == 1
           && guard++ < 4) {
      type = classReference.getSpecifics()[0].getType();
    }
    return type;
  }

  @Nullable
  private static SpecificHaxeClassReference unwrapToUnderlying(SpecificHaxeClassReference reference) {
    HaxeClassModel model = reference.getHaxeClassModel();
    if (model == null) return null;
    if (!model.isAbstractType() && !model.isTypedef()) return null;
    SpecificTypeReference underlying = model.getUnderlyingType(reference.getGenericResolver());
    if (underlying instanceof SpecificHaxeClassReference underlyingReference && underlyingReference != reference) {
      return underlyingReference;
    }
    return null;
  }

  private static boolean referencesSameClass(SpecificHaxeClassReference a, SpecificHaxeClassReference b) {
    if (a.isSameType(b)) return true;
    // unresolved references have no model for isSameType to compare by qualified name,
    // so fall back to the short name
    if (a.getHaxeClassModel() == null || b.getHaxeClassModel() == null) {
      String nameA = a.getHaxeClassReference().getName();
      return nameA != null && nameA.equals(b.getHaxeClassReference().getName());
    }
    return false;
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

    evaluation.isMacroMethod = methodModel.isMacro();
    // Module-level functions have no enclosing class, so they are never instance ("member")
    // macro methods: their first parameter is a real argument, not an implicit `this` (eThis).
    evaluation.isStaticMethod = methodModel.isStatic() || methodModel.getDeclaringClass() == null;
    evaluation.isEnumConstructor = false;
    evaluation.callie = callie;
    return evaluation;
  }

  @NotNull
  public static HaxeCallExpressionContextContainer createContextForMethodCall(@NotNull HaxeCallExpression callExpression,
                                                                              @Nullable SpecificTypeReference assignHint,
                                                                              @NotNull HaxeMethod method) {
    HaxeMethodModel methodModel = method.getModel();

    List<HaxeMethodModel> methodModels = new ArrayList<>();
    methodModels.add(methodModel);
    methodModels.addAll(methodModel.getOverloadsFromMeta());

      List<HaxeCallExpressionContext> list = new ArrayList<>();

      for (HaxeMethodModel model : methodModels) {
          HaxeCallExpressionContext contextForMethodCall = createContextForMethodCall(callExpression, assignHint, method, model);
          list.add(contextForMethodCall);
      }
      return HaxeCallExpressionContextContainer.create(list);

  }

  @NotNull
  public static HaxeCallExpressionContextContainer createContextForMethodCall(@NotNull HaxeCallExpression callExpression,
                                                                              @NotNull HaxeMethod method) {
    return createContextForMethodCall(callExpression, null, method);
  }

  @NotNull
  private static HaxeCallExpressionContext createContextForMethodCall(@NotNull HaxeCallExpression callExpression,
                                                                     @Nullable SpecificTypeReference assignHint,
                                                                    @NotNull HaxeMethod methodPsi,
                                                                    @NotNull HaxeMethodModel methodModel
  ) {
    ProgressIndicatorProvider.checkCanceled();

    HaxeGenericResolver genericResolver = new HaxeGenericResolver();

    // Forward the assign hint so the parent resolver's type-parameter pre-pinning loop can
    // prefer the declared assignment target over an incidental common parent class when
    // widening T across sibling-subclass arguments.
    ResultHolder assignHintHolder = assignHint == null ? null : assignHint.createHolder();
    HaxeGenericResolver parentResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(callExpression, assignHintHolder);
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
    boolean isStaticExtension = callExpression.resolveIsStaticExtension();

    SpecificHaxeClassReference callieClass = null;
    SpecificTypeReference callieType = tryGetCallieType(callExpression, methodPsi, isStaticExtension);
    if (callieType instanceof SpecificHaxeClassReference classReference) {
      if (!classReference.isUnknown()) {
        callieClass = classReference;
        genericResolver.addAll(callieClass.getGenericResolver());
      }
    }

    HaxeGenericResolver methodTranslatedResolver = translateResolverToMethodDeclaringClass(genericResolver, callieClass, methodPsi);

    boolean canCache = argumentList.stream().allMatch(CallExpressionArgumentModel::isCanCache) && returnType.cacheable;

    HaxeCallExpressionContext evaluation = new HaxeCallExpressionContext(argumentList, parameterList, returnType, parentResolver, methodTranslatedResolver);
    evaluation.assignHint = tryCastAssignHintToReturnType(assignHint, returnType); // casting to returnType to make sure typeParams matches.
    evaluation.isStaticExtension = isStaticExtension;
    evaluation.isMacroMethod = methodModel.isMacro();
    // Module-level functions have no enclosing class, so they are never instance ("member")
    // macro methods: their first parameter is a real argument, not an implicit `this` (eThis).
    evaluation.isStaticMethod = methodModel.isStatic() || methodModel.getDeclaringClass() == null;
    evaluation.isBindCall = isBindCall(callExpression);
    evaluation.isInEnumValueMatchArgument = isEnumValueMatchCall(callExpression);
    evaluation.isEnumValueMatchCallExpression = isEnumValueMatchCallExpression(callExpression);
    evaluation.isEnumConstructor = isEnumConstructor(callExpression);
    evaluation.callie = callieType;
    evaluation.canCache = canCache;


    return evaluation;
  }

  private static boolean isEnumValueMatchCall(@NotNull HaxeCallExpression callExpression) {
    HaxeCallExpression parent = PsiTreeUtil.getParentOfType(callExpression, HaxeCallExpression.class);
    while(parent != null) {
      if(isEnumValueMatchCallExpression(parent)) {
        return true;
      }
      parent = PsiTreeUtil.getParentOfType(parent, HaxeCallExpression.class);
    }
    return false;
  }
  private static boolean isEnumValueMatchCallExpression(@NotNull HaxeCallExpression callExpression) {
    if (callExpression.getExpression() instanceof HaxeReferenceExpression referenceExpression) {
      PsiElement resolve = referenceExpression.resolve();
      if (resolve instanceof HaxeMethodDeclaration methodDeclaration) {
        FullyQualifiedInfo qualifiedInfo = methodDeclaration.getModel().getQualifiedInfo();
        boolean patternMatchingCall = qualifiedInfo != null && qualifiedInfo.toString().equalsIgnoreCase("EnumValue.EnumValue.match");
        if (patternMatchingCall) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isEnumConstructor(@NotNull HaxeCallExpression callExpression) {
    if( callExpression.getExpression() instanceof HaxeReferenceExpression referenceExpression) {
        return referenceExpression.resolve() instanceof HaxeEnumValueDeclarationConstructor;
    }
    return false;
  }

  private static @Nullable SpecificTypeReference tryCastAssignHintToReturnType(@Nullable SpecificTypeReference assignHint, ResultHolder returnType) {
    if (returnType != null && !returnType.isUnknown() && returnType.isClassType()) {
      if (assignHint instanceof SpecificHaxeClassReference hintClassReference) {
        SpecificHaxeClassReference returnTypeClass = returnType.getClassType();
        if (returnTypeClass != null) {
          SpecificHaxeClassReference castedHint = hintClassReference.tryCastTo(returnTypeClass);
          if (castedHint != null && !castedHint.isSameTypeAndGenerics(returnTypeClass)) {
            return castedHint;
          }
        }
      }
    }
    return assignHint;
  }

  @NotNull
  public static HaxeCallExpressionContext createContextForFunctionCall(@NotNull HaxeCallExpression callExpression,
                                                                       @NotNull SpecificFunctionReference function) {
    ProgressIndicatorProvider.checkCanceled();

    HaxeGenericResolver genericResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(callExpression);

    List<CallExpressionArgumentModel> argumentList = getArgumentList(callExpression);
    List<CallExpressionParameterModel> parameterList = getParameterList(function);
    ResultHolder returnType = function.getReturnType();


    HaxeCallExpressionContext evaluation = new HaxeCallExpressionContext(argumentList, parameterList, returnType, genericResolver, null);
    SpecificTypeReference callie = tryGetCallieType(callExpression, null, evaluation.isStaticExtension);
    evaluation.isStaticExtension = false;
    evaluation.isMacroMethod = false;
    evaluation.isStaticMethod = false;
    evaluation.isBindCall = isBindCall(callExpression);
    evaluation.callie = callie;

    return evaluation;
  }

  public static boolean isBindCall(@NotNull HaxeCallExpression callExpression) {
    HaxeExpression expression = callExpression.getExpression();
    if (expression != null && expression.getLastChild().textMatches("bind")) {
      HaxeReference left = HaxeResolveUtil.getLeftReference(expression);
      if (left != null) {
        ResultHolder result = HaxeExpressionEvaluator.evaluate(left).result;
        return result.isFunctionType();
      }
    }
    return false;
  }

  // NOTE: Abstract types can have overloads for constructors as long as they are inlined
  // to ensure we got the right overload we do a resolve on new expression (resolves to constructor)
  private static List<HaxeMethodModel> getConstructorsModelForNewExpression(@NotNull HaxeNewExpression newExpression) {
    PsiElement constructor = newExpression.resolve();
    if(constructor instanceof  HaxeConstructorDeclaration declaration) {
      List<HaxeMethodModel> methodModels = new ArrayList<>();
      HaxeMethodModel model = declaration.getModel();
      List<HaxeMethodModel> overloadsFromMeta = model.getOverloadsFromMeta();
      methodModels.add(model);
      methodModels.addAll(overloadsFromMeta);
      return methodModels;
    }else if (constructor instanceof HaxeLocalFunctionDeclaration declaration) {
      // most likely an metadata overload  ex.  @:overload(fn..)
      return List.of(declaration.getModel());
    }else if(constructor == null){
      // resolve failed, probably parameter mismatch (we look up type and find list of normal/not-overload constructors)
      ResultHolder typeFromType = HaxeTypeResolver.getTypeFromType(newExpression.getType());
      SpecificHaxeClassReference classType = typeFromType.getClassType();
      if(!typeFromType.isUnknown() && classType != null) {
        HaxeClassModel haxeClassModel = classType.getHaxeClassModel();
        if(haxeClassModel != null) {
            return haxeClassModel.getConstructors(null);
        }
      }
    }
    return List.of();
  }

  @NotNull
  public static HaxeCallExpressionContextContainer createContextForConstructorCall(@NotNull HaxeNewExpression newExpression) {
    List<HaxeMethodModel> methodModels = getConstructorsModelForNewExpression(newExpression);
    List<HaxeCallExpressionContext> list = new ArrayList<>();
    for (HaxeMethodModel methodModel : methodModels) {
      ProgressIndicatorProvider.checkCanceled();
      HaxeCallExpressionContext call = createContextForConstructorCall(newExpression, methodModel, null);
      list.add(call);
    }
    return HaxeCallExpressionContextContainer.create(list);

  }

  public static HaxeCallExpressionContextContainer createContextForConstructorCall(@NotNull HaxeNewExpression newExpression, HaxeMethodModel methodModel) {
    List<HaxeMethodModel> methodModels =  new ArrayList<>(methodModel.getOverloadsFromMeta());
    methodModels.add(methodModel);
    List<HaxeCallExpressionContext> list = new ArrayList<>();
    for (HaxeMethodModel model : methodModels) {
      ProgressIndicatorProvider.checkCanceled();
      HaxeCallExpressionContext call = createContextForConstructorCall(newExpression, model, null);
      list.add(call);
    }
    return HaxeCallExpressionContextContainer.create(list);
  }

  @NotNull
  public static HaxeCallExpressionContextContainer createContextForConstructorCall(@NotNull HaxeNewExpression newExpression, @Nullable ResultHolder assignHint) {
    List<HaxeMethodModel> methodModels = getConstructorsModelForNewExpression(newExpression);
    List<HaxeCallExpressionContext> list = new ArrayList<>();
    for (HaxeMethodModel methodModel : methodModels) {
      ProgressIndicatorProvider.checkCanceled();
      HaxeCallExpressionContext call = createContextForConstructorCall(newExpression, methodModel, assignHint);
      list.add(call);
    }
    return HaxeCallExpressionContextContainer.create(list);

  }
  public static HaxeCallExpressionContext createContextForConstructorCall(@NotNull HaxeNewExpression newExpression, HaxeMethodModel methodModel, @Nullable ResultHolder assignHint) {

    HaxeGenericResolver genericResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(newExpression);
    List<CallExpressionArgumentModel> argumentList = getArgumentList(newExpression);
    ResultHolder type = HaxeTypeResolver.getTypeFromType(newExpression.getType());
    SpecificHaxeClassReference classType = type.getClassType();
    boolean canCache = type.cacheable && argumentList.stream().allMatch(CallExpressionArgumentModel::isCanCache);
    if (classType != null) {
      SpecificTypeReference typeRef = classType.fullyResolveTypeDefAndUnwrapNullTypeReference();
      if (typeRef instanceof SpecificHaxeClassReference classReference ) {
        HaxeGenericResolver referenceGenericResolver = classReference.getGenericResolver();
        HaxeClassModel classModel = classReference.getHaxeClassModel();
        if (classModel != null) {
          HaxeMethodModel constructorModel = null;
          // abstract types can have inline overloads
          PsiElement resolve = newExpression.getType().getReferenceExpression().resolve();
          if(resolve instanceof HaxeConstructor constructor) {

          }
          if (methodModel != null) {
            constructorModel = methodModel;
          }else {
            constructorModel = classModel.getConstructor(genericResolver);
          }
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
            evaluation.isMacroMethod = false;
            evaluation.isStaticMethod = false;
            evaluation.isEnumConstructor = false; // enums dont use the new keyword
            evaluation.isConstructor = true;
            evaluation.canCache = canCache;
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
        ProgressIndicatorProvider.checkCanceled();
        ResultHolder result = HaxeExpressionEvaluator.evaluateWithRecursionGuard(expression).result;
        CallExpressionArgumentModel model = CallExpressionArgumentModel.create(expression, result.getType(), !result.isUnknown() && result.cacheable);
        argumentList.add(model);
      }
    }
    return argumentList;
  }
  private static @NotNull List<CallExpressionArgumentModel> getArgumentList(@NotNull HaxeNewExpression newExpression) {
    List<CallExpressionArgumentModel> argumentList = new ArrayList<>();
      List<HaxeExpression> expressions = newExpression.getExpressionList();
      for (HaxeExpression expression : expressions) {
        ProgressIndicatorProvider.checkCanceled();
          ResultHolder result = HaxeExpressionEvaluator.evaluateWithRecursionGuard(expression).result;
          CallExpressionArgumentModel model = CallExpressionArgumentModel.create(expression, result.getType(), result.cacheable);
          model.canCache = result.cacheable;
          argumentList.add(model);
      }
    return argumentList;
  }

  private static @NotNull List<CallExpressionArgumentModel> getArgumentList(@NotNull List<SpecificTypeReference> types) {
    List<CallExpressionArgumentModel> argumentList = new ArrayList<>();
    for (SpecificTypeReference type : types) {
      ProgressIndicatorProvider.checkCanceled();
      CallExpressionArgumentModel model = CallExpressionArgumentModel.create(type.getElementContext(), type, false);
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
  public static SpecificTypeReference tryGetCallieType(@NotNull HaxeCallExpression callExpression) {
    return tryGetCallieType(callExpression, null, false);
  }
  @NotNull
  public static SpecificTypeReference tryGetCallieType(@NotNull HaxeCallExpression callExpression,  @Nullable HaxeMethod method, boolean extensionMethod) {


    HaxeExpression expression = callExpression.getExpression();
    if (expression != null) {
      @NotNull PsiElement[] children = expression.getChildren();
      // if we got more than one child we are a chain and need to resolve the chain to know correct class
      if (children.length > 1) {
        PsiElement child = children[children.length - 2];
        // if extension method  and callie is a specific class or Enum, wrap type in Enum<T> or CLass<T>
        if(extensionMethod && child instanceof HaxeReferenceExpression referenceExpression) {
          PsiElement resolve = referenceExpression.resolve();
          if (resolve instanceof  HaxeClass haxeClass) {
            return wrapTypeInClassOrEnum(referenceExpression, haxeClass).getType();
          }
        }
        HaxeExpressionEvaluatorContext evaluatorContext = new HaxeExpressionEvaluatorContext(child);
        ResultHolder result = HaxeExpressionEvaluator.evaluateWithRecursionGuard(child, evaluatorContext, null).result;
        if (!result.isUnknown()) return result.getType(); // can be any "type" class/function/enum

      }else {
        // if only 1 child then we are calling on default "this" class reference
        HaxeClass type = PsiTreeUtil.getStubOrPsiParentOfType(callExpression.getExpression(), HaxeClass.class);
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

