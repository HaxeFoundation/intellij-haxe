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

import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.ide.actions.HaxeStaticMemberAddImportIntentionAction;
import com.intellij.plugins.haxe.ide.actions.HaxeTypeAddImportIntentionAction;
import com.intellij.plugins.haxe.lang.psi.stubs.index.HaxeClassNameStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.HaxeFieldNameStubIndex;
import com.intellij.plugins.haxe.lang.psi.stubs.index.HaxeMethodNameStubIndex;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.metadata.psi.HaxeMetadataCompileTimeMeta;
import com.intellij.plugins.haxe.metadata.psi.impl.HaxeMetadataTypeName;
import com.intellij.plugins.haxe.model.HaxeBaseMemberModel;
import com.intellij.plugins.haxe.model.HaxeMemberModel;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeUnresolvedTypeAnnotator extends HaxeVisitor implements Annotator {
  private AnnotationHolder myHolder = null;
  private List<TextRange> annotatedRanges = new ArrayList<>();

  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if(!element.isValid()) return;

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
    List<HaxeMemberModel> members = new ArrayList<>();
    List<HaxeComponent> classes = new ArrayList<>(HaxeClassNameStubIndex.getByName(expression.getText(), expression.getProject(), scope));
    if (expression.getParent() instanceof HaxeCallExpression) {
      members.addAll(findStaticMembers(expression.getText(), expression.getProject(), scope, true, false));
    } else {
      members.addAll(findStaticMembers(expression.getText(), expression.getProject(), scope, false, true));
    }

    boolean classesFound = !classes.isEmpty();
    boolean membersFound = !members.isEmpty();

    if (classesFound || membersFound) {
      // operator overload metas don't have "real" references so we skip this check
      if (isCompileTimeMeta(expression, HaxeMeta.OP)) return;
      TextRange textRange = expression.getTextRange();
      if (!annotatedRanges.contains(textRange)) {
        annotatedRanges.add(textRange);
        AnnotationBuilder annotationBuilder = myHolder.newAnnotation(HighlightSeverity.ERROR, HaxeBundle.message("haxe.unresolved.type")).range(expression);

        if(classesFound) {
          annotationBuilder.withFix(new HaxeTypeAddImportIntentionAction(expression, classes));
        }
        if(membersFound) {
          annotationBuilder.withFix(new HaxeStaticMemberAddImportIntentionAction(expression, members));
        }

        annotationBuilder.create();
      }
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

  /**
   * Finds static public members (methods and/or fields/enum-values) with the given name using stub indexes.
   * No file re-parsing required — stub data is used for static/public flag checks.
   */
  private static List<HaxeMemberModel> findStaticMembers(@NotNull String memberName,
                                                          @NotNull com.intellij.openapi.project.Project project,
                                                          @NotNull GlobalSearchScope scope,
                                                          boolean includeMethods,
                                                          boolean includeFields) {
    List<HaxeMemberModel> results = new ArrayList<>();
    if (includeMethods) {
      for (HaxeMethod method : StubIndex.getElements(HaxeMethodNameStubIndex.KEY, memberName, project, scope, HaxeMethod.class)) {
        if (method.isStatic() && method.isPublic()) {
          results.add(method.getModel());
        }
      }
    }
    if (includeFields) {
      for (HaxePsiField field : StubIndex.getElements(HaxeFieldNameStubIndex.KEY, memberName, project, scope, HaxePsiField.class)) {
        if (field.isStatic() && field.isPublic()) {
          HaxeBaseMemberModel base = field.getModel();
          if (base instanceof HaxeMemberModel memberModel) {
            results.add(memberModel);
          }
        }
      }
    }
    return results;
  }

}
