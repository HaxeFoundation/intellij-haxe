/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2017 Ilya Malanin
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
package com.intellij.plugins.haxe.ide.annotator;

import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Set;

/**
 * @author: Fedor.Korotkov
 */
public abstract class HaxeAnnotatingVisitor extends HaxeVisitor {
  private static final Set<String> BUILTIN = new THashSet<String>(Arrays.asList(
    "$type", "trace", "__call__", "__vmem_set__", "__vmem_get__", "__vmem_sign__", "__global__", "_global", "__foreach__"
  ));

  @Override
  public void visitReferenceExpression(@NotNull HaxeReferenceExpression reference) {
    if (reference.getTokenType() != HaxeTokenTypes.REFERENCE_EXPRESSION) {
      return; // call, array access, this, literal, etc
    }

    if (isInsidePackageStatement(reference) || isBuiltInMethod(reference)) return;

    checkDeprecatedVarCall(reference);

    if (reference.resolve() == null) {
      handleUnresolvedReference(reference);
    }

    super.visitReferenceExpression(reference);
  }

  private boolean isBuiltInMethod(@NotNull HaxeReferenceExpression reference) {
    return BUILTIN.contains(reference.getReferenceName()) &&
           reference.getParent() instanceof HaxeCallExpression &&
           !(reference.getParent().getParent() instanceof HaxeReference);
  }

  private boolean isInsidePackageStatement(@NotNull HaxeReferenceExpression reference) {
    return PsiTreeUtil.getParentOfType(reference, HaxePackageStatement.class) != null;

  }

  @Override
  public void visitMethodDeclaration(@NotNull HaxeMethodDeclaration functionDeclaration) {
    if (functionDeclaration.hasCompileTimeMetadata(HaxeMeta.DEPRECATED)) {
      handleDeprecatedFunctionDeclaration(functionDeclaration);
    }

    super.visitMethod(functionDeclaration);
  }

  @Override
  public void visitCallExpression(@NotNull HaxeCallExpression o) {
    final PsiElement child = o.getFirstChild();
    if (child instanceof HaxeReferenceExpression referenceExpression) {
      final PsiElement reference = referenceExpression.resolve();

      if (reference instanceof HaxeMethodDeclaration functionDeclaration) {
        if (functionDeclaration.hasCompileTimeMetadata(HaxeMeta.DEPRECATED)) {
          handleDeprecatedCallExpression(referenceExpression);
        }
      }
    }

    super.visitCallExpression(o);
  }

  @Override
  public void visitFieldDeclaration(@NotNull HaxeFieldDeclaration varDeclaration) {
    if (varDeclaration.hasCompileTimeMetadata(HaxeMeta.DEPRECATED)) {
      handleDeprecatedFieldDeclaration(varDeclaration);
    }
    super.visitFieldDeclaration(varDeclaration);
  }

  @Override
  public void visitElement(PsiElement element) {
    ProgressIndicatorProvider.checkCanceled();
    element.acceptChildren(this);
  }

  protected void handleUnresolvedReference(HaxeReferenceExpression reference) {
  }

  protected void handleDeprecatedFunctionDeclaration(HaxeMethodDeclaration functionDeclaration) {
  }

  protected void handleDeprecatedCallExpression(HaxeReferenceExpression referenceExpression) {
  }

  protected void handleDeprecatedFieldDeclaration(HaxeFieldDeclaration varDeclaration) {
  }

  private void checkDeprecatedVarCall(HaxeReferenceExpression referenceExpression) {
    PsiElement reference = referenceExpression.resolve();

    if (reference instanceof HaxeFieldDeclaration varDeclaration) {
      if (varDeclaration.hasCompileTimeMetadata(HaxeMeta.DEPRECATED)) {
        handleDeprecatedCallExpression(referenceExpression);
      }
    }
  }

  private boolean isDeprecatedMeta(@NotNull HaxeMeta meta) {
    return meta.isType(HaxeMeta.DEPRECATED);
  }
}