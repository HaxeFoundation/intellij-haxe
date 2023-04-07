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
package com.intellij.plugins.haxe.actions;

import com.intellij.codeInsight.navigation.GotoTargetHandler;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.testFramework.fixtures.CodeInsightTestUtil;
import org.junit.Test;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeGoToImplementationTest extends HaxeCodeInsightFixtureTestCase {
  @Override
  protected String getBasePath() {
    return "/gotoImplementation/";
  }

  private void doTest(int expectedLength) throws Throwable {
    myFixture.configureByFile(getTestName(false) + ".hx");
    GotoTargetHandler.GotoData data = CodeInsightTestUtil.gotoImplementation(myFixture.getEditor(), myFixture.getFile());

    assertNotNull(myFixture.getFile().toString(), data);
    // TODO: listen updater task?
    assertEquals(expectedLength, data.targets.length);
  }

  @Test
  public void testGti1() throws Throwable {
    doTest(2);
  }

  @Test
  public void testGti2() throws Throwable {
    doTest(1);
  }

  @Test
  public void testGti3() throws Throwable {
    doTest(2);
  }

  @Test
  public void testGti4() throws Throwable {
    doTest(2);
  }
}
