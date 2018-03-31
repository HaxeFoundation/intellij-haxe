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
package com.intellij.plugins.haxe.ide;

import com.intellij.codeInsight.TargetElementEvaluatorEx2;
import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.openapi.editor.Editor;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeTargetElementEvaluator extends TargetElementEvaluatorEx2 {
  @Nullable
  @Override
  public PsiElement adjustReferenceOrReferencedElement(PsiFile file,
                                                       Editor editor,
                                                       int offset,
                                                       int flags,
                                                       @Nullable PsiElement refElement) {
    PsiReference ref = TargetElementUtil.findReference(editor, offset);
    if (refElement == null && ref != null) {
      refElement = ref.resolve();
    }

    if (ref != null && refElement != null) {
      if (refElement instanceof HaxeClass) {
        boolean isInNewExpression = PsiTreeUtil.getParentOfType(ref.getElement(), HaxeNewExpression.class) != null;
        if (isInNewExpression) {
          HaxeClassModel classModel = ((HaxeClass)refElement).getModel();
          return classModel.hasConstructor() ? classModel.getConstructor().getBasePsi() : null;
        }
      }
    }

    return refElement;
  }

  @Nullable
  @Override
  public PsiElement getElementByReference(@NotNull PsiReference ref, int flags) {
    return ref.resolve();
  }
}
