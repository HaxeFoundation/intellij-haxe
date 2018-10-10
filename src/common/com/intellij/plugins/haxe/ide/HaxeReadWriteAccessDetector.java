/*
 * Copyright 2018 Ilya Malanin
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

import com.intellij.codeInsight.highlighting.ReadWriteAccessDetector;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.util.HaxePsiUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;

public class HaxeReadWriteAccessDetector extends ReadWriteAccessDetector {
  @Override
  public boolean isReadWriteAccessible(@NotNull final PsiElement element) {
    return element instanceof HaxePsiField || element instanceof HaxeClass || element instanceof HaxeMethod;
  }

  @Override
  public boolean isDeclarationWriteAccess(@NotNull final PsiElement element) {
    if (element instanceof HaxePsiField && ((HaxePsiField)element).getVarInit() != null) {
      return true;
    }
    return element instanceof HaxeParameter && ((HaxeParameter)element).getDeclarationScope() instanceof HaxeForStatement;
  }

  @NotNull
  @Override
  public Access getReferenceAccess(@NotNull final PsiElement referencedElement, @NotNull final PsiReference reference) {
    return getExpressionAccess(reference.getElement());
  }

  @NotNull
  @Override
  public Access getExpressionAccess(@NotNull final PsiElement expression) {
    if (!(expression instanceof HaxeExpression)) {
      return Access.Read;
    }
    final HaxeExpression expr = (HaxeExpression)expression;

    boolean readAccess = HaxePsiUtil.isAccessedForReading(expr);
    boolean writeAccess = HaxePsiUtil.isAccessedForWriting(expr);
    if (writeAccess && readAccess) {
      return Access.ReadWrite;
    }
    return writeAccess ? Access.Write : Access.Read;
  }
}
