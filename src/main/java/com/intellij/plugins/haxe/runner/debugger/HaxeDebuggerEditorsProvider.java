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
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.xdebugger.impl.XDebuggerHistoryManager;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.evaluation.EvaluationMode;
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeDebuggerEditorsProvider extends XDebuggerEditorsProvider {
  /**
   * Marks a fragment whose editor expects a plain VALUE (the Variables view's
   * Set Value field): completion must not auto-pop there — the user is typing
   * a literal and Enter means "submit", but an open lookup swallows it and
   * inserts a suggestion instead: typing a number and pressing Enter appends
   * the "function" keyword. Explicit completion (Ctrl+Space) still works.
   */
  public static final Key<Boolean> LITERAL_VALUE_INPUT = Key.create("haxe.debugger.literal.value.input");

  /**
   * The history id the platform's Set Value editor stores its expressions
   * under (SetValueInplaceEditor's editor id, verified in the platform
   * source). The provider is never TOLD which editor a document is for (the
   * createDocument overload with an editor id exists but is always fed null),
   * but the Set Value editor's expressions land in this history, so an
   * expression found there identifies the editor.
   */
  private static final String SET_VALUE_HISTORY_ID = "setValue";

  @NotNull
  public FileType getFileType() {
    return HaxeFileType.INSTANCE;
  }

  @NotNull
  public Document createDocument(@NotNull Project project,
                                 @NotNull XExpression expression,
                                 @Nullable XSourcePosition sourcePosition,
                                 @NotNull EvaluationMode mode) {

    Document document = HaxeDebuggerSupportUtils.createDocument(expression.getExpression(), project,
                                                                sourcePosition != null ? sourcePosition.getFile() : null,
                                                                sourcePosition != null ? sourcePosition.getOffset() : -1
    );
    if (XDebuggerHistoryManager.getInstance(project)
          .getRecentExpressions(SET_VALUE_HISTORY_ID)
          .contains(expression)) {
      PsiFile fragment = PsiDocumentManager.getInstance(project).getPsiFile(document);
      if (fragment != null) {
        fragment.putUserData(LITERAL_VALUE_INPUT, Boolean.TRUE);
      }
    }
    return document;
  }
}
