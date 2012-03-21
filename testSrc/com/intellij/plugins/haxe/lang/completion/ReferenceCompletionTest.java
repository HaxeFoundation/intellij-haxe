package com.intellij.plugins.haxe.lang.completion;

/**
 * @author: Fedor.Korotkov
 */
public class ReferenceCompletionTest extends HaxeCompletionTestBase {
  public ReferenceCompletionTest() {
    super("completion", "references");
  }

  public void testTest1() throws Throwable {
    doTest();
  }

  public void testTest2() throws Throwable {
    doTest();
  }

  public void testTest3() throws Throwable {
    doTest();
  }

  public void testTest4() throws Throwable {
    doTest();
  }

  public void testTest5() throws Throwable {
    doTest();
  }

  public void testTest6() throws Throwable {
    doTest();
  }

  public void testTest7() throws Throwable {
    doTest();
  }

  public void testSelfMethod() throws Throwable {
    doTest();
  }

  public void testThisMembers() throws Throwable {
    doTest();
  }

  public void testClassName() throws Throwable {
    myFixture.configureByFiles("ClassName.hx", "com/util/ClassFactory.hx");
    doTestVariantsInner("ClassName.txt");
  }
}
