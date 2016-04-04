/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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
import com.intellij.plugins.haxe.model.HaxeModifierType;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.util.PsiTreeUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * @author: Fedor.Korotkov
 */
public abstract class HaxeAnnotatingVisitor extends HaxeVisitor {
  private static final Set<String> BUILTIN = new THashSet<String>(Arrays.asList(
    "trace", "__call__", "__vmem_set__", "__vmem_get__", "__vmem_sign__", "__global__", "_global", "__foreach__"
  ));

  @Override
  public void visitReferenceExpression(@NotNull HaxeReferenceExpression reference) {
    if (reference.getTokenType() != HaxeTokenTypes.REFERENCE_EXPRESSION) {
      return; // call, array access, this, literal, etc
    }
    final HaxeReference[] childReferences = PsiTreeUtil.getChildrenOfType(reference, HaxeReference.class);
    if (childReferences != null && childReferences.length > 1) {
      checkDeprecatedVarCall(reference);
      super.visitReferenceExpression(reference);
      return;
    }
    final HaxeReference leftSiblingReference = HaxeResolveUtil.getLeftReference(reference);
    final PsiElement referenceTarget = reference.resolve();
    if (referenceTarget != null) {
      return; // OK
    }

    if (BUILTIN.contains(reference.getText()) &&
        reference.getParent() instanceof HaxeCallExpression &&
        !(reference.getParent().getParent() instanceof HaxeReference)) {
      return;
    }

    if (!(reference.getParent() instanceof HaxeReference) && !(reference.getParent() instanceof HaxePackageStatement) && !(reference.getParent() instanceof HaxeImportStatementWithWildcard)) {
      // whole reference expression
      handleUnresolvedReference(reference);
    }
    final PsiElement leftSiblingReferenceTarget = leftSiblingReference == null ? null : leftSiblingReference.resolve();
    if (leftSiblingReference != null && leftSiblingReferenceTarget == null) {
      return; // already bad
    }

    // check all parents (ex. com.reference.Bar)
    PsiElement parent = reference.getParent();
    while (parent instanceof HaxeReference) {
      if (((HaxeReference)parent).resolve() != null) return;
      parent = parent.getParent();
    }

    if (parent instanceof HaxePackageStatement) {
      return; // package
    }

    if (parent instanceof HaxeImportStatementWithWildcard) {
      return;
    }

    handleUnresolvedReference(reference);
  }

  @Override
  public void visitFunctionDeclarationWithAttributes(@NotNull HaxeFunctionDeclarationWithAttributes functionDeclaration) {
    List<HaxeCustomMeta> metas = functionDeclaration.getCustomMetaList();
    for (HaxeCustomMeta meta : metas) {
      if (HaxeModifierType.DEPRECATED.s.equals(meta.getText())) {
        handleDeprecatedFunctionDeclaration(functionDeclaration);
      }
    }

    super.visitFunctionDeclarationWithAttributes(functionDeclaration);
  }

  @Override
  public void visitCallExpression(@NotNull HaxeCallExpression o) {
    PsiElement child = o.getFirstChild();
    if (child instanceof HaxeReferenceExpression) {
      HaxeReferenceExpression referenceExpression = (HaxeReferenceExpression) child;
      PsiElement reference = referenceExpression.resolve();

      if (reference instanceof HaxeFunctionDeclarationWithAttributes) {
        HaxeFunctionDeclarationWithAttributes functionDeclaration = (HaxeFunctionDeclarationWithAttributes)reference;

        List<HaxeCustomMeta> metas = functionDeclaration.getCustomMetaList();
        for (HaxeCustomMeta meta : metas) {
          if (HaxeModifierType.DEPRECATED.s.equals(meta.getText())) {
            handleDeprecatedCallExpression(referenceExpression);
          }
        }
      }
    }

    super.visitCallExpression(o);
  }

  @Override
  public void visitVarDeclaration(@NotNull HaxeVarDeclaration varDeclaration) {
    List<HaxeCustomMeta> metas = varDeclaration.getCustomMetaList();
    for (HaxeCustomMeta meta : metas) {
      if (HaxeModifierType.DEPRECATED.s.equals(meta.getText())) {
        handleDeprecatedVarDeclaration(varDeclaration);
      }
    }

    super.visitVarDeclaration(varDeclaration);
  }

  @Override
  public void visitElement(PsiElement element) {
    ProgressIndicatorProvider.checkCanceled();
    element.acceptChildren(this);
  }

  protected void handleUnresolvedReference(HaxeReferenceExpression reference) {}
  protected void handleDeprecatedFunctionDeclaration(HaxeFunctionDeclarationWithAttributes functionDeclaration) {}
  protected void handleDeprecatedCallExpression(HaxeReferenceExpression referenceExpression) {}
  protected void handleDeprecatedVarDeclaration(HaxeVarDeclaration varDeclaration) {}

  private void checkDeprecatedVarCall(HaxeReferenceExpression referenceExpression) {
    PsiElement reference = referenceExpression.resolve();

    if (reference instanceof HaxeVarDeclarationPart) {
      HaxeVarDeclaration varDeclaration = (HaxeVarDeclaration) reference.getParent();

      List<HaxeCustomMeta> metas = varDeclaration.getCustomMetaList();
      for (HaxeCustomMeta meta : metas) {
        if (HaxeModifierType.DEPRECATED.s.equals(meta.getText())) {
          handleDeprecatedCallExpression(referenceExpression);
        }
      }
    }
  }
}