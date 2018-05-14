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
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.lang.psi.HaxeParameterList;
import com.intellij.plugins.haxe.model.HaxeDocumentModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeAddMethodReturnTypeFix extends HaxeFixAndIntentionAction {
  private final ResultHolder type;

  public HaxeAddMethodReturnTypeFix(@Nullable HaxeMethod element, ResultHolder type) {
    super(element);
    this.type = type;
  }

  @Override
  public void invoke(@NotNull Project project,
                     @NotNull PsiFile file,
                     @Nullable Editor editor,
                     @NotNull PsiElement startElement,
                     @NotNull PsiElement endElement) {
    HaxeDocumentModel document = HaxeDocumentModel.fromElement(startElement);

    HaxeMethodModel model = ((HaxeMethod)startElement).getModel();
    PsiElement anchor = UsefulPsiTreeUtil.getChild(model.getMethodPsi(), HaxeParameterList.class);
    if (anchor != null) {
      anchor = PsiTreeUtil.findSiblingForward(anchor, HaxeTokenTypes.PRPAREN, null);
    } else {
      PsiElement body = model.getBodyPsi();
      if (body != null) {
        anchor = PsiTreeUtil.findSiblingBackward(body, HaxeTokenTypes.PRPAREN, null);
      }
    }
    if (anchor != null) {
      document.addTextAfterElement(anchor, ':' + type.toStringWithoutConstant());
    }
  }

  @NotNull
  @Override
  public String getText() {
    return HaxeBundle.message("haxe.quickfix.add.method.return.type", type.toStringWithoutConstant());
  }
}
