/*
 * Copyright 2017-2017 Ilya Malanin
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

import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.ide.generation.GetterSetterMethodBuilder;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeFieldModel;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiParserFacade;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CreateGetterSetterQuickfix extends BaseIntentionAction {
  private final HaxeClassModel classModel;
  private final HaxeFieldModel field;
  private final Boolean generateGetter;

  public CreateGetterSetterQuickfix(HaxeClassModel classModel, HaxeFieldModel field, Boolean generateGetter) {
    this.classModel = classModel;
    this.field = field;
    this.generateGetter = generateGetter;
  }

  @NotNull
  @Override
  public String getText() {
    return generateGetter ? "Generate getter" : "Generate setter";
  }

  @Nls
  @NotNull
  @Override
  public String getFamilyName() {
    return getText();
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    return true;
  }

  @Override
  public void invoke(@NotNull final Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    ApplicationManager.getApplication().invokeLater(() -> {
        if (classModel.getBodyPsi() == null) return;

        final StringBuilder builder = new StringBuilder();
        GetterSetterMethodBuilder.build(builder, field, generateGetter);

        WriteCommandAction.writeCommandAction(project).run(() ->{
            List<HaxeNamedComponent> elements =
              HaxeElementGenerator.createNamedSubComponentsFromText(project, builder.toString());

            PsiElement body = classModel.getBodyPsi();
            PsiElement anchor = body.getLastChild().getPrevSibling();

            for (PsiElement element : elements) {
              PsiElement newLine =  PsiParserFacade.getInstance(project).createWhiteSpaceFromText("\n\n");
              anchor = body.addAfter(element, anchor);
              anchor = body.addBefore(newLine, anchor);
            }
        });
      }
    );
  }
}
