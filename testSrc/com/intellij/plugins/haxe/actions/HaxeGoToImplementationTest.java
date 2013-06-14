package com.intellij.plugins.haxe.actions;

import com.intellij.codeInsight.navigation.GotoImplementationHandler;
import com.intellij.codeInsight.navigation.GotoTargetHandler;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;

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

  public void testGti3() throws Throwable {
    doTest(3);
  }

  public void testGti4() throws Throwable {
    doTest(2);
  }
}
