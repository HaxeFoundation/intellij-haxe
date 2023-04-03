/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
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
//
// Lifted this file from IDEA 2016.2 for backward-compatibility with IDEA v14, v14.1.
//
package com.intellij.codeInsight;

import com.intellij.plugins.haxe.util.HaxeTestUtils;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.testFramework.LightPlatformCodeInsightTestCase;
import org.jetbrains.annotations.NonNls;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: lesya
 * Date: Sep 21, 2005
 * Time: 11:02:33 PM
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractEnterActionTestCase extends LightPlatformCodeInsightTestCase {
  private static final String TEST_PATH = "/codeInsight/enterAction/";

  @Override
  protected void tearDown() throws Exception {
    CodeStyleSettingsManager.getInstance(getProject()).dropTemporarySettings();
    HaxeTestUtils.cleanupUnexpiredAppleUITimers(this::addSuppressedException);
    super.tearDown();
  }

  protected void doGetIndentTest(final PsiFile file, final int lineNum, final String expected) {
    final int offset = PsiDocumentManager.getInstance(getProject()).getDocument(file).getLineEndOffset(lineNum);
    final String actial = CodeStyleManager.getInstance(getProject()).getLineIndent(file, offset);
    assertEquals(expected, actial);
  }

  protected void doTest() throws Exception {
    doTest("java");
  }

  protected void doTextTest(@NonNls String ext, @NonNls String before, @NonNls String after) throws IOException {
    configureFromFileText("a." + ext, before);
    performAction();
    checkResultByText(null, after, false);
  }

  protected void doTest(final String ext) throws Exception {
    final String name = getTestName(false);

    configureByFile(TEST_PATH + name + "." + ext);
    performAction();
    checkResultByFile(null, TEST_PATH + name + "_after." + ext, false);
  }

  protected void performAction() {
    type('\n');
  }
}
