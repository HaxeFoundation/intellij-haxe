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
package com.intellij.plugins.haxe.ide.intention;

import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeStringLiteralExpression;
import com.intellij.plugins.haxe.model.HaxeDocumentModel;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ConvertQuotesIntention extends BaseIntentionAction {
  private static final String SINGLE_QUOTES = "'";
  private static final String DOUBLE_QUOTES = "\"";

  private HaxeStringLiteralExpression expression;

  public ConvertQuotesIntention() {
  }

  @Nls
  @NotNull
  @Override
  public String getFamilyName() {
    return "Convert quotes";
  }

  @NotNull
  @Override
  public String getText() {
    if (isWrappedWithDoubleQuotes(expression)) {
      return "Wrap with single quotes";
    }

    return "Wrap with double quotes";
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
    PsiElement place = file.findElementAt(editor.getCaretModel().getOffset());
    expression = PsiTreeUtil.getParentOfType(place, HaxeStringLiteralExpression.class);

    return expression != null && canSwitchQuotes(expression);
  }

  @Override
  public void invoke(@NotNull final Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    ApplicationManager.getApplication().invokeLater(
      new Runnable() {
        @Override
        public void run() {
          new WriteCommandAction.Simple(project) {
            @Override
            public void run() {
              final HaxeDocumentModel document = HaxeDocumentModel.fromElement(expression);
              final String newQuotes = isWrappedWithDoubleQuotes(expression) ? SINGLE_QUOTES : DOUBLE_QUOTES;

              document.replaceElementText(expression.getFirstChild(), newQuotes);
              document.replaceElementText(expression.getLastChild(), newQuotes);
            }
          }.execute();
        }
      }
    );
  }

  private boolean canSwitchQuotes(HaxeStringLiteralExpression expression) {
    boolean hasInterpolation = expression.getLongTemplateEntryList().size() != 0 || expression.getShortTemplateEntryList().size() != 0;
    return !hasInterpolation || isWrappedWithDoubleQuotes(expression);
  }

  private boolean isWrappedWithDoubleQuotes(HaxeStringLiteralExpression expression) {
    return Objects.equals(expression.getFirstChild().getText(), DOUBLE_QUOTES);
  }
}