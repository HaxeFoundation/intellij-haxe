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
package com.intellij.plugins.haxe.runner.debugger;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeDebuggerSupportUtils {
  public static Document createDocument(@NotNull final String text,
                                        @NotNull final Project project,
                                        @Nullable final VirtualFile contextVirtualFile,
                                        final int contextOffset) {

    PsiElement context = null;
    if (contextVirtualFile != null) {
      context = getContextElement(contextVirtualFile, contextOffset, project);
    }
    final PsiFile codeFragment = HaxeElementGenerator.createExpressionCodeFragment(project, text, context, true);
    return PsiDocumentManager.getInstance(project).getDocument(codeFragment);
  }

  @Nullable
  public static PsiElement getContextElement(VirtualFile virtualFile, int offset, final @NotNull Project project) {
    Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
    PsiFile file = PsiManager.getInstance(project).findFile(virtualFile);
    if (file == null || document == null) {
      return null;
    }

    if (offset < 0) offset = 0;
    if (offset > document.getTextLength()) offset = document.getTextLength();
    int startOffset = offset;

    int lineEndOffset = document.getLineEndOffset(document.getLineNumber(offset));
    PsiElement result = null;
    do {
      PsiElement element = file.findElementAt(offset);
      if (!(element instanceof PsiWhiteSpace) && !(element instanceof PsiComment)) {
        result = element;
        break;
      }

      offset = element.getTextRange().getEndOffset() + 1;
    }
    while (offset < lineEndOffset);

    if (result == null) {
      result = file.findElementAt(startOffset);
    }
    return result;
  }
}
