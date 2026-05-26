/*
 * Copyright 2018-2020 Eric Bishton
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

import com.intellij.openapi.util.RecursionGuard;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeTypeParameterDeclaration;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluatorContext;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.intellij.plugins.haxe.model.type.HaxeParameterUtil.mapArgumentsToParameters;

public class HaxeGenericResolverUtil {

    private static final RecursionGuard<PsiElement> statementRecursionGuard = RecursionManager.createGuard("StatementGenericResolverGuard");

  @NotNull
  public static HaxeGenericResolver generateResolverFromScopeParents(PsiElement element) {
    return generateResolverFromScopeParents(element, null);
  }

  @NotNull
  public static HaxeGenericResolver generateResolverFromScopeParents(PsiElement element, @Nullable ResultHolder assignHint) {
    HaxeGenericResolver resolver = new HaxeGenericResolver();

    appendClassGenericResolver(element, resolver);
    appendMethodGenericResolver(element, resolver);

    appendStatementGenericResolver(HaxeResolveUtil.getLeftReference(element), resolver);

    // Scope the assign hint tightly to the call-expression pre-pinning loop. Setting it
    // earlier would leak into appendStatementGenericResolver's left-reference evaluation
    // (e.g. macro-stub callees infer their return type from the surrounding hint), and
    // leaving it on the returned resolver would propagate via addAll() to call sites that
    // previously didn't see a hint here.
    if (assignHint != null) resolver.setAssignHint(assignHint);
    appendCallExpressionGenericResolver(element, resolver);
    if (assignHint != null) resolver.setAssignHint(null);

    return resolver;
  }

  @Nullable
  public static HaxeGenericResolver generateResolverForSupers(HaxeClassModel classModel) {
    if (null == classModel) return null;
    return generateResolverForSupers(classModel.getPsi());
  }

  @Nullable
  public static HaxeGenericResolver generateResolverForSupers(HaxeClass clazz) {
    if (null == clazz) return null;
    HaxeGenericResolver resolver = new HaxeGenericResolver();
    for (PsiClass superClazz : clazz.getSupers()) {
      appendClassGenericResolver(superClazz, resolver);
    }
    return resolver;
  }

  @Nullable static HaxeGenericResolver appendClassGenericResolver(PsiElement element, @NotNull HaxeGenericResolver resolver) {
    HaxeClass clazz = element instanceof HaxeClass haxeClass
                      ? haxeClass
                      : PsiTreeUtil.getStubOrPsiParentOfType(element, HaxeClass.class);

    HaxeClassModel classModel = HaxeClassModel.fromElement(clazz);
    if (null != classModel) {
      resolver.addAll(classModel.getGenericResolver(null));
    }

    return resolver;
  }

  @NotNull public static HaxeGenericResolver appendMethodGenericResolver(PsiElement element, @NotNull HaxeGenericResolver resolver) {
    HaxeMethod method = PsiTreeUtil.getStubOrPsiParentOfType(element, HaxeMethod.class);
    if (null != method) {
      appendMethodGenericResolver(method, resolver);

      HaxeMethodModel model = method.getModel();
      resolver.addAll(model.getGenericResolver(resolver));
    }

    return resolver;
  }

  @NotNull static HaxeGenericResolver appendStatementGenericResolver(PsiElement element, @NotNull HaxeGenericResolver resolver) {
    if (null == element) return resolver;

    if (element instanceof HaxeReference) {
        ResultHolder result1 =  statementRecursionGuard.doPreventingRecursion(element, true,
                () -> HaxeExpressionEvaluator.evaluate(element, new HaxeExpressionEvaluatorContext(element), resolver.copy()).result);
      if (result1 != null && !result1.isUnknown() && result1.getClassType() != null) {
        SpecificHaxeClassReference result = result1.getClassType();
        resolver.addAll(result.getGenericResolver());
      if (result.getHaxeClass() != null) {
        resolver.addAll(getResolverSkipAbstractNullScope(result.getHaxeClass().getModel(), result.getGenericResolver()));
        resolver.addAll(result.getHaxeClass().getMemberResolver(resolver));
      }
      }
    }
    return resolver;
  }
// todo replace  with proper recursion guard
  private static final ThreadLocal<Stack<HaxeCallExpression>> processingCallExpressions = ThreadLocal.withInitial(Stack::new);
  @NotNull public static HaxeGenericResolver appendCallExpressionGenericResolver(@Nullable PsiElement element, @NotNull HaxeGenericResolver resolver) {
    if (null == element) return resolver;

    Map<Integer, HaxeParameterUtil.ParameterToArgumentAndResolver> parameterArgumentMap = null;

    if (element instanceof HaxeCallExpression call) {
        Stack<HaxeCallExpression> stack = processingCallExpressions.get();
        // already processing this call expression, do not start another attempt
        if (stack.contains(call)) {
          return resolver;
        }
      try {
        stack.add(call);

      HaxeExpression callExpression = call.getExpression();
      PsiElement callTarget = callExpression instanceof HaxeReferenceExpression referenceExpression ? referenceExpression.resolve() : null;
      if (null != callTarget) {
        HaxeGenericResolver methodResolver = null;
        List<HaxeParameterModel> methodParameters = null;
        HaxeMethodModel methodModelForReturn = null;
        if (callTarget instanceof HaxeMethodDeclaration) {
          HaxeMethodModel methodModel = (HaxeMethodModel)HaxeMethodModel.fromPsi(callTarget);
          if (null != methodModel) {
            methodResolver =
              methodModel.getGenericResolver(new HaxeGenericResolver()); // Use a new resolver to capture only the method types.
            methodParameters = methodModel.getParameters();
            methodModelForReturn = methodModel;
          }
        } //else if (callTarget instanceof HaxeLocalFunctionDeclaration) {
          // TODO: Implement a HaxeLocalFunctionModel and use it here.
          // }
        if (null != methodResolver && !methodResolver.isEmpty() && null != methodParameters && !methodParameters.isEmpty()) {

          // Project the call site's assign hint through the function's declared return type to
          // derive a per-type-parameter "preferred" concrete type. The pre-pinning loop below
          // forwards this as the unifier's `suggestedType` so sibling subclasses sharing an
          // interface collapse to the interface the user actually assigned to (instead of an
          // incidental common parent class chosen by getCompatibleTypes() iteration order).
          Map<HaxeTypeParameterDeclaration, ResultHolder> typeParameterSuggestions = new HashMap<>();
          ResultHolder assignHint = resolver.getAssignHint();
          if (assignHint != null && !assignHint.isUnknown() && methodModelForReturn != null) {
            ResultHolder declaredReturnType = methodModelForReturn.getReturnType(null);
            if (declaredReturnType != null) {
              mapTypeParameters(typeParameterSuggestions, declaredReturnType, assignHint);
            }
          }
          // Loop through all of the parameters of the method and call site.  For those
          // that use a type parameter where the resolver entry doesn't have a known type,
          // grab the type from the call site and put that in the resolver.  Any resolver
          // entries that have a constraint should keep the constraint and let the type
          // checker deal with any issues.
          HaxeExpressionList parameterList = call.getExpressionList();
          List<HaxeExpression> expressionList =new ArrayList<>();
          if(null != parameterList) {
            expressionList.addAll(parameterList.getExpressionList());
          }
          // if this is a static extension method call we need to add the type of the callie
          if (call.resolveIsStaticExtension()) {
            // add callie as parameter
            HaxeReference callieReference = HaxeResolveUtil.getLeftReference(callExpression);
            if (callieReference != null)expressionList.addFirst(callieReference);
          }
          if (!expressionList.isEmpty()) {


            parameterArgumentMap = mapArgumentsToParameters(call, methodParameters, expressionList, false, resolver);

            for (Map.Entry<Integer, HaxeParameterUtil.ParameterToArgumentAndResolver> entry : parameterArgumentMap.entrySet()) {

              ResultHolder parameterType = entry.getValue().parameter().getType();
              ResultHolder argumentType = entry.getValue().argumentType();
              Map<HaxeTypeParameterDeclaration, ResultHolder> typeParameterMap = new HashMap<>();
              mapTypeParameters(typeParameterMap, parameterType, argumentType);

              for (Map.Entry<HaxeTypeParameterDeclaration, ResultHolder> tpEntry : typeParameterMap.entrySet()) {
                HaxeTypeParameterDeclaration typeParameter = tpEntry.getKey();

                ResultHolder typeParameterType = tpEntry.getValue();
                ResultHolder existingType = methodResolver.resolveArgument(typeParameter);

                if (existingType != null) {
                  // If the existing pin already accepts the new argument, the existing pin is the
                  // wider type; keep it as-is. Otherwise the new arg is either wider or a sibling
                  // of the existing pin — in both cases let the unifier find the right common type.
                  // (Without the sibling branch, `pick<T>(new A(), new B())` where A and B share an
                  // interface would silently leave T pinned to A and produce a false-positive
                  // "Expected: A, got: B" downstream.)
                  if (existingType.canAssign(typeParameterType)) {
                    continue;
                  }
                  ResultHolder suggestion = typeParameterSuggestions.get(typeParameter);
                  SpecificTypeReference suggestionType = (suggestion != null && !suggestion.isUnknown()) ? suggestion.getType() : null;
                  SpecificTypeReference existingRef = existingType.getType();
                  SpecificTypeReference newRef = typeParameterType.getType();
                  SpecificTypeReference unifiedRef =
                    HaxeTypeUnifier.unify(existingRef, newRef, existingRef.context, suggestionType, UnificationRules.DEFAULT);
                  if (unifiedRef.isUnknown()) {
                    continue;
                  }
                  typeParameterType = unifiedRef.createHolder();
                }

                ResultHolder constraint = methodResolver.resolveConstraint(typeParameter);
                // resolve constraint if type parameter ex. (T:B, B:DisplayObject)
                if (constraint != null && constraint.isTypeParameter()) constraint = methodResolver.resolve(constraint);
                if (constraint != null && !constraint.canAssign(typeParameterType)) continue;

                if (typeParameterType.isDynamic() && typeParameterType.getConstant() instanceof HaxeNull) {
                  continue; // ignore null arguments
                }
                methodResolver.addArgument(typeParameter, typeParameterType);
              }
            }
        }
          resolver.addAll(methodResolver);
        }
      }
      } finally {
        stack.remove(call);
      }
    }
    return resolver;
  }

  // Hack?
  // Since null<T> references in some places are handled as if they where type T
  // we also have to support resolving Type Parameters as if Null<T> was just T
  public  static  HaxeGenericResolver getResolverSkipAbstractNullScope(@Nullable HaxeClassModel model, @NotNull HaxeGenericResolver resolver) {
    if (model instanceof HaxeAbstractClassModel abstractClassModel) {
      if ("Null".equals(abstractClassModel.getName()) && abstractClassModel.isCoreType()) {
        HaxeGenericParam param = abstractClassModel.getAbstractClass().getGenericParam();
        if (param != null) {
          HaxeGenericListPart generic = param.getGenericListPartList().get(0);
          ResultHolder resolve = resolver.resolveTypeParameter(generic);
          return resolve == null || resolve.getClassType() == null ? resolver : resolve.getClassType().getGenericResolver();
        }
      }
    }
    return resolver;
  }



  private static void mapTypeParameters(Map<HaxeTypeParameterDeclaration, ResultHolder> map, ResultHolder parameter, ResultHolder argument) {
    if (parameter == null || argument == null) return;

    SpecificTypeReference paramType = parameter.getType();
    if (paramType instanceof SpecificHaxeClassReference classReference) {
      paramType = classReference.fullyResolveTypeDefAndUnwrapNullTypeReference();
    }

    SpecificTypeReference argType = argument.getType();
    if (argType instanceof SpecificHaxeClassReference classReference) {
      argType = classReference.fullyResolveTypeDefAndUnwrapNullTypeReference();
    }

    if (paramType.isTypeParameter() && !argType.isTypeParameter() ) {
      HaxeClass aClass = parameter.tryUnwrapNullType().getClassType().getHaxeClass();
      if(aClass instanceof HaxeTypeParameterDeclaration typeParameter) {
        map.put(typeParameter, argument);
      }
      return;
    }

    if (paramType instanceof  SpecificHaxeClassReference paramClass && argType instanceof SpecificHaxeClassReference argClass) {
      @NotNull ResultHolder[] paramSpecifics = paramClass.getSpecifics();
      @NotNull ResultHolder[] argSpecifics = argClass.getSpecifics();
      if (paramSpecifics.length == argSpecifics.length) {
        for (int i = 0; i < paramSpecifics.length; i++) {
          ResultHolder paramSpec = paramSpecifics[i];
          ResultHolder argSpec = argSpecifics[i];
          mapTypeParameters(map, paramSpec, argSpec);
        }
      }
    }
    else if (paramType instanceof  SpecificFunctionReference paramFn && argType instanceof SpecificFunctionReference argfn) {
      mapTypeParametersFunction(map, paramFn, argfn);
    }
  }

  private static void mapTypeParametersFunction(Map<HaxeTypeParameterDeclaration, ResultHolder> map, SpecificFunctionReference parameter, SpecificFunctionReference argument) {
    List<HaxeArgument> paramArgs = parameter.getArguments();
    List<HaxeArgument> argArgs = argument.getArguments();
    if (paramArgs.size() == argArgs.size()) {
      for (int i = 0; i < paramArgs.size(); i++) {
        HaxeArgument paramArg = paramArgs.get(i);
        HaxeArgument argArg = argArgs.get(i);
        mapTypeParameters(map, paramArg.getType(), argArg.getType());
      }
    }
  }

  /**
   * Resolve the type parameters of a generic method that was passed by reference (no call
   * parentheses) into a position whose declared type is a concrete function type. Walks the
   * declared (method-side) function signature against the expected (hint-side) signature
   * pairwise, binding each method type parameter to the corresponding concrete type from the
   * hint. Returns null when nothing could be bound, when either side is missing, or when the
   * method reference is not backed by a {@link HaxeMethodModel}.
   *
   * The returned resolver contains only the inferred method-level bindings; callers typically
   * feed it to {@link HaxeMethodModel#getFunctionType(HaxeGenericResolver)} to obtain a fully
   * substituted function signature for the subsequent assignability check.
   */
  @Nullable
  public static HaxeGenericResolver buildMethodTypeParamResolverFromHint(@Nullable SpecificFunctionReference methodReference,
                                                                         @Nullable SpecificFunctionReference hint) {
    if (methodReference == null || hint == null) return null;
    if (methodReference.method == null) return null;

    Map<HaxeTypeParameterDeclaration, ResultHolder> bindings = new HashMap<>();

    List<HaxeArgument> methodArgs = methodReference.getArguments();
    List<HaxeArgument> hintArgs = hint.getArguments();
    int pairCount = Math.min(methodArgs.size(), hintArgs.size());
    for (int i = 0; i < pairCount; i++) {
      mapTypeParameters(bindings, methodArgs.get(i).getType(), hintArgs.get(i).getType());
    }
    mapTypeParameters(bindings, methodReference.getReturnType(), hint.getReturnType());

    if (bindings.isEmpty()) return null;

    HaxeGenericResolver resolver = new HaxeGenericResolver();
    for (Map.Entry<HaxeTypeParameterDeclaration, ResultHolder> entry : bindings.entrySet()) {
      resolver.add(entry.getKey(), entry.getValue());
    }
    return resolver;
  }
}
