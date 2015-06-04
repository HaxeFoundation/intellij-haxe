/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Created by as3boyan on 13.11.14.
 */
public class SplitIntoDeclarationAndAssignment implements IntentionAction {
  @NotNull
  @Override
  public String getText() {
    return "Split into declaration and assignment";
  }

  @NotNull
  @Override
  public String getFamilyName() {
    return getText();
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    PsiElement elementAt = file.findElementAt(editor.getCaretModel().getOffset());
    HaxeLocalVarDeclaration localVarDeclaration = PsiTreeUtil.getParentOfType(elementAt, HaxeLocalVarDeclaration.class);
    if (localVarDeclaration != null) {
      List<HaxeLocalVarDeclarationPart> localVarDeclarationPartList = localVarDeclaration.getLocalVarDeclarationPartList();
      if (localVarDeclarationPartList.size() == 1) {
        HaxeLocalVarDeclarationPart localVarDeclarationPart = localVarDeclarationPartList.get(0);
        HaxeVarInit varInit = localVarDeclarationPart.getVarInit();
        if (localVarDeclarationPart.getPropertyDeclaration() == null && varInit != null) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    PsiElement elementAt = file.findElementAt(editor.getCaretModel().getOffset());
    HaxeLocalVarDeclaration localVarDeclaration = PsiTreeUtil.getParentOfType(elementAt, HaxeLocalVarDeclaration.class);
    HaxeLocalVarDeclarationPart localVarDeclarationPart = localVarDeclaration.getLocalVarDeclarationPartList().get(0);

    String name = localVarDeclarationPart.getComponentName().getName();
    HaxeTypeTag typeTag = localVarDeclarationPart.getTypeTag();
    HaxeVarInit varInit = localVarDeclarationPart.getVarInit();

    String text = "var " + name;
    if (typeTag != null) {
      text += " " + typeTag.getText();
    }
    text += ";";
    HaxeVarDeclaration varDeclaration = HaxeElementGenerator.createVarDeclaration(project, text);
    text = name + varInit.getText();
    varDeclaration.getNode().addLeaf(HaxeTokenTypes.OSEMI, "\n", null);
    PsiElement statementFromText = HaxeElementGenerator.createStatementFromText(project, text);
    statementFromText.getNode().addLeaf(HaxeTokenTypes.OSEMI, ";", null);

    localVarDeclaration.getParent().addBefore(varDeclaration, localVarDeclaration);
    PsiElement replace = localVarDeclaration.replace(statementFromText);

    final TextRange range = replace.getTextRange();
    final PsiFile baseFile = file.getViewProvider().getPsi(file.getViewProvider().getBaseLanguage());
    CodeStyleManager.getInstance(project).reformatText(baseFile, range.getStartOffset(), range.getEndOffset());
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }
}
