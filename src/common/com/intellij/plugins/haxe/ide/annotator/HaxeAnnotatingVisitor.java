/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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
package com.intellij.plugins.haxe.ide.annotator;

import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeModifierType;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
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
    if (reference.getFirstChild() instanceof HaxeReference) {
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

    if (reference.getFirstChild() instanceof HaxeParenthesizedExpression) {
      HaxeParenthesizedExpression parenthesizedExpression = (HaxeParenthesizedExpression)reference.getFirstChild();
      if (PsiTreeUtil.getChildOfType(parenthesizedExpression, HaxeTypeCheckExpr.class) != null) {
        super.visitReferenceExpression(reference);
        return;
      }
    }

    if (!(reference.getParent() instanceof HaxeReference) &&
        !(reference.getParent() instanceof HaxePackageStatement) &&
        !(reference.getParent() instanceof HaxeImportStatement && ((HaxeImportStatement)reference.getParent()).getWildcard() != null)) {
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

    if (parent instanceof HaxeImportStatement && ((HaxeImportStatement)parent).getWildcard() != null) {
      return;
    }

    handleUnresolvedReference(reference);
  }

  @Override
  public void visitFunctionDeclarationWithAttributes(@NotNull HaxeFunctionDeclarationWithAttributes functionDeclaration) {
    List<HaxeCustomMeta> metas = functionDeclaration.getCustomMetaList();
    for (HaxeCustomMeta meta : metas) {
      if (isDeprecatedMeta(meta)) {
        handleDeprecatedFunctionDeclaration(functionDeclaration);
      }
    }

    super.visitFunctionDeclarationWithAttributes(functionDeclaration);
  }

  @Override
  public void visitCallExpression(@NotNull HaxeCallExpression o) {
    final PsiElement child = o.getFirstChild();
    if (child instanceof HaxeReferenceExpression) {
      HaxeReferenceExpression referenceExpression = (HaxeReferenceExpression)child;
      final PsiElement reference = referenceExpression.resolve();

      if (reference instanceof HaxeFunctionDeclarationWithAttributes) {
        final HaxeFunctionDeclarationWithAttributes functionDeclaration = (HaxeFunctionDeclarationWithAttributes)reference;
        final List<HaxeCustomMeta> metas = functionDeclaration.getCustomMetaList();
        for (HaxeCustomMeta meta : metas) {
          if (isDeprecatedMeta(meta)) {
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
      if (isDeprecatedMeta(meta)) {
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

  protected void handleUnresolvedReference(HaxeReferenceExpression reference) {
  }

  protected void handleDeprecatedFunctionDeclaration(HaxeFunctionDeclarationWithAttributes functionDeclaration) {
  }

  protected void handleDeprecatedCallExpression(HaxeReferenceExpression referenceExpression) {
  }

  protected void handleDeprecatedVarDeclaration(HaxeVarDeclaration varDeclaration) {
  }

  private void checkDeprecatedVarCall(HaxeReferenceExpression referenceExpression) {
    PsiElement reference = referenceExpression.resolve();

    if (reference instanceof HaxeVarDeclaration) {
      HaxeVarDeclaration varDeclaration = (HaxeVarDeclaration)reference;

      List<HaxeCustomMeta> metas = varDeclaration.getCustomMetaList();
      for (HaxeCustomMeta meta : metas) {
        if (isDeprecatedMeta(meta)) {
          handleDeprecatedCallExpression(referenceExpression);
        }
      }
    }
  }

  private boolean isDeprecatedMeta(@NotNull HaxeCustomMeta meta) {
    String metaText = meta.getText();
    return metaText != null && metaText.startsWith(HaxeModifierType.DEPRECATED.s);
  }
}