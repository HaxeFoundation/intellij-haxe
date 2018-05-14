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
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.lang.psi.HaxePsiField;
import com.intellij.plugins.haxe.lang.psi.HaxePsiModifier.ModifierConstant;
import com.intellij.plugins.haxe.model.HaxeMemberModel;
import com.intellij.plugins.haxe.model.HaxeModelTarget;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeDropModifierFix extends HaxeFixAndIntentionAction {
  private final @ModifierConstant String modifier;
  private final String targetName;

  public HaxeDropModifierFix(HaxeNamedComponent o, @ModifierConstant String dropModifier) {
    this(o, dropModifier, null);
  }

  public HaxeDropModifierFix(HaxeNamedComponent o, @ModifierConstant String modifier, @Nullable String targetName) {
    super(o);
    this.modifier = modifier;
    this.targetName = targetName;
  }

  @Override
  public void invoke(@NotNull Project project,
                     @NotNull PsiFile file,
                     @Nullable Editor editor,
                     @NotNull PsiElement startElement,
                     @NotNull PsiElement endElement) {

    HaxeMemberModel element = (HaxeMemberModel)((HaxeModelTarget)startElement).getModel();
    element.getModifiers().removeModifier(modifier);
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
    if (targetName != null) {
      return HaxeBundle.message("haxe.quickfix.drop.modifier.from", modifier, targetName);
    }
    return HaxeBundle.message("haxe.quickfix.drop.modifier", modifier);
  }
}
