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

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.type.resolver.ResolveSource;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.intellij.plugins.haxe.model.type.HaxeParameterUtil.mapArgumentsToParameters;

public class HaxeGenericResolverUtil {

  @NotNull
  public static HaxeGenericResolver generateResolverFromScopeParents(PsiElement element) {
    HaxeGenericResolver resolver = new HaxeGenericResolver();

    HaxeMethod method = element instanceof HaxeMethod
                        ? (HaxeMethod)element
                        : UsefulPsiTreeUtil.getParentOfType(element, HaxeMethod.class);
    boolean isStatic = null != method && method.isStatic();
    if (!isStatic) {
      appendClassGenericResolver(element, resolver);
    }
    appendMethodGenericResolver(element, resolver);
    appendStatementGenericResolver(HaxeResolveUtil.getLeftReference(element), resolver);
    appendCallExpressionGenericResolver(element, resolver);
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
    HaxeClass clazz = element instanceof HaxeClass
                      ? (HaxeClass) element
                      : UsefulPsiTreeUtil.getParentOfType(element, HaxeClass.class);

    HaxeClassModel classModel = HaxeClassModel.fromElement(clazz);
    if (null != classModel) {
      resolver.addAll(classModel.getGenericResolver(null));
    }

    return resolver;
  }

  @NotNull public static HaxeGenericResolver appendMethodGenericResolver(PsiElement element, @NotNull HaxeGenericResolver resolver) {
    HaxeMethod method = UsefulPsiTreeUtil.getParentOfType(element, HaxeMethod.class);
    if (null != method) {
      appendMethodGenericResolver(method, resolver);

      HaxeMethodModel model = method.getModel();
      resolver.addAll(model.getGenericResolver(resolver));
    }

    return resolver;
  }

  @NotNull static HaxeGenericResolver appendStatementGenericResolver(PsiElement element, @NotNull HaxeGenericResolver resolver) {
    if (null == element) return resolver;

    HaxeReference left = HaxeResolveUtil.getLeftReference(element);
    if ( null != left) {
      appendStatementGenericResolver(left, resolver);
    }
    if (element instanceof HaxeReference) {
      HaxeResolveResult result = ((HaxeReference)element).resolveHaxeClass();
      resolver.addAll(result.getGenericResolver());
      if (result.getHaxeClass() != null) {
        resolver.addAll(getResolverSkipAbstractNullScope(result.getHaxeClass().getModel(), result.getGenericResolver()));
        resolver.addAll(result.getHaxeClass().getMemberResolver(resolver));
      }
    }
    return resolver;
  }

  @NotNull public static HaxeGenericResolver appendCallExpressionGenericResolver(@Nullable PsiElement element, @NotNull HaxeGenericResolver resolver) {
    if (null == element) return resolver;

    Map<Integer, HaxeParameterUtil.ParameterToArgumentAndResolver> parameterArgumentMap = null;

    if (element instanceof HaxeCallExpression callExpression2) {
      HaxeCallExpression call = (HaxeCallExpression)element;
      HaxeExpression callExpression = call.getExpression();
      PsiElement callTarget = callExpression instanceof HaxeReferenceExpression ? ((HaxeReferenceExpression)callExpression).resolve() : null;
      if (null != callTarget) {
        HaxeGenericResolver methodResolver = null;
        List<HaxeParameterModel> methodParameters = null;
        if (callTarget instanceof HaxeMethodDeclaration) {
          HaxeMethodModel methodModel = (HaxeMethodModel)HaxeMethodModel.fromPsi(callTarget);
          if (null != methodModel) {
            methodResolver =
              methodModel.getGenericResolver(new HaxeGenericResolver()); // Use a new resolver to capture only the method types.
            methodParameters = methodModel.getParameters();
          }
        } //else if (callTarget instanceof HaxeLocalFunctionDeclaration) {
          // TODO: Implement a HaxeLocalFunctionModel and use it here.
          // }
        if (null != methodResolver && !methodResolver.isEmpty() && null != methodParameters && !methodParameters.isEmpty()) {
          // Loop through all of the parameters of the method and call site.  For those
          // that use a type parameter where the resolver entry doesn't have a known type,
          // grab the type from the call site and put that in the resolver.  Any resolver
          // entries that have a constraint should keep the constraint and let the type
          // checker deal with any issues.
          HaxeExpressionList parameterList = call.getExpressionList();
          List<HaxeExpression> expressionList = null != parameterList ? parameterList.getExpressionList() : null;
          if (null != expressionList) {


            parameterArgumentMap = mapArgumentsToParameters(callExpression2, methodParameters, expressionList, false, resolver);

            for (Map.Entry<Integer, HaxeParameterUtil.ParameterToArgumentAndResolver> entry : parameterArgumentMap.entrySet()) {

              ResultHolder parameterType = entry.getValue().parameter().getType();
              ResultHolder argumentType = entry.getValue().argumentType();
              Map<String, ResultHolder> typeParameterMap = new HashMap<>();
              mapTypeParameters(typeParameterMap, parameterType, argumentType);

              for (Map.Entry<String, ResultHolder> tpEntry : typeParameterMap.entrySet()) {
                String typeParameterName = tpEntry.getKey();
                ResultHolder typeParameterType = tpEntry.getValue();
                ResultHolder existingType = methodResolver.resolveArgument(typeParameterName);
                if (existingType == null || typeParameterType.canAssign(existingType)) {
                  if (existingType != null) {
                    typeParameterType = HaxeTypeUnifier.unify(existingType, typeParameterType);
                  }
                  ResultHolder constraint = methodResolver.resolveConstraint(typeParameterName);
                  if (constraint == null || constraint.canAssign(typeParameterType)) {
                    methodResolver.add(typeParameterName, typeParameterType, ResolveSource.ARGUMENT_TYPE);
                  }
                }
              }



            }
          }

          resolver.addAll(methodResolver);
        }
      }
    }
    return resolver;
  }

  // Hack?
  // Since null<T> references in some places are handled as if they where type T
  // we also have to support resolving Type Parameters as if Null<T> was just T
  public  static  HaxeGenericResolver getResolverSkipAbstractNullScope(@Nullable HaxeClassModel model, @NotNull HaxeGenericResolver resolver) {
    if (model instanceof HaxeAbstractClassModel) {
      HaxeAbstractClassModel abstractClassModel = (HaxeAbstractClassModel)model;
      if ("Null".equals(abstractClassModel.getName()) && abstractClassModel.isCoreType()) {
        HaxeGenericParam param = abstractClassModel.getAbstractClass().getGenericParam();
        if (param != null) {
          HaxeGenericListPart generic = param.getGenericListPartList().get(0);
          ResultHolder resolve = resolver.resolve(generic);
          return resolve == null || resolve.getClassType() == null ? resolver : resolve.getClassType().getGenericResolver();
        }
      }
    }
    return resolver;
  }



  private static void mapTypeParameters(Map<String, ResultHolder> map, ResultHolder parameter, ResultHolder argument) {
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
      map.put(parameter.getClassType().getClassName(), argument);
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

  private static void mapTypeParametersFunction(Map<String, ResultHolder> map, SpecificFunctionReference parameter, SpecificFunctionReference argument) {
    List<SpecificFunctionReference.Argument> paramArgs = parameter.getArguments();
    List<SpecificFunctionReference.Argument> argArgs = argument.getArguments();
    if (paramArgs.size() == argArgs.size()) {
      for (int i = 0; i < paramArgs.size(); i++) {
        SpecificFunctionReference.Argument paramArg = paramArgs.get(i);
        SpecificFunctionReference.Argument argArg = argArgs.get(i);
        mapTypeParameters(map, paramArg.getType(), argArg.getType());
      }
    }
  }

}
