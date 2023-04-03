/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
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
import com.intellij.plugins.haxe.lang.psi.HaxeClassBody;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.util.HaxeCodeGenerateUtil;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

/**
 * Created by as3boyan on 06.10.14.
 * Adapted to Haxe from com.intellij.codeInsight.editorActions.smartEnter.MissingClassBodyFixer
 */
public class MissingClassBodyFixer implements Fixer {
  public static final String CLASS_BODY_BEGIN = " {";
  public static final String CLASS_BODY_END = "\n}";

  @Override
  public void apply(Editor editor, HaxeSmartEnterProcessor processor, PsiElement psiElement) throws IncorrectOperationException {
    if (isTopLevel(psiElement)) {
      if (psiElement instanceof HaxeClass) {
        fixMissingBody(editor, (HaxeClass)psiElement);
      }
      else if (psiElement.getNode().getElementType() == HaxeTokenTypes.PLCURLY) {
        fixPartialBody(editor, psiElement);
      }
    }
  }

  private boolean isTopLevel(@NotNull PsiElement element) {
    return element.getParent() instanceof HaxeFile;
  }

  private boolean isMissingBody(@NotNull HaxeClass haxeClass) {
    return null == haxeClass.getNode().findChildByType(HaxeTokenTypes.CLASS_BODY);
  }

  private void fixMissingBody(Editor editor, HaxeClass haxeClass) {
    HaxeComponentName haxeClassComponentName = haxeClass.getComponentName();
    ASTNode bodyNode = haxeClass.getNode().findChildByType(HaxeTokenTypes.CLASS_BODY);
    HaxeClassBody body = null != bodyNode ? (HaxeClassBody)bodyNode.getPsi() : null;

    // XXX: Could generate component name from file name (already generated when the file was created), or from an
    //      unresolved and unqualified class reference nearby?

    if (haxeClassComponentName != null) {
      if (null == body) {
        int offset = haxeClassComponentName.getTextRange().getEndOffset();
        editor.getDocument().insertString(offset, CLASS_BODY_BEGIN + CLASS_BODY_END);
        editor.getCaretModel().moveToOffset(offset);
      } else {
        fixPartialBody(editor, body);
      }
    }
  }

  private HaxeClass findPreviousClassToken(PsiElement element) {
    PsiElement previous = UsefulPsiTreeUtil.getPrevSiblingSkipWhiteSpacesAndComments(element, true);
    if (previous instanceof HaxeClass) {
      return (HaxeClass)previous;
    }
    return null;
  }

  private void fixPartialBody(Editor editor, PsiElement curlyBracket) {

    // We don't have a closing bracket, or else we wouldn't be getting a curly bracket for an element.

    HaxeClass haxeClass = findPreviousClassToken(curlyBracket);
    HaxeComponentName haxeClassComponentName = null != haxeClass ? haxeClass.getComponentName() : null;

    // XXX: Could generate component name from file name (already generated when the file was created), or from an
    //      unresolved and unqualified class reference nearby?

    if (haxeClassComponentName != null && isMissingBody(haxeClass)) {
      int offset = curlyBracket.getTextRange().getEndOffset();
      editor.getDocument().insertString(offset, CLASS_BODY_END);
      editor.getCaretModel().moveToOffset(offset);
    }
  }

  private void fixPartialBody(@NotNull Editor editor, @NotNull HaxeClassBody body) {
    if (!HaxeCodeGenerateUtil.addStringAtErrorElement(editor, body, CLASS_BODY_END)) {
      HaxeCodeGenerateUtil.addMissingDelimiters(editor, body, HaxeTokenTypes.PLCURLY, HaxeTokenTypes.PRCURLY);
    }
  }
}