/*
 * Copyright 2017-2017 Ilya Malanin
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
package com.intellij.plugins.haxe.ide.info;

import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;

import java.util.List;

class HaxeFunctionDescriptionBuilder {
  @Nullable
  static HaxeFunctionDescription buildForMethod(HaxeCallExpression expression) {
    final HaxeGenericSpecialization specialization = expression.getSpecialization();
    final boolean isStaticExtension = expression.resolveIsStaticExtension();

    final HaxeReference reference = (HaxeReference)expression.getExpression();
    final PsiElement target = reference.resolve();

    if (target instanceof HaxeMethod) {
      final HaxeClass haxeClass = (HaxeClass)((HaxeMethod)target).getContainingClass();
      final HaxeClassResolveResult resolveResult = HaxeClassResolveResult.create(haxeClass, specialization);
      return build((HaxeMethod)target, resolveResult, isStaticExtension);
    }
    return null;
  }

  @Nullable
  static HaxeFunctionDescription buildForConstructor(final HaxeNewExpression expression) {
    final HaxeGenericSpecialization specialization = expression.getSpecialization();
    final HaxeClass haxeClass = (HaxeClass)expression.getType().getReferenceExpression().resolve();

    if (haxeClass != null) {
      final PsiMethod[] constructors = haxeClass.getConstructors();
      if (constructors.length > 0) {
        final HaxeClassResolveResult resolveResult = HaxeClassResolveResult.create(haxeClass, specialization);

        final HaxeMethod constructor = (HaxeMethod)constructors[0];
        return build(constructor, resolveResult, false);
      }
    }

    return null;
  }

  private static HaxeFunctionDescription build(HaxeMethod method,
                                               HaxeClassResolveResult resolveResult,
                                               boolean isExtension) {

    HaxeParameterDescription[] parameterDescriptions = null;

    final HaxeParameterList parameterList = PsiTreeUtil.getChildOfType(method, HaxeParameterList.class);
    if (parameterList != null) {
      List<HaxeParameter> list = parameterList.getParameterList();
      if (isExtension) {
        list.remove(0);
      }
      parameterDescriptions = HaxeParameterDescriptionBuilder.buildFromList(list, resolveResult);
    }

    return new HaxeFunctionDescription(parameterDescriptions);
  }
}
