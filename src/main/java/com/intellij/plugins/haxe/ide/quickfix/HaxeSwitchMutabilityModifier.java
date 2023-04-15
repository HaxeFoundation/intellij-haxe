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
package com.intellij.plugins.haxe.ide.quickfix;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeFieldDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeMethodDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxePsiModifier;
import com.intellij.plugins.haxe.model.HaxeDocumentModel;
import com.intellij.plugins.haxe.model.HaxeMemberModel;
import com.intellij.plugins.haxe.model.HaxeModelTarget;
import com.intellij.plugins.haxe.model.HaxeModifiersModel;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeSwitchMutabilityModifier extends HaxeFixAndIntentionAction {
  public HaxeSwitchMutabilityModifier(@Nullable HaxeFieldDeclaration element) {
    super(element);
  }

  public HaxeSwitchMutabilityModifier(@Nullable HaxeMethodDeclaration element) {
    super(element);
  }

  @Override
  public void invoke(@NotNull Project project,
                     @NotNull PsiFile file,
                     @Nullable Editor editor,
                     @NotNull PsiElement startElement,
                     @NotNull PsiElement endElement) {
    final HaxeDocumentModel document = HaxeDocumentModel.fromElement(startElement);
    final HaxeMemberModel model = (HaxeMemberModel)((HaxeModelTarget)startElement).getModel();

    if (startElement instanceof HaxeFieldDeclaration) {
      final HaxeFieldDeclaration field = (HaxeFieldDeclaration)startElement;
      document.replaceElementText(field.getMutabilityModifier(), model.isFinal() ? HaxePsiModifier.VAR : HaxePsiModifier.FINAL);
    } else {
      final HaxeModifiersModel modifiers = model.getModifiers();
      if (model.isFinal()) {
        modifiers.removeModifier(HaxePsiModifier.FINAL);
      } else {
        modifiers.addModifier(HaxePsiModifier.FINAL);
      }
    }
  }

  @Override
  public boolean isAvailable(@NotNull Project project,
                             @NotNull PsiFile file,
                             @NotNull PsiElement startElement,
                             @NotNull PsiElement endElement) {
    return startElement instanceof HaxeFieldDeclaration || startElement instanceof HaxeMethodDeclaration;
  }

  @NotNull
  @Override
  public String getText() {
    PsiElement element = getStartElement();
    String memberType;
    final HaxeMemberModel model = (HaxeMemberModel)((HaxeModelTarget)element).getModel();
    if (element instanceof HaxeFieldDeclaration) {
      HaxeFieldDeclaration field = (HaxeFieldDeclaration)getStartElement();
      final boolean isProperty = field.getPropertyDeclaration() != null;
      memberType = isProperty ? "property" : "field";
    } else {
      memberType = "method";
    }

    if (model.isFinal()) {
      return HaxeBundle.message("haxe.quickfix.make.mutable", memberType, model.getName());
    } else {
      return HaxeBundle.message("haxe.quickfix.make.immutable", memberType, model.getName());
    }
  }
}
