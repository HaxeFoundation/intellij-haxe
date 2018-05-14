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

import com.intellij.lang.ASTNode;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.HaxePackageStatement;
import com.intellij.plugins.haxe.lang.psi.HaxeReference;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxePackageNameFix extends HaxeFixAndIntentionAction {
  private final String rootBasedPackageName;

  public HaxePackageNameFix(HaxePackageStatement statement, String rootBasedPackageName) {
    super(statement);
    this.rootBasedPackageName = rootBasedPackageName;
  }

  @Override
  public void invoke(@NotNull Project project,
                     @NotNull PsiFile file,
                     @Nullable Editor editor,
                     @NotNull PsiElement startElement,
                     @NotNull PsiElement endElement) {

    HaxePackageStatement packageStatement = (HaxePackageStatement)startElement;
    Document document = PsiDocumentManager.getInstance(project).getDocument(packageStatement.getContainingFile());
    if (document != null) {
      HaxeReference reference = packageStatement.getReferenceExpression();
      if (reference != null) {
        TextRange textRange = reference.getTextRange();
        document.replaceString(textRange.getStartOffset(), textRange.getEndOffset(), rootBasedPackageName);
      } else {
        ASTNode semicolonNode = packageStatement.getNode().findChildByType(HaxeTokenTypes.OSEMI);
        int insertOffset;
        if (semicolonNode != null) {
          insertOffset = semicolonNode.getStartOffset();
        } else {
          insertOffset = packageStatement.getTextRange().getEndOffset();
        }
        document.insertString(insertOffset, rootBasedPackageName);
      }
    }
  }

  @Override
  public boolean isAvailable(@NotNull Project project,
                             @NotNull PsiFile file,
                             @NotNull PsiElement startElement,
                             @NotNull PsiElement endElement) {
    return rootBasedPackageName != null;
  }

  @NotNull
  @Override
  public String getText() {
    return HaxeBundle.message("haxe.quickfix.set.package.name.to", rootBasedPackageName);
  }
}
