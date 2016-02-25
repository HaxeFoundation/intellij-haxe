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

import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeRenameTest extends HaxeCodeInsightFixtureTestCase {
  @Override
  protected String getBasePath() {
    return "/rename/";
  }

  public void doTest(String newName, String... additionalFiles) {
    myFixture.testRename(getTestName(false) + ".hx", getTestName(false) + "After.hx", newName, additionalFiles);
  }

  public void testLocalVariable1() throws Throwable {
    doTest("fooNew");
  }

  public void testLocalVariable2() throws Throwable {
    doTest("fooNew");
  }

  public void testFunctionParameter() throws Throwable {
    doTest("fooNew");
  }

  public void testMethod() throws Throwable {
    doTest("fooNew");
  }

  public void testMainClass() throws Throwable {
    doTest("MainClassAfter");
  }

  public void testStaticField() throws Throwable {
    doTest("fooNew", "additional/StaticFieldHelper.hx");
  }

  public void testStaticMethod() throws Throwable {
    doTest("fooNew", "additional/StaticMethodHelper.hx");
  }

  public void testCatchParameter() throws Throwable {
    doTest("error");
  }
}
