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
package com.intellij.plugins.haxe.ide;

import com.intellij.injected.editor.VirtualFileWindow;
import com.intellij.lang.ImportOptimizer;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxeImportStatementRegular;
import com.intellij.plugins.haxe.lang.psi.HaxeImportStatementWithInSupport;
import com.intellij.plugins.haxe.lang.psi.HaxeImportStatementWithWildcard;
import com.intellij.plugins.haxe.util.HaxeImportUtil;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by fedorkorotkov.
 */
public class HaxeImportOptimizer implements ImportOptimizer {
  @Override
  public boolean supports(PsiFile file) {
    return file instanceof HaxeFile;
  }

  @NotNull
  @Override
  public Runnable processFile(final PsiFile file) {
    VirtualFile vFile = file.getVirtualFile();
    if (vFile instanceof VirtualFileWindow) vFile = ((VirtualFileWindow)vFile).getDelegate();
    if (vFile == null || !ProjectRootManager.getInstance(file.getProject()).getFileIndex().isInSourceContent(vFile)) {
      return EmptyRunnable.INSTANCE;
    }

    return new Runnable() {
      @Override
      public void run() {
        optimizeImports(file);
      }
    };
  }

  private static void optimizeImports(final PsiFile file) {
    removeUnusedImports(file);

    reorderImports(file);
  }

  private static void removeUnusedImports(PsiFile file) {
    for (HaxeImportStatementRegular unusedImportStatement : HaxeImportUtil.findUnusedImports(file)) {
      unusedImportStatement.delete();
    }

    for (HaxeImportStatementWithInSupport unusedImportStatement : HaxeImportUtil.findUnusedInImports(file)) {
      unusedImportStatement.delete();
    }

    for (HaxeImportStatementWithWildcard unusedImportStatement : HaxeImportUtil.findUnusedInImportsWithWildcards(file)) {
      unusedImportStatement.delete();
    }

    // TODO Optimize this method, it takes a lot of time.

    // TODO Remove unused usings.
  }

  public static void reorderImports(final PsiFile file) {
    List<HaxeImportStatementRegular> allImports = UsefulPsiTreeUtil.getAllImportStatements(file);

    if (allImports.size() < 2) {
      return;
    }

    int startOffset = allImports.get(0).getStartOffsetInParent();
    HaxeImportStatementRegular lastImport = allImports.get(allImports.size() - 1);
    int endOffset = lastImport.getStartOffsetInParent() + lastImport.getText().length();

    // We assume the common practice of placing all imports in a single “block” at the top of a file. If there is something else (comments,
    // code, etc) there we just stop reordering to prevent data loss.
    for (PsiElement child : file.getChildren()) {
      int childOffset = child.getStartOffsetInParent();
      if (childOffset >= startOffset && childOffset <= endOffset
          && !(child instanceof HaxeImportStatementRegular)
          && !(child instanceof PsiWhiteSpace)) {
        return;
      }
    }

    List<String> sortedImports = new ArrayList<String>();

    for (HaxeImportStatementRegular currentImport : allImports) {
      sortedImports.add(currentImport.getText());
    }

    Collections.sort(sortedImports);

    final PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(file.getProject());
    final Document document = psiDocumentManager.getDocument(file);
    if (document != null) {
      final PsiDocumentManager documentManager = PsiDocumentManager.getInstance(file.getProject());

      documentManager.doPostponedOperationsAndUnblockDocument(document);
      document.deleteString(startOffset, endOffset);

      StringBuilder sortedImportsText = new StringBuilder();
      for (String sortedImport : sortedImports) {
        sortedImportsText.append(sortedImport);
        sortedImportsText.append("\n");
      }
      // Removes last "\n".
      CharSequence sortedImportsTextTrimmed = sortedImportsText.subSequence(0, sortedImportsText.length() - 1);

      documentManager.doPostponedOperationsAndUnblockDocument(document);
      document.insertString(startOffset, sortedImportsTextTrimmed);
    }

    // TODO Reorder usings.
  }
}