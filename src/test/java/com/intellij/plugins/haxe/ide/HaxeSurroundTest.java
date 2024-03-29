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

import com.intellij.codeInsight.generation.surroundWith.SurroundWithHandler;
import com.intellij.lang.surroundWith.Surrounder;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.ide.surroundWith.*;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.codeStyle.CodeStyleManager;
import org.junit.Test;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeSurroundTest extends HaxeCodeInsightFixtureTestCase {
  @Override
  protected String getBasePath() {
    return "/surroundWith/";
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    setTestStyleSettings();
  }

  protected void doTest(final Surrounder surrounder) throws Exception {
    myFixture.configureByFile(getTestName(false) + ".hx");

    WriteCommandAction.runWriteCommandAction(getProject(), new Runnable() {
      @Override
      public void run() {
        SurroundWithHandler.invoke(getProject(), myFixture.getEditor(), myFixture.getFile(), surrounder);
        PsiDocumentManager.getInstance(getProject()).doPostponedOperationsAndUnblockDocument(myFixture.getDocument(myFixture.getFile()));
        CodeStyleManager.getInstance(myFixture.getProject()).reformat(myFixture.getFile());
      }
    });
      /*CommandProcessor.getInstance().executeCommand(getProject(), new Runnable() {
          @Override
          public void run() {
              SurroundWithHandler.invoke(getProject(), myFixture.getEditor(), myFixture.getFile(), surrounder);
              PsiDocumentManager.getInstance(getProject()).doPostponedOperationsAndUnblockDocument(myFixture.getDocument(myFixture.getFile()));
              CodeStyleManager.getInstance(myFixture.getProject()).reformat(myFixture.getFile());
          }
      }, null, null);*/

    myFixture.checkResultByFile(getTestName(false) + "_after.hx");
  }

  @Test
  public void testIf() throws Exception {
    doTest(new HaxeIfSurrounder());
  }

  @Test
  public void testIfElse() throws Exception {
    doTest(new HaxeIfElseSurrounder());
  }

  @Test
  public void testWhile() throws Exception {
    doTest(new HaxeWhileSurrounder());
  }

  @Test
  public void testDoWhile() throws Exception {
    doTest(new HaxeDoWhileSurrounder());
  }

  @Test
  public void testTryCatch() throws Exception {
    doTest(new HaxeTryCatchSurrounder());
  }
}
