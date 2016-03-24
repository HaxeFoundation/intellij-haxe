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

import com.intellij.openapi.editor.Editor;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.HaxeIfStatement;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;

/**
 * Created by as3boyan on 06.10.14.
 */
public class IfConditionFixer implements Fixer {
  @Override
  public void apply(Editor editor, HaxeSmartEnterProcessor processor, PsiElement psiElement) throws IncorrectOperationException {
    if (!(psiElement instanceof HaxeIfStatement)) {
      return;
    }

    HaxeIfStatement ifStatement = (HaxeIfStatement)psiElement;

    if (null != ifStatement && ifStatement.getBlockStatementList().size() > 0) {
      return;
    }

    if (ifStatement.getNode().findChildByType(HaxeTokenTypes.PLPAREN) == null) {
      int offset = ifStatement.getFirstChild().getTextRange().getEndOffset();
      editor.getDocument().insertString(offset, " () {\n}");
      editor.getCaretModel().moveToOffset(offset + 2);
      processor.setSkipEnter(true);
    }
    else if (null != ifStatement.getExpressionList() && ifStatement.getNode().findChildByType(HaxeTokenTypes.PLCURLY) == null) {
      int offset = ifStatement.getNode().findChildByType(HaxeTokenTypes.PRPAREN).getTextRange().getEndOffset();
      editor.getDocument().insertString(offset, " {\n\n}");
      editor.getCaretModel().moveToOffset(offset + 3);
      processor.setSkipEnter(true);
    }
  }
}
