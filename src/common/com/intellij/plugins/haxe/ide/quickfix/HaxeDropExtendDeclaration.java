/*
 * Copyright 2018-2018 Ilya Malanin
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
package com.intellij.plugins.haxe.ide.quickfix;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeDocumentModel;
import com.intellij.plugins.haxe.model.StripSpaces;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HaxeDropExtendDeclaration extends HaxeFixAndIntentionAction {
  private final SmartPsiElementPointer<HaxeType> type;

  public HaxeDropExtendDeclaration(HaxeInheritanceDeclaration o, HaxeType dropExtendType) {
    super(o);
    this.type = SmartPointerManager.getInstance(o.getProject()).createSmartPsiElementPointer(dropExtendType);
  }

  public HaxeDropExtendDeclaration(HaxeAnonymousType o, HaxeType dropExtendType) {
    super(o);
    this.type = SmartPointerManager.getInstance(o.getProject()).createSmartPsiElementPointer(dropExtendType);
  }

  @Override
  public void invoke(@NotNull Project project,
                     @NotNull PsiFile file,
                     @Nullable Editor editor,
                     @NotNull PsiElement startElement,
                     @NotNull PsiElement endElement) {

    HaxeType elementToRemove = type.getElement();

    if (elementToRemove == null || !(startElement instanceof HaxeClass)) return;

    List<HaxeType> list = ((HaxeClass)startElement).getHaxeExtendsList();
    final IElementType leftBorder;
    if (startElement instanceof HaxeAnonymousType) {
      int typeIndex = list.indexOf(elementToRemove);
      if (typeIndex < 0) return;
      leftBorder = typeIndex == 0 ? HaxeTokenTypes.OGREATER : HaxeTokenTypes.OCOMMA;
    } else {
      leftBorder = HaxeTokenTypes.KEXTENDS;
    }

    PsiElement extendToken = UsefulPsiTreeUtil.getPrevSiblingSkippingCondition(
      elementToRemove,
      element -> !(element instanceof HaxePsiToken) || !(((HaxePsiToken)element).getTokenType().equals(leftBorder)),
      true
    );

    if (extendToken != null) {
      TextRange replaceTextRange = new TextRange(
        extendToken.getTextRange().getStartOffset(),
        elementToRemove.getTextRange().getEndOffset()
      );
      HaxeDocumentModel.fromElement(startElement).replaceElementText(replaceTextRange, "", StripSpaces.AFTER);
    }
  }

  @Override
  public boolean isAvailable(@NotNull Project project,
                             @NotNull PsiFile file,
                             @NotNull PsiElement startElement,
                             @NotNull PsiElement endElement) {
    return type.getElement() != null;
  }

  @NotNull
  @Override
  public String getText() {
    return HaxeBundle.message("haxe.quickfix.drop.extend");
  }
}
