/*
 * Copyright 2020 Eric Bishton
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
package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class HaxeParenthesizedExpressionReferenceImpl extends HaxeReferenceImpl implements HaxeParenthesizedExpressionReference {
  PsiElement expression;

  public HaxeParenthesizedExpressionReferenceImpl(PsiElement element) {
    super(element.getNode());
    expression = element;
  }

  @NotNull
  @Override
  public HaxeIdentifier getIdentifier() {
    return new HaxeIdentifierImpl(new HaxeDummyASTNode("Parenthesized Expression", HaxeParenthesizedExpressionReferenceImpl.this.getProject())) {
      @NotNull
      @Override
      public Project getProject() {
        return ((HaxeDummyASTNode)getNode()).getProject();
      }
    };
  }

  @Override
  protected List<? extends PsiElement> doResolve(@NotNull HaxeReference reference, boolean incompleteCode) {
    HaxeResolveResult resolved = getResolvedType();
    if (null != resolved.getHaxeClass()) {
      return Collections.singletonList(resolved.getHaxeClass());
    }
    return Collections.emptyList();
  }

  @NotNull
  public HaxeResolveResult getResolvedType() {
    HaxeExpressionEvaluatorContext context =
      HaxeExpressionEvaluator.evaluate(expression, new HaxeExpressionEvaluatorContext(expression),
                                       HaxeGenericResolverUtil.generateResolverFromScopeParents(expression));

    ResultHolder result = context.result;
    SpecificHaxeClassReference specificReference = result.getClassType();
    if (null != specificReference) {
      HaxeClass clazz = specificReference.getHaxeClass();
      HaxeResolveResult resolved = HaxeResolveResult.create(clazz, specificReference.getGenericResolver().getSpecialization(clazz));
      return resolved;
    }
    return HaxeResolveResult.EMPTY;
  }
}
