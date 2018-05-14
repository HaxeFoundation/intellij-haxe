/*
 * Copyright 2018-2018 Ilya Malanin
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
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.lang.psi.HaxePsiField;
import com.intellij.plugins.haxe.lang.psi.HaxePsiModifier.ModifierConstant;
import com.intellij.plugins.haxe.model.HaxeMemberModel;
import com.intellij.plugins.haxe.model.HaxeModelTarget;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeAddModifierFix extends HaxeFixAndIntentionAction {
  private final @ModifierConstant String modifier;

  public HaxeAddModifierFix(HaxePsiField o, @ModifierConstant String modifier) {
    super(o);
    this.modifier = modifier;
  }

  public HaxeAddModifierFix(HaxeMethod o, @ModifierConstant String modifier) {
    super(o);
    this.modifier = modifier;
  }

  @Override
  public void invoke(@NotNull Project project,
                     @NotNull PsiFile file,
                     @Nullable Editor editor,
                     @NotNull PsiElement startElement,
                     @NotNull PsiElement endElement) {

    HaxeMemberModel element = (HaxeMemberModel)((HaxeModelTarget)startElement).getModel();
    element.getModifiers().addModifier(modifier);
  }

  @Override
  public boolean isAvailable(@NotNull Project project,
                             @NotNull PsiFile file,
                             @NotNull PsiElement startElement,
                             @NotNull PsiElement endElement) {

    return startElement instanceof HaxeModelTarget && (startElement instanceof HaxeMethod || startElement instanceof HaxePsiField);
  }

  @NotNull
  @Override
  public String getText() {
    return HaxeBundle.message("haxe.quickfix.add.modifier", modifier);
  }
}
