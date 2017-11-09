/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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
package com.intellij.plugins.haxe.ide.intention;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeImportStatement;
import com.intellij.plugins.haxe.model.HaxeModel;
import com.intellij.plugins.haxe.model.HaxeModelTarget;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.plugins.haxe.util.HaxeImportUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by as3boyan on 04.10.14.
 */
public class ReplaceImportStatementWithWildcardWithSingleClassImports implements IntentionAction {
  @NotNull
  @Override
  public String getText() {
    return "Replace with single class imports";
  }

  @NotNull
  @Override
  public String getFamilyName() {
    return getText();
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    PsiElement elementAt = file.findElementAt(editor.getCaretModel().getOffset());
    HaxeImportStatement importStatement = PsiTreeUtil.getParentOfType(elementAt, HaxeImportStatement.class);

    return importStatement != null &&
           importStatement.getWildcard() != null &&
           HaxeImportUtil.getExternalReferences(file).stream()
             .anyMatch(element -> HaxeImportUtil.isStatementExposesReference(importStatement, element));
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    PsiElement elementAt = file.findElementAt(editor.getCaretModel().getOffset());
    HaxeImportStatement importStatement = PsiTreeUtil.getParentOfType(elementAt, HaxeImportStatement.class);

    if (importStatement == null || importStatement.getWildcard() == null) return;

    List<PsiElement> newImports = HaxeImportUtil.getExternalReferences(file).stream()
      .map(element -> HaxeImportUtil.exposeReference(importStatement, element)).filter(Objects::nonNull).distinct()
      .collect(Collectors.toList());

    newImports.forEach(elementToImport -> {
      if (elementToImport instanceof HaxeModelTarget) {
        HaxeModel model = ((HaxeModelTarget)elementToImport).getModel();
        HaxeImportStatement newImportStatement =
          HaxeElementGenerator.createImportStatementFromPath(project, model.getQualifiedInfo().getPresentableText());
        if (newImportStatement != null) {
          file.addBefore(newImportStatement, importStatement);
        }
      }
    });

    importStatement.delete();
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }
}
