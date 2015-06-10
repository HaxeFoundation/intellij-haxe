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
package com.intellij.plugins.haxe.editor.smartEnter;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by as3boyan on 07.10.14.
 */
public class SemicolonFixer implements Fixer {
  @Override
  public void apply(Editor editor, HaxeSmartEnterProcessor processor, PsiElement psiElement) throws IncorrectOperationException {
    if (fixReturn(editor, psiElement) || fixAfterLastValidElement(editor, psiElement)) {
      processor.setSkipEnter(true);
    }
  }

  private static boolean fixReturn(@NotNull Editor editor, @Nullable PsiElement psiElement) {
    if (!(psiElement instanceof HaxeReturnStatement)) {
      return false;
    }

    HaxeReturnStatement haxeReturnStatement = (HaxeReturnStatement)psiElement;

    if (StringUtil.endsWithChar(haxeReturnStatement.getText(), ';')) {
      return false;
    }

    HaxeFunctionDeclarationWithAttributes haxeFunctionDeclarationWithAttributes =
      PsiTreeUtil.getParentOfType(haxeReturnStatement, HaxeFunctionDeclarationWithAttributes.class);

    if (haxeFunctionDeclarationWithAttributes != null) {
      HaxeTypeTag typeTag = haxeFunctionDeclarationWithAttributes.getTypeTag();
      if (typeTag != null) {
        HaxeTypeOrAnonymous typeOrAnonymous = typeTag.getTypeOrAnonymous();
        if (typeOrAnonymous != null) {
          if (typeOrAnonymous.getText().equals("Void")) {
            return false;
          }
        }
      }
    }

    HaxeExpression haxeReturnStatementExpression = haxeReturnStatement.getExpression();
    if (haxeReturnStatementExpression != null) {
      Document doc = editor.getDocument();
      int offset = haxeReturnStatementExpression.getTextRange().getEndOffset();
      doc.insertString(offset, ";");
      editor.getCaretModel().moveToOffset(offset + 1);
      return true;
    }

    return false;
  }

  private static boolean fixAfterLastValidElement(@NotNull Editor editor, @Nullable PsiElement psiElement) {
    if (psiElement == null ||
        !(psiElement instanceof HaxeExpression) &&
        !(psiElement instanceof HaxeReferenceExpression) &&
        !(psiElement instanceof HaxeImportStatementRegular) &&
        !(psiElement instanceof HaxeImportStatementWithInSupport) &&
        !(psiElement instanceof HaxeImportStatementWithWildcard) &&
        !(psiElement instanceof HaxeBreakStatement) &&
        !(psiElement instanceof HaxeContinueStatement)) {
      return false;
    }

    if (StringUtil.endsWithChar(psiElement.getText(), ';')) {
      return false;
    }

    Document doc = editor.getDocument();
    int offset = psiElement.getTextRange().getEndOffset();
    doc.insertString(offset, ";");
    editor.getCaretModel().moveToOffset(offset + 1);
    return true;
  }


}
