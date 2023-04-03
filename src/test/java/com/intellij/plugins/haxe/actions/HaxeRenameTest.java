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

import com.intellij.openapi.editor.CaretState;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.util.ArrayUtil;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeRenameTest extends HaxeCodeInsightFixtureTestCase {
  @Override
  protected String getBasePath() {
    return "/rename/";
  }

  public void doTest(String newName, String... additionalFiles) {
    myFixture.testRename(getSourceFileName(), getResultFileName(), newName, additionalFiles);
  }

  public void doTestOnNthSelection(int n, String newName, String... additionalFiles) {
    // One-based, for humans. :)
    assertTrue(n > 0);

    myFixture.configureByFiles(ArrayUtil.reverseArray(ArrayUtil.append(additionalFiles, getTruncatedSourceFileName())));

    // Extract the caret info, and then reset it to just the selection/position requested.
    List<CaretState> carets = myFixture.getEditor().getCaretModel().getCaretsAndSelections();
    assertNotEmpty(carets);  // No carets/selections in the source file.
    assertTrue(carets.size() >= n);

    List<CaretState> useCaret = new ArrayList<CaretState>(1);
    useCaret.add(carets.get(n - 1));
    myFixture.getEditor().getCaretModel().setCaretsAndSelections(useCaret);

    myFixture.testRename(getTruncatedResultFileName(), newName);
  }

  private String toSourceName(String name) {
    return name + ".hx";
  }

  private String toResultName(String name) {
    return name + "After.hx";
  }

  private String getSourceFileName() {
    return toSourceName(getTestName(false));
  }

  private String getResultFileName() {
    return toResultName(getTestName(false));
  }

  private String getTruncatedSourceFileName() {
    return toSourceName(getTruncatedTestName());
  }

  private String getTruncatedResultFileName() {
    return toResultName(getTruncatedTestName());
  }

  private String getTruncatedTestName() {
    String testName = getTestName(false);

    int i = testName.length() - 1;
    for (; i > 0 && Character.isDigit(testName.charAt(i)); --i)
      ;

    return testName.substring(0, i + 1);
  }

  @Test
  public void testLocalVariable1() throws Throwable {
    doTest("fooNew");
  }

  @Test
  public void testLocalVariable2() throws Throwable {
    doTest("fooNew");
  }

  @Test
  public void testFunctionParameter() throws Throwable {
    doTest("fooNew");
  }

  @Test
  public void testMethod() throws Throwable {
    doTest("fooNew");
  }

  @Test
  public void testMainClass() throws Throwable {
    doTest("MainClassAfter");
  }

  @Test
  public void testStaticField() throws Throwable {
    doTest("fooNew", "additional/StaticFieldHelper.hx");
  }

  @Test
  public void testStaticMethod() throws Throwable {
    doTest("fooNew", "additional/StaticMethodHelper.hx");
  }

  @Test
  public void testCatchParameter() throws Throwable {
    doTest("error");
  }

  @Test
  public void testForVar() throws Throwable {
    doTest("index");
  }

  @Test
  public void testRenameGenericParam() throws Throwable {
    doTest("P");
  }

  @Test
  public void testDoNotRenameConstructor1() throws Throwable {
    doTestOnNthSelection(1, "After");
  }

  @Test
  public void testDoNotRenameConstructor2() throws Throwable {
    doTestOnNthSelection(2, "After");
  }

  @Test
  public void testDoNotRenameConstructor3() throws Throwable {
    doTestOnNthSelection(3, "After");
  }

  @Test
  public void testDoNotRenameConstructorName() throws Throwable {
    doTest("foo");
  }
}
