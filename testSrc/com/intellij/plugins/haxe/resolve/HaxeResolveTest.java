/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2015 AS3Boyan
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
package com.intellij.plugins.haxe.resolve;

import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.ide.module.HaxeModuleType;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxeTestUtils;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.MultiFileTestCase;

/**
 * Created by Boch on 20.04.2015.
 */
public class HaxeResolveTest extends MultiFileTestCase {
  @Override
  protected String getTestDataPath() {
    return HaxeTestUtils.BASE_TEST_DATA_PATH;
  }

  @Override
  protected String getTestRoot() {
    return "/resolve/";
  }

  @Override
  protected ModuleType getModuleType() {
    return HaxeModuleType.getInstance();
  }

  //Both names are relative to root directory
  private void doTest() throws Exception {
    doTest(new PerformAction() {
      @Override
      public void performAction(VirtualFile rootDir, VirtualFile rootAfter) throws Exception {
        String pathToTest = "Test.hx";
        final VirtualFile child = VfsUtil.findRelativeFile(pathToTest, rootDir);
        assertNotNull("Neither class nor file " + pathToTest + " not found", child);
        PsiElement file = myPsiManager.findFile(child);
        assertNotNull(file);

        String pathToExternEnum = "bar/Bar.hx";
        final VirtualFile externEnumFile = VfsUtil.findRelativeFile(pathToExternEnum, rootDir);
        PsiElement file2 = myPsiManager.findFile(externEnumFile);

        HaxeFile haxeFile = (HaxeFile)file;
        HaxeFunctionDeclarationWithAttributes functionDeclarationWithAttributes =
          (HaxeFunctionDeclarationWithAttributes)haxeFile.getClasses()[0].findMethodsByName("test", true)[0];

        assertNotNull(functionDeclarationWithAttributes);

        HaxeExpression expression = functionDeclarationWithAttributes.getBlockStatement().getReturnStatementList().get(0).getExpression();
        assertNotNull(expression);

        HaxeReferenceExpression referenceExpression = (HaxeReferenceExpression)expression;
        PsiElement resolve = referenceExpression.resolve();

        assertNotNull(resolve);

        HaxeFile haxeFile2 = (HaxeFile)file2;
        HaxeEnumDeclaration enumDeclaration = (HaxeEnumDeclaration)haxeFile2.getClasses()[0];
        assertTrue(enumDeclaration.getEnumBody().getEnumValueDeclarationList().contains(resolve));
      }
    });
  }

  //For example accessing flash.display.StageAlign.TOP_LEFT in some class;
  //https://github.com/TiVo/intellij-haxe/issues/39
  public void testResolveExternEnumByFullPath() throws Exception {
    doTest();
  }
}
