/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.plugins.haxe.lang.psi.manipulators;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.lang.psi.HaxeRegularExpressionLiteral;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.psi.AbstractElementManipulator;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;

/**
 * Created by fedorkorotkov.
 */
public class HaxeRegularExpressionLiteralManipulator extends AbstractElementManipulator<HaxeRegularExpressionLiteral> {
  @Override
  public HaxeRegularExpressionLiteral handleContentChange(HaxeRegularExpressionLiteral element, TextRange range, String newContent)
    throws IncorrectOperationException {
    String oldText = element.getText();
    PsiFile file = element.getContainingFile();
    newContent = StringUtil.escapeSlashes(newContent);
    String newText = oldText.substring(0, range.getStartOffset()) + newContent + oldText.substring(range.getEndOffset());
    PsiElement fromText = HaxeElementGenerator.createExpressionFromText(file.getProject(), newText);
    if (fromText instanceof HaxeRegularExpressionLiteral) {
      return (HaxeRegularExpressionLiteral)element.replace(fromText);
    }
    return element;
  }
}
