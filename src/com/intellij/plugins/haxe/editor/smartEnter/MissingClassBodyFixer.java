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

import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Editor;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.impl.source.tree.ChildRole;
import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;

/**
 * Created by as3boyan on 06.10.14.
 * Adapted to Haxe from com.intellij.codeInsight.editorActions.smartEnter.MissingClassBodyFixer
 */
public class MissingClassBodyFixer implements Fixer {
  @Override
  public void apply(Editor editor, HaxeSmartEnterProcessor processor, PsiElement psiElement) throws IncorrectOperationException {
    HaxeClass haxeClass = PsiTreeUtil.getParentOfType(psiElement, HaxeClass.class);
    if (haxeClass == null) return;

    ASTNode node = haxeClass.getNode().findChildByType(HaxeTokenTypes.PLCURLY);

    if (node == null) {
      int offset = haxeClass.getTextRange().getEndOffset();
      editor.getDocument().insertString(offset, " {\n}");
      editor.getCaretModel().moveToOffset(offset);
    }
  }
}