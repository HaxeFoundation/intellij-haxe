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

import com.intellij.codeInsight.editorActions.smartEnter.SmartEnterProcessor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by as3boyan on 06.10.14.
 */
public class HaxeSmartEnterProcessor extends SmartEnterProcessor {
  private static final Logger LOG = Logger.getInstance("#com.intellij.plugins.haxe.editor.smartEnter.HaxeSmartEnterProcessor");

  private static final Fixer[] ourFixers;

  private static boolean skipEnter;

  static {
    final List<Fixer> fixers = new ArrayList<Fixer>();
    fixers.add(new MissingClassBodyFixer());
    fixers.add(new IfConditionFixer());
    fixers.add(new SemicolonFixer());
    ourFixers = fixers.toArray(new Fixer[fixers.size()]);
  }

  public void setSkipEnter(boolean skipEnter) {
    HaxeSmartEnterProcessor.skipEnter = skipEnter;
  }

  @Override
  protected void reformat(PsiElement atCaret) throws IncorrectOperationException {
    if (atCaret == null) {
      return;
    }

    super.reformat(atCaret);
  }

  @Override
  public boolean process(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile psiFile) {
    commit(editor);

    PsiElement statementAtCaret = getStatementAtCaret(editor, psiFile);

    skipEnter = false;

    List<PsiElement> psiElements = new ArrayList<PsiElement>();

    PsiElement psiElement;

    psiElement = statementAtCaret;

    while (psiElement != null && !(psiElement instanceof HaxeFile)) {
      psiElements.add(psiElement);
      psiElement = psiElement.getParent();
    }

    for (PsiElement element : psiElements) {
      for (Fixer fixer : ourFixers) {
        fixer.apply(editor, this, element);

        if (isUncommited(element.getProject())) {
          reformat(element);
          return skipEnter;
        }
      }
    }

    return skipEnter;
  }
}
