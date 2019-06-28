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
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeGenericResolverUtil {

  @Nullable
  public static HaxeGenericResolver generateResolverFromScopeParents(PsiElement element) {
    HaxeGenericResolver resolver = new HaxeGenericResolver();
    appendClassGenericResolver(element, resolver);
    appendMethodGenericResolver(element, resolver);
    appendStatementGenericResolver(HaxeResolveUtil.getLeftReference(element), resolver);
    return resolver;
  }

  @Nullable static HaxeGenericResolver appendClassGenericResolver(PsiElement element, @NotNull HaxeGenericResolver resolver) {
    HaxeClass clazz = element instanceof HaxeClass
                      ? (HaxeClass) element
                      : (HaxeClass) UsefulPsiTreeUtil.getParentOfType(element, HaxeClass.class);

    HaxeClassModel classModel = HaxeClassModel.fromElement(clazz);
    if (null != classModel) {
      resolver.addAll(classModel.getGenericResolver(null));
    }

    return resolver;
  }

  @Nullable static HaxeGenericResolver appendMethodGenericResolver(PsiElement element, @NotNull HaxeGenericResolver resolver) {
    HaxeMethod method = (HaxeMethod) UsefulPsiTreeUtil.getParentOfType(element, HaxeMethod.class);
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

}
