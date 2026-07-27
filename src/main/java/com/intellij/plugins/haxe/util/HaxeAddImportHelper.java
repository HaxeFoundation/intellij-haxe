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

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.plugins.haxe.lang.psi.HaxeExpressionCodeFragment;
import com.intellij.plugins.haxe.lang.psi.HaxeImportStatement;
import com.intellij.plugins.haxe.lang.psi.HaxePackageStatement;
import com.intellij.plugins.haxe.lang.psi.HaxeUsingStatement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiParserFacade;
import com.intellij.psi.util.PsiTreeUtil;
import lombok.CustomLog;

import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
@CustomLog
public class HaxeAddImportHelper {

  /**
   * Makes {@code path} resolvable in {@code file}. For an ordinary file this
   * inserts an {@code import path;} statement; for an evaluate/debugger
   * {@link HaxeExpressionCodeFragment} it records the import ON the fragment,
   * outside its text, so the evaluated expression is left untouched (the Haxe
   * analogue of {@link com.intellij.psi.PsiImportHolder#importClass}).
   *
   * <p>This is the single chokepoint every auto-import path — completion, copy /
   * paste, reference binding, the add-import intentions — flows through, so none
   * of them can inject an import into a fragment's evaluated text. Recording it on
   * the fragment changes no PSI, so resolve caches are dropped and highlighting
   * restarted to clear the now-stale "unresolved" state. Returns the inserted
   * statement, or {@code null} when the import was held on a fragment.
   */
  public static HaxeImportStatement addImport(String path, PsiFile file) {
    if (file instanceof HaxeExpressionCodeFragment fragment) {
      fragment.importClass(path);
      file.getManager().dropResolveCaches();
      DaemonCodeAnalyzer.getInstance(file.getProject()).restart(file,"new import");
      return null;
    }
    PsiElement child = PsiTreeUtil.findChildOfType(file, HaxePackageStatement.class);
    List<HaxeImportStatement> importStatements = PsiTreeUtil.findChildrenOfType(file, HaxeImportStatement.class).stream().toList();
    if(!importStatements.isEmpty()) {
      child = importStatements.get(importStatements.size()-1);
    }


    if(child != null) {
      return insertImportBefore(path, file, child);
    }else {
      // if no package or import found (probably first import), we just add it to the top of the file
      return insertImportTop(path, file);
    }
  }
  public static HaxeUsingStatement addUsing(String path, PsiFile file) {
    if (file instanceof HaxeExpressionCodeFragment) {
      // a fragment holds no `using` list; skip rather than inject `using` text
      // into the evaluated expression
      return null;
    }
    PsiElement child = PsiTreeUtil.findChildOfType(file, HaxePackageStatement.class);
    List<HaxeImportStatement> importStatements = PsiTreeUtil.findChildrenOfType(file, HaxeImportStatement.class).stream().toList();
    if(!importStatements.isEmpty()) {
      child = importStatements.get(importStatements.size()-1);
    }


    if(child != null) {
      return insertUsingBefore(path, file, child);
    }else {
      // if no package or import found (probably first import), we just add it to the top of the file
      return insertUsingTop(path, file);
    }
  }

  private static HaxeImportStatement insertImportBefore(String path, PsiFile file, PsiElement child) {
    final HaxeImportStatement importStatement =
      HaxeElementGenerator.createImportStatementFromPath(file.getProject(), path);
    if (importStatement == null) {
      return null;
    }

    final PsiElement newLineElement = PsiParserFacade.getInstance(file.getProject()).createWhiteSpaceFromText("\n");
    HaxeImportStatement element = (HaxeImportStatement)file.addAfter(importStatement, child);
    file.addAfter(newLineElement, child);
    return element;
  }
  private static HaxeUsingStatement insertUsingBefore(String path, PsiFile file, PsiElement child) {
    final HaxeUsingStatement usingStatement =
      HaxeElementGenerator.createUsingStatementFromPath(file.getProject(), path);
    if (usingStatement == null) {
      return null;
    }

    final PsiElement newLineElement = PsiParserFacade.getInstance(file.getProject()).createWhiteSpaceFromText("\n");
    HaxeUsingStatement element = (HaxeUsingStatement)file.addAfter(usingStatement, child);
    file.addAfter(newLineElement, child);
    return element;
  }

  private static HaxeImportStatement insertImportTop(String path, PsiFile file) {
    HaxeImportStatement importStatement = HaxeElementGenerator.createImportStatementFromPath(file.getProject(), path);
    if (importStatement == null) {
      return null;
    }

    final PsiElement newLineElement = PsiParserFacade.getInstance(file.getProject()).createWhiteSpaceFromText("\n");
    PsiElement child = file.getFirstChild();
    HaxeImportStatement element = (HaxeImportStatement)file.addBefore(importStatement.copy(), child);
    file.addAfter(newLineElement.copy(), element);
    return element;
  }
  private static HaxeUsingStatement insertUsingTop(String path, PsiFile file) {
    HaxeUsingStatement usingStatement = HaxeElementGenerator.createUsingStatementFromPath(file.getProject(), path);
    if (usingStatement == null) {
      return null;
    }

    final PsiElement newLineElement = PsiParserFacade.getInstance(file.getProject()).createWhiteSpaceFromText("\n");
    PsiElement child = file.getFirstChild();
    HaxeUsingStatement element = (HaxeUsingStatement)file.addBefore(usingStatement.copy(), child);
    file.addAfter(newLineElement.copy(), element);
    return element;
  }
}
