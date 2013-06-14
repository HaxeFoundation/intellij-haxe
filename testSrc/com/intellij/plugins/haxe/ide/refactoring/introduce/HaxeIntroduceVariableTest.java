package com.intellij.plugins.haxe.ide.refactoring.introduce;

import com.intellij.plugins.haxe.lang.psi.HaxeCallExpression;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeIntroduceVariableTest extends HaxeIntroduceTestBase {
  @Override
  protected String getBasePath() {
    return "/refactoring/introduceVariable/";
  }

  @Override
  protected HaxeIntroduceHandler createHandler() {
    return new HaxeIntroduceVariableHandler();
  }

  public void testAfterStatement() throws Throwable {
    doTest();
  }

  public void testAlone() throws Throwable {
    doTest();
  }

  public void testReplaceAll1() throws Throwable {
    doTest();
  }

  public void testReplaceAll2() throws Throwable {
    doTest();
  }

  public void testReplaceAll3() throws Throwable {
    doTestInplace(null);
  }

  public void testReplaceOne1() throws Throwable {
    doTest(null, false);
  }

  public void testSuggestName1() throws Throwable {
    doTestSuggestions(HaxeCallExpression.class, "test");
  }

  public void testSuggestName2() throws Throwable {
    doTestSuggestions(HaxeCallExpression.class, "test1");
  }
}
