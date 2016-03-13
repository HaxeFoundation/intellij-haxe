/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2016 AS3Boyan
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

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.psi.*;

import java.util.List;

/**
 * Created by Boch on 05.02.2016.
 */
public class HaxeResolveVariableTest extends HaxeCodeInsightFixtureTestCase {
  @Override
  protected String getBasePath() {
    return "/resolve/variable/";
  }

  private void doTest() throws Exception {
    myFixture.configureByFile(getTestName(false) + ".hx");
  }

  //https://github.com/TiVo/intellij-haxe/issues/234
  public void testVariable() throws Exception {
    PsiFile[] files = myFixture.configureByFiles("Test.hx", "List.hx");

    PsiFile file = files[0];

    assert file != null;
    HaxeFile haxeFile = (HaxeFile)file;
    HaxeFunctionDeclarationWithAttributes method =
      (HaxeFunctionDeclarationWithAttributes)haxeFile.getClasses()[0].findMethodsByName("new", false)[0];

    HaxeBlockStatement blockStatement = method.getBlockStatement();
    assert blockStatement != null;
    List<HaxeExpression> haxeExpressions = blockStatement.getExpressionList();
    HaxeExpression expression = haxeExpressions.get(0);
    HaxeAssignExpression assignExpression = (HaxeAssignExpression)expression;
    HaxeExpression expression1 = assignExpression.getExpressionList().get(0);
    HaxeReferenceExpression referenceExpression = (HaxeReferenceExpression)expression1;
    PsiElement resolve = referenceExpression.resolve();
    assertTrue("list variable should be local var declaration part ", resolve instanceof HaxeVarDeclarationPart);
  }

}
