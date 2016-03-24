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
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeImportStatementRegular;
import com.intellij.plugins.haxe.lang.psi.HaxeImportStatementWithWildcard;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.plugins.haxe.util.HaxeImportUtil;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import java.util.List;

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
    HaxeImportStatementWithWildcard importStatementWithWildcard = PsiTreeUtil.getParentOfType(elementAt, HaxeImportStatementWithWildcard.class);
    if (importStatementWithWildcard != null) {
      String qName = UsefulPsiTreeUtil.getQNameForImportStatementWithWildcardType(importStatementWithWildcard);
      if (!UsefulPsiTreeUtil.isImportStatementWildcardForType(qName)) {
        List<HaxeClass>
          classesForImportStatementWithWildcard = UsefulPsiTreeUtil.getClassesForImportStatementWithWildcard(importStatementWithWildcard);

        return !classesForImportStatementWithWildcard.isEmpty();
      }
    }
    return false;
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    PsiElement elementAt = file.findElementAt(editor.getCaretModel().getOffset());
    HaxeImportStatementWithWildcard importStatementWithWildcard = PsiTreeUtil.getParentOfType(elementAt, HaxeImportStatementWithWildcard.class);

    String packageName = importStatementWithWildcard.getReferenceExpression().getText();

    for (HaxeClass haxeClass : HaxeImportUtil.getClassesUsedFromImportStatementWithWildcard(file, importStatementWithWildcard)) {
      HaxeImportStatementRegular importStatementRegular =
        HaxeElementGenerator.createImportStatementFromPath(importStatementWithWildcard.getProject(), haxeClass.getQualifiedName());

      importStatementWithWildcard.getParent().addBefore(importStatementRegular, importStatementWithWildcard);
    }

    importStatementWithWildcard.delete();
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }
}
