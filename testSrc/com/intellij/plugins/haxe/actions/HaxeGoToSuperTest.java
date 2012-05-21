package com.intellij.plugins.haxe.actions;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.lang.CodeInsightActions;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeGoToSuperTest extends JavaCodeInsightFixtureTestCase {
  @Override
  protected String getTestDataPath() {
    return PathManager.getHomePath() + FileUtil.toSystemDependentName("/plugins/haxe/testData/gotoSuper/");
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
