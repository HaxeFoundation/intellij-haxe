/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.plugins.haxe.actions;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.lang.CodeInsightActions;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.HaxeLanguage;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeGoToSuperTest extends HaxeCodeInsightFixtureTestCase {
  @Override
  protected String getBasePath() {
    return "/gotoSuper/";
  }

  private void doTest() throws Throwable {
    myFixture.configureByFile(getTestName(false) + ".hx");
    final CodeInsightActionHandler handler = CodeInsightActions.GOTO_SUPER.forLanguage(HaxeLanguage.INSTANCE);
    handler.invoke(getProject(), myFixture.getEditor(), myFixture.getFile());
    myFixture.checkResultByFile(getTestName(false) + ".txt");
  }

  public void testGts1() throws Throwable {
    doTest();
  }

  public void testGts2() throws Throwable {
    doTest();
  }
}
