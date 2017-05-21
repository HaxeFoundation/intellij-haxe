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
import com.intellij.plugins.haxe.util.HaxePresentableUtil;
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

    final PsiElement target = ((HaxeReference)expression.getExpression()).resolve();

    if (target instanceof HaxeMethod) {
      final HaxeClass targetParent = (HaxeClass)((HaxeMethod)target).getContainingClass();
      final HaxeClassResolveResult resolveResult = HaxeClassResolveResult.create(targetParent, specialization);

      return build((HaxeMethod)target, resolveResult, specialization, isStaticExtension);
    }
    return null;
  }

  @Nullable
  static HaxeFunctionDescription buildForConstructor(final HaxeNewExpression expression) {
    final HaxeGenericSpecialization specialization = expression.getSpecialization();
    final HaxeClass haxeClass = (HaxeClassDeclaration)expression.getType().getReferenceExpression().resolve();

    if (haxeClass != null) {
      final PsiMethod[] constructors = haxeClass.getConstructors();
      if (constructors.length > 0) {
        final HaxeFunctionDeclarationWithAttributes constructor = (HaxeFunctionDeclarationWithAttributes)constructors[0];
        final HaxeClassResolveResult resolveResult = HaxeClassResolveResult.create(haxeClass, specialization);

        return build(constructor, resolveResult, specialization, false);
      }
    }

    return null;
  }

  private static HaxeFunctionDescription build(HaxeMethod method, HaxeClassResolveResult resolveResult, HaxeGenericSpecialization specialization, boolean isExtension) {
    String returnType = null;
    HaxeParameterDescription[] parameterDescriptions;

    final HaxeTypeTag typeTag = PsiTreeUtil.getChildOfType(method, HaxeTypeTag.class);
    if (typeTag != null) {
      returnType = HaxePresentableUtil.buildTypeText(method, typeTag, resolveResult.getSpecialization());
    }

    final HaxeParameterList parameterList = PsiTreeUtil.getChildOfType(method, HaxeParameterList.class);
    if (parameterList != null) {
      List<HaxeParameter> list = parameterList.getParameterList();
      if (isExtension) {
        list.remove(0);
      }

      parameterDescriptions = HaxeParameterDescriptionBuilder.buildFromList(list, specialization);
    } else {
      parameterDescriptions = new HaxeParameterDescription[0];
    }

    return new HaxeFunctionDescription(
      method.getName(),
      returnType,
      parameterDescriptions
    );
  }
}