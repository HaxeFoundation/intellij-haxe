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
import com.intellij.plugins.haxe.lang.psi.HaxeImplementsDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeType;
import com.intellij.plugins.haxe.model.HaxeDocumentModel;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeChangeImplementsToExtendsFix extends HaxeFixAndIntentionAction {
  public HaxeChangeImplementsToExtendsFix(@Nullable PsiElement element) {
    super(element);
  }

  @Override
  public void invoke(@NotNull Project project,
                     @NotNull PsiFile file,
                     @Nullable Editor editor,
                     @NotNull PsiElement startElement,
                     @NotNull PsiElement endElement) {
    HaxeImplementsDeclaration declaration = (HaxeImplementsDeclaration)startElement;
    HaxeType type = declaration.getType();
    String replacement = "extends";
    if (type != null) {
      replacement += " " + type.getText();
    }
    HaxeDocumentModel.fromElement(startElement).replaceElementText(declaration, replacement);
  }

  @Override
  public boolean isAvailable(@NotNull Project project,
                             @NotNull PsiFile file,
                             @NotNull PsiElement startElement,
                             @NotNull PsiElement endElement) {
    return startElement instanceof HaxeImplementsDeclaration;
  }

  @NotNull
  @Override
  public String getText() {
    return HaxeBundle.message("haxe.quickfix.change.implements.to.extends", getStartElement().getText());
  }
}
