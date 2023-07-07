/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2017 Ilya Malanin
 * Copyright 2019-2020 Eric Bishton
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
package com.intellij.plugins.haxe.ide.annotator.color;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import static com.intellij.plugins.haxe.ide.annotator.color.HaxeColorAnnotatorUtil.getAttributeByType;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeSlowColorAnnotator implements Annotator {
  @Override
  public void annotate(@NotNull PsiElement node, @NotNull AnnotationHolder holder) {
    if (node instanceof PsiWhiteSpace) return;


    if (node instanceof HaxeReference reference) {
      final boolean chain = PsiTreeUtil.getChildOfType(node, HaxeReference.class) != null;
      if (chain) {
        if (tryAnnotateQName(node, holder)) return;
      }
      if(!(reference instanceof  HaxeReferenceExpression)
         && !(reference instanceof  HaxePropertyAccessor)
         && !(reference instanceof  HaxeSuperExpression)) {
        // skipping all reference-types that would not result in a highlight, no need waste time  resolving etc.
        return;
      }
      PsiElement element = reference.resolveToComponentName();

      if (element instanceof HaxeComponentName) {
        final boolean isStatic = PsiTreeUtil.getParentOfType(node, HaxeImportStatement.class) == null && checkStatic(element.getParent());
        final TextAttributesKey attribute = getAttributeByType(HaxeComponentType.typeOf(element.getParent()), isStatic);
        if (attribute != null) {
          element = reference.getReferenceNameElement();
          if (element != null) node = element;
          holder.newSilentAnnotation(HighlightSeverity.INFORMATION).range(node).textAttributes(attribute).create();
        }
      }
    }
  }

  private static boolean tryAnnotateQName(PsiElement node, AnnotationHolder holder) {
    // Maybe this is class name
    final HaxeClass resultClass = HaxeResolveUtil.tryResolveClassByQName(node);
    if (resultClass != null) {
      final TextAttributesKey attribute = getAttributeByType(HaxeComponentType.typeOf(resultClass), false);
      if (attribute != null) {
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION).textAttributes(attribute).create();
      }
      return true;
    }
    return false;
  }

  private static boolean checkStatic(PsiElement parent) {
    return parent instanceof HaxeNamedComponent && ((HaxeNamedComponent)parent).isStatic();
  }

}
