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

  public void testLocalVariable() throws Throwable {
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
}
