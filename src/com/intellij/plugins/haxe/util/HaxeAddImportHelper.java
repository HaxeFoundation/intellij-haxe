package com.intellij.plugins.haxe.util;

import com.intellij.plugins.haxe.lang.psi.HaxeImportStatement;
import com.intellij.plugins.haxe.lang.psi.HaxePackageStatement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeAddImportHelper {
  public static void addImport(String path, PsiFile file) {
    int positionIndex = 0;
    final PsiElement[] children = file.getChildren();

    while ((children[positionIndex] instanceof HaxePackageStatement ||
            UsefulPsiTreeUtil.isWhitespaceOrComment(children[positionIndex]) &&
            positionIndex < children.length)) {
      ++positionIndex;
    }

    assert positionIndex < children.length;
    insertImportBefore(path, file, children[positionIndex]);
  }

  private static void insertImportBefore(String path, PsiFile file, PsiElement child) {
    final HaxeImportStatement importStatement =
      HaxeElementGenerator.createImportStatementFromPath(file.getProject(), path);
    if (importStatement == null) {
      return;
    }

    file.addBefore(importStatement, child);
  }
}
