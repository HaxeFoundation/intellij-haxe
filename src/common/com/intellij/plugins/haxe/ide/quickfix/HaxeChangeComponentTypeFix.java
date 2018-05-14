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
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeDocumentModel;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeChangeComponentTypeFix extends HaxeFixAndIntentionAction {
  private static final String UNDEFINED = "<undefined>";

  private static final String VARIABLE = "variable";
  private static final String PARAMETER = "parameter";

  private final ResultHolder toType;
  private final ResultHolder fromType;

  public HaxeChangeComponentTypeFix(HaxeTypeTag typeTag, ResultHolder fromType, ResultHolder toType) {
    super(typeTag);

    this.toType = toType;
    this.fromType = fromType;
  }

  @Override
  public void invoke(@NotNull Project project,
                     @NotNull PsiFile file,
                     @Nullable Editor editor,
                     @NotNull PsiElement startElement,
                     @NotNull PsiElement endElement) {

    HaxeDocumentModel.fromElement(startElement)
      .replaceElementText(startElement, ':' + toType.toStringWithoutConstant());
  }

  @NotNull
  @Override
  public String getText() {
    String componentKind = VARIABLE;
    String componentName = UNDEFINED;

    if (myStartElement.getElement() != null) {
      PsiElement parent = myStartElement.getElement().getParent();
      if (parent instanceof HaxeComponent) {
        HaxeComponentName componentNamePsi = ((HaxeComponent)parent).getComponentName();
        if (componentNamePsi != null) {
          componentName = componentNamePsi.getText();
        }
      }

      if (parent instanceof HaxeParameter) {
        componentKind = PARAMETER;
      }
    }

    return HaxeBundle.message("haxe.quickfix.change.type", componentKind, componentName,
                              fromType.toStringWithoutConstant(),
                              toType.toStringWithoutConstant());
  }
}