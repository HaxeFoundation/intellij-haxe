/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2017 Ilya Malanin
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
package com.intellij.plugins.haxe.util;

import com.intellij.plugins.haxe.lang.psi.HaxeImportStatement;
import com.intellij.plugins.haxe.lang.psi.HaxePackageStatement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiParserFacade;
import lombok.CustomLog;

import static com.intellij.plugins.haxe.util.UsefulPsiTreeUtil.isWhitespaceOrCommentButNotDocs;

/**
 * @author: Fedor.Korotkov
 */
@CustomLog
public class HaxeAddImportHelper {
  public static HaxeImportStatement addImport(String path, PsiFile file) {
    int positionIndex = 0;
    final PsiElement[] children = file.getChildren();

    PsiElement child = children[positionIndex];
    // find HaxePackageStatement position
    while (!(child instanceof HaxePackageStatement)) {
      if (++positionIndex < children.length) {
        child = children[positionIndex];
      }else {
        log.warn("Unable to insert Import statement");
        return null;
      }
    }
    // find last whitespace line before  docs or code and after last import
    while (child instanceof HaxePackageStatement
           || child instanceof HaxeImportStatement
           || isWhitespaceOrCommentButNotDocs(child)) {
      if (++positionIndex < children.length) {
        child = children[positionIndex];
      }else {
        log.warn("Unable to insert Import statement");
        return null;
      }
    }

    assert child != null;
    return insertImportBefore(path, file, child);
  }

  private static HaxeImportStatement insertImportBefore(String path, PsiFile file, PsiElement child) {
    final HaxeImportStatement importStatement =
      HaxeElementGenerator.createImportStatementFromPath(file.getProject(), path);
    if (importStatement == null) {
      return null;
    }

    final PsiElement newLineElement = PsiParserFacade.getInstance(file.getProject()).createWhiteSpaceFromText("\n");
    file.addBefore(newLineElement, child);
    file.addBefore(importStatement, child);
    return importStatement;
  }
}
