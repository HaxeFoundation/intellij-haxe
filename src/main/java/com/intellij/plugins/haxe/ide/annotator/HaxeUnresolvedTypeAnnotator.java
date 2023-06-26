/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2019 Eric Bishton
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

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.Key;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.actions.HaxeTypeAddImportIntentionAction;
import com.intellij.plugins.haxe.ide.index.HaxeComponentIndex;
import com.intellij.plugins.haxe.lang.psi.HaxeComponent;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeType;
import com.intellij.plugins.haxe.lang.psi.HaxeVisitor;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.psi.HaxeMetadataCompileTimeMeta;
import com.intellij.plugins.haxe.metadata.psi.impl.HaxeMetadataTypeName;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeUnresolvedTypeAnnotator extends HaxeVisitor implements Annotator {
  private static final AnnotatorTracker ANNOTATOR_TRACKER = new AnnotatorTracker("AnnotatorTracker");
  private AnnotationHolder myHolder = null;

  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (element instanceof HaxeType || element instanceof HaxeReferenceExpression) {
      assert myHolder == null;
      try {
        myHolder = holder;
        element.accept(this);
      }
      finally {
        myHolder = null;
      }
    }
  }

  @Override
  public void visitType(@NotNull HaxeType type) {
    super.visitType(type);
    final HaxeReferenceExpression expression = type.getReferenceExpression();
    if (expression.resolve() != null) {
      return;
    }

    tryCreateAnnotation(expression);
  }

  @Override
  public void visitReferenceExpression(@NotNull HaxeReferenceExpression expression) {
    super.visitReferenceExpression(expression);

    if (expression.resolve() == null) {
      tryCreateAnnotation(expression);
    }
  }

  private void tryCreateAnnotation(HaxeReferenceExpression expression) {
    final GlobalSearchScope scope = HaxeResolveUtil.getScopeForElement(expression);
    final List<HaxeComponent> components =
      HaxeComponentIndex.getItemsByName(expression.getText(), expression.getProject(), scope);
    if (!components.isEmpty()) {
      // operator overload metas don't have "real" references so we skip this check
      if (isCompileTimeMeta(expression, HaxeMeta.OP)) return;
      expression.putUserData(ANNOTATOR_TRACKER, HighlightSeverity.ERROR);
      myHolder.newAnnotation(HighlightSeverity.ERROR, HaxeBundle.message("haxe.unresolved.type"))
        .withFix(new HaxeTypeAddImportIntentionAction(expression, components))
        .create();
    }
  }

  private boolean isCompileTimeMeta(HaxeReferenceExpression expression, HaxeMetadataTypeName metadataTypeName) {
    HaxeMetadataCompileTimeMeta type = PsiTreeUtil.getParentOfType(expression, HaxeMetadataCompileTimeMeta.class);
    if (type != null) {
      if (type.isType(metadataTypeName)) {
        return true;
      }
    }
    return false;
  }

  private static class AnnotatorTracker extends Key<HighlightSeverity> {
    public AnnotatorTracker(@NonNls @NotNull String name) {
      super(name);
    }
  }
}
