package com.intellij.plugins.haxe.actions;

import com.intellij.codeInsight.navigation.GotoImplementationHandler;
import com.intellij.codeInsight.navigation.GotoTargetHandler;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeGoToImplementationTest extends JavaCodeInsightFixtureTestCase {
  @Override
  protected String getTestDataPath() {
    return PathManager.getHomePath() + FileUtil.toSystemDependentName("/plugins/haxe/testData/gotoImplementation/");
  }

  private void doTest(int expectedLength) throws Throwable {
    myFixture.configureByFile(getTestName(false) + ".hx");
    final GotoTargetHandler.GotoData data =
      new GotoImplementationHandler().getSourceAndTargetElements(myFixture.getEditor(), myFixture.getFile());
    assertNotNull(myFixture.getFile().toString(), data);
    assertEquals(expectedLength, data.targets.length);
  }

  public void testGti1() throws Throwable {
    doTest(2);
  }

  public void testGti2() throws Throwable {
    doTest(1);
  }
}
