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
import com.intellij.plugins.haxe.ide.generation.GetterSetterMethodBuilder;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.lang.psi.HaxePropertyAccessor;
import com.intellij.plugins.haxe.lang.psi.HaxePsiCompositeElement;
import com.intellij.plugins.haxe.lang.psi.HaxePsiField;
import com.intellij.plugins.haxe.model.HaxeAccessorType;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeFieldModel;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HaxeCreateAccessorMethodFix extends HaxeFixAndIntentionAction {
  public HaxeCreateAccessorMethodFix(HaxePsiField field, HaxePropertyAccessor accessor) {
    super(field, accessor);
  }

  @Override
  public void invoke(@NotNull Project project,
                     @NotNull PsiFile file,
                     @Nullable Editor editor,
                     @NotNull PsiElement startElement,
                     @NotNull PsiElement endElement) {

    HaxePsiField field = (HaxePsiField)startElement;
    HaxeFieldModel model = (HaxeFieldModel)field.getModel();
    HaxeClassModel declaringClass = model.getDeclaringClass();
    HaxeAccessorType accessorType = HaxeAccessorType.fromPsi(getEndElement());

    HaxePsiCompositeElement classBody = declaringClass != null ? declaringClass.getBodyPsi() : null;
    if (classBody != null) {
      final StringBuilder builder = new StringBuilder();
      GetterSetterMethodBuilder.build(builder, model, accessorType == HaxeAccessorType.GET);

      List<HaxeNamedComponent> elements =
        HaxeElementGenerator.createNamedSubComponentsFromText(project, builder.toString());

      PsiElement anchor = classBody.getLastChild();

      for (PsiElement element : elements) {
        PsiElement newLine = HaxeElementGenerator.createNewLine(project);
        anchor = classBody.addAfter(element, anchor);
        anchor = classBody.addBefore(newLine, anchor);
      }
    }
  }

  @NotNull
  @Override
  public String getText() {
    String fieldName = ((HaxePsiField)getStartElement()).getName();
    String accessorTypeName = HaxeAccessorType.fromPsi(getEndElement()).text;
    return HaxeBundle.message("haxe.quickfix.create.accessor.method", fieldName, accessorTypeName);
  }
}
