/*
 * Copyright 2018-2019 Eric Bishton
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
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.HaxeParameterModel;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HaxeGenericResolverUtil {

  @Nullable
  public static HaxeGenericResolver generateResolverFromScopeParents(PsiElement element) {
    HaxeGenericResolver resolver = new HaxeGenericResolver();

    HaxeMethod method = UsefulPsiTreeUtil.getParentOfType(element, HaxeMethod.class);
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

  @Nullable static HaxeGenericResolver appendMethodGenericResolver(PsiElement element, @NotNull HaxeGenericResolver resolver) {
    HaxeMethod method = UsefulPsiTreeUtil.getParentOfType(element, HaxeMethod.class);
    if (null != method) {
      appendMethodGenericResolver(method, resolver);

      HaxeMethodModel model = method.getModel();
      resolver.addAll(model.getGenericResolver(resolver));
    }

    return resolver;
  }

  @Nullable static HaxeGenericResolver appendStatementGenericResolver(PsiElement element, @NotNull HaxeGenericResolver resolver) {
    if (null == element) return resolver;

    HaxeReference left = HaxeResolveUtil.getLeftReference(element);
    if ( null != left) {
      appendStatementGenericResolver(left, resolver);
    }
    if (element instanceof HaxeReference) {
      HaxeClassResolveResult result = ((HaxeReference)element).resolveHaxeClass();
      resolver.addAll(result.getGenericResolver());
    }
    return resolver;
  }

  @Nullable static HaxeGenericResolver appendCallExpressionGenericResolver(PsiElement element, @NotNull HaxeGenericResolver resolver) {
    if (null == element) return resolver;

    if (element instanceof HaxeCallExpression) {
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
            int len = Math.min(expressionList.size(), methodParameters.size());
            for (int n = 0; n < len; n++) {
              ResultHolder paramType = methodParameters.get(n).getType();
              String paramName = paramType.getType().toStringWithoutConstant();
              ResultHolder resolverType = methodResolver.resolve(paramType);
              // if NULL == resolverType, then the type name isn't in the resolver.
              // if NULL != resolverType and !isUnknown, then the type is already set.
              // if NULL != resolverType and isUnknown, then poke the type from the call site.
              if (null != resolverType && resolverType.isUnknown()) {
                HaxeExpression expression = expressionList.get(n);

                HaxeExpressionEvaluatorContext evaluatorContext = new HaxeExpressionEvaluatorContext(call, null);
                HaxeExpressionEvaluator.evaluate(expression, evaluatorContext, resolver);
                methodResolver.add(paramName, evaluatorContext.result);
              }
            }
          }

          resolver.addAll(methodResolver);
        }
      }
    }
    return resolver;
  }

}
