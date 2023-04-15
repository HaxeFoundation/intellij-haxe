/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2017 Ilya Malanin
 * Copyright 2018-2020 Eric Bishton
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

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeExpressionCodeFragmentImpl;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.LightVirtualFile;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeElementGenerator {

  public static PsiElement createExpressionFromText(Project myProject, String text) {
    PsiElement fromText = createStatementFromText(myProject, "var test = " + text + ";");
    if (fromText instanceof HaxeFieldDeclaration) {
      HaxeFieldDeclaration declarationPart = ((HaxeFieldDeclaration)fromText);
      HaxeVarInit varInit = declarationPart.getVarInit();
      return varInit != null ? varInit.getExpression() : null;
    }
    return null;
  }

  public static PsiElement createStatementFromText(Project myProject, String text) {
    final PsiFile dummyFile = createDummyFile(myProject, HaxeCodeGenerateUtil.wrapStatement(text).getFirst());
    final HaxeClass haxeClass = PsiTreeUtil.getChildOfType(dummyFile, HaxeClass.class);
    assert haxeClass != null;
    final HaxeMethodDeclaration mainMethod =
      (HaxeMethodDeclaration)haxeClass.getHaxeMethods(null).iterator().next();
    final HaxeBlockStatement statement = mainMethod.getBlockStatement();
    assert statement != null;
    return statement.getChildren()[0];
  }
  public static HaxeFieldDeclaration createVarDeclaration(Project myProject, String text) {
    final PsiFile dummyFile = createDummyFile(myProject, HaxeCodeGenerateUtil.wrapFunction(text).getFirst());
    final HaxeClass haxeClass = PsiTreeUtil.getChildOfType(dummyFile, HaxeClass.class);
    assert haxeClass != null;
    return haxeClass.getFieldDeclarations(null).iterator().next();
  }

  // XXX: Eventually, this ordering should come from the class order in
  //      preferences... once we have one.
  private static List<HaxeNamedComponent> sortNamedSubComponents(List<HaxeNamedComponent> unsorted) {
    // Can't sort a hashed collection, so we must copy it to an orderable type.
    List<HaxeNamedComponent> sorted = new ArrayList<HaxeNamedComponent>(unsorted);
    Collections.sort(sorted, new Comparator<HaxeNamedComponent>() {
      @Override
      public int compare(HaxeNamedComponent o1, HaxeNamedComponent o2) {
        String name1 = o1.getName();
        String name2 = o2.getName();
        return name1.compareTo(name2);
      }
    });
    return sorted;
  }

  public static List<HaxeNamedComponent> createNamedSubComponentsFromText(Project myProject, String text) {
    final PsiFile dummyFile = createDummyFile(myProject, HaxeCodeGenerateUtil.wrapFunction(text).getFirst());
    final HaxeClass haxeClass = PsiTreeUtil.getChildOfType(dummyFile, HaxeClass.class);
    assert haxeClass != null;
    return sortNamedSubComponents(HaxeResolveUtil.findNamedSubComponents(null, haxeClass));
  }

  @Nullable
  public static HaxeIdentifier createIdentifierFromText(Project myProject, String name) {
    return createImportAndFindChild(myProject, name, HaxeIdentifier.class);
  }

  @Nullable
  public static HaxeReference createReferenceFromText(Project myProject, String name) {
    return createImportAndFindChild(myProject, name, HaxeReference.class);
  }

  @Nullable
  public static HaxeReferenceExpression createReferenceExpressionFromText(Project myProject, String name) {
    return createImportAndFindChild(myProject, name, HaxeReferenceExpression.class);
  }

  @Nullable
  private static <T extends PsiElement> T createImportAndFindChild(Project myProject, String name, Class<T> aClass) {
    final HaxeImportStatement importStatement = createImportStatementFromPath(myProject, name);
    if (importStatement == null) {
      return null;
    }
    return PsiTreeUtil.findChildOfType(importStatement, aClass);
  }

  @Nullable
  public static HaxeImportStatement createImportStatementFromPath(Project myProject, String path) {
    final PsiFile dummyFile = createDummyFile(myProject, "import " + path + ";");
    return PsiTreeUtil.getChildOfType(dummyFile, HaxeImportStatement.class);
  }

  @Nullable
  public static HaxePsiToken createEmptyStatement(Project myProject) {
    final HaxeImportStatement importStatement = createImportStatementFromPath(myProject, "Std");
    PsiElement last = importStatement.getLastChild();
    assert last instanceof HaxePsiToken;
    return last instanceof HaxePsiToken ? (HaxePsiToken)last : null;
  }

  @Nullable
  public static HaxePackageStatement createPackageStatementFromPath(Project myProject, String path) {
    final PsiFile dummyFile = createDummyFile(myProject, "package " + path + ";");
    return PsiTreeUtil.getChildOfType(dummyFile, HaxePackageStatement.class);
  }

  public static PsiFile createDummyFile(Project myProject, String text) {
    final PsiFileFactory factory = PsiFileFactory.getInstance(myProject);
    final String name = "dummy." + HaxeFileType.HAXE_FILE_TYPE.getDefaultExtension();
    final LightVirtualFile virtualFile = new LightVirtualFile(name, HaxeFileType.HAXE_FILE_TYPE, text);
    final PsiFile psiFile = ((PsiFileFactoryImpl)factory).trySetupPsiForFile(virtualFile, HaxeLanguage.INSTANCE, false, true);
    assert psiFile != null;
    return psiFile;
  }

  public static PsiFile createExpressionCodeFragment(Project myProject, String text, PsiElement context, boolean resolveScope) {
    final String name = "dummy." + HaxeFileType.HAXE_FILE_TYPE.getDefaultExtension();
    HaxeExpressionCodeFragmentImpl codeFragment = new HaxeExpressionCodeFragmentImpl(myProject, name, text, true);
    codeFragment.setContext(context);
    return codeFragment;
  }

  public static HaxeMethodDeclaration createMethodDeclaration(Project myProject,
                                                              String text) {
    final PsiFile dummyFile = createDummyFile(myProject, HaxeCodeGenerateUtil.wrapInterfaceFunction(text).getFirst());
    final HaxeClass haxeClass = PsiTreeUtil.getChildOfType(dummyFile, HaxeClass.class);
    assert haxeClass != null;
    return (HaxeMethodDeclaration)haxeClass.getHaxeMethods(null).iterator().next();
  }
}
