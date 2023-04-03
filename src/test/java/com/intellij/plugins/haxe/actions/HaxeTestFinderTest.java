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
package com.intellij.plugins.haxe.actions;

import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.ide.HaxeTestFinder;
import com.intellij.psi.PsiFile;
import org.junit.Test;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeTestFinderTest extends HaxeCodeInsightFixtureTestCase {
  private HaxeTestFinder myTestFinder = null;

  @Override
  protected String getBasePath() {
    return "/testFinder/";
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myTestFinder = new HaxeTestFinder();
  }

  private void doFindTestsTest(int i) {
    doFindTestsTest(myFixture.configureByFiles(getTestName(false) + "." + HaxeFileType.DEFAULT_EXTENSION), 1);
  }

  private void doFindTestsTest(PsiFile[] files, int size) {
    assertEquals(size, myTestFinder.findTestsForClass(myFixture.getElementAtCaret()).size());
  }

  private void doFindClassesTest(int i) {
    doFindClassesTest(myFixture.configureByFiles(getTestName(false) + "." + HaxeFileType.DEFAULT_EXTENSION), 1);
  }

  private void doFindClassesTest(PsiFile[] files, int size) {
    assertEquals(size, myTestFinder.findClassesForTest(myFixture.getElementAtCaret()).size());
  }

  @Test
  public void testFoo1() throws Throwable {
    doFindTestsTest(1);
  }

  @Test
  public void testFoo2() throws Throwable {
    doFindTestsTest(1);
  }

  @Test
  public void testFoo3() throws Throwable {
    doFindTestsTest(myFixture.configureByFiles(
      "Foo3.hx",
      "test/Foo3Test.hx",
      "test/TestFoo3.hx"
    ), 2);
  }

  @Test
  public void testFoo4Test() throws Throwable {
    doFindClassesTest(1);
  }

  @Test
  public void testFoo5Test() throws Throwable {
    doFindClassesTest(1);
  }
}
