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
package com.intellij.plugins.haxe.ide.completion;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import com.intellij.plugins.haxe.model.HaxeExposableModel;
import com.intellij.plugins.haxe.model.HaxeModel;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class HaxeLookupElementFactory {
  public static LookupElementBuilder create(@NotNull HaxeModel model) {
    return create(model, null);
  }

  @Nullable
  public static LookupElementBuilder create(@NotNull HaxeModel model, @Nullable String alias) {
    PsiElement basePsi = model.getBasePsi();
    HaxeNamedComponent namedComponent = getNamedComponent(basePsi);

    if (namedComponent == null) return null;

    String name = StringUtil.defaultIfEmpty(alias, model.getName());
    String presentableText = null;
    String tailText = getParentPath(model);
    Icon icon = null;

    ItemPresentation presentation = namedComponent.getPresentation();
    if (presentation != null) {
      icon = presentation.getIcon(false);
      presentableText = presentation.getPresentableText();
    }

    LookupElementBuilder lookupElement = LookupElementBuilder.create(basePsi, name);

    if (presentableText != null) {
      if (alias != null && presentableText.startsWith(model.getName())) {
        presentableText = presentableText.replace(model.getName(), alias);
      }
      lookupElement = lookupElement.withPresentableText(presentableText);
    }

    if (icon != null) lookupElement = lookupElement.withIcon(icon);

    if (tailText != null) {
      if (alias != null) {
        tailText = HaxeBundle.message("haxe.lookup.alias", tailText + "." + model.getName());
      }
      tailText = " " + tailText;
      lookupElement = lookupElement.withTailText(tailText, true);
    }

    return lookupElement;
  }

  @Nullable
  private static HaxeNamedComponent getNamedComponent(PsiElement element) {
    return element instanceof HaxeNamedComponent
           ? (HaxeNamedComponent)element
           : PsiTreeUtil.findChildOfType(element, HaxeNamedComponent.class);
  }

  @Nullable
  private static String getParentPath(@NotNull HaxeModel model) {
    final HaxeExposableModel parent = model.getExhibitor();
    if (parent != null) {
      final FullyQualifiedInfo qualifiedInfo = parent.getQualifiedInfo();
      if (qualifiedInfo != null) {
        return qualifiedInfo.getPresentableText();
      }
    }

    return null;
  }
}
