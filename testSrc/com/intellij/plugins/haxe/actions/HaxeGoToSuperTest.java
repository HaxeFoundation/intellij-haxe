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
