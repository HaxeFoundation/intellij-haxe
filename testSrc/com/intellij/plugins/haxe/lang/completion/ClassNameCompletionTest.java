package com.intellij.plugins.haxe.lang.completion;

/**
 * @author: Fedor.Korotkov
 */
public class ClassNameCompletionTest extends HaxeCompletionTestBase {
  public ClassNameCompletionTest() {
    super("completion", "types");
  }

  @Override
  protected void doTest() throws Throwable {
    myFixture.configureByFiles(getTestName(false) + ".hx", "com/Foo.hx", "com/bar/Foo.hx", "com/bar/IBar.hx");
    doTestVariantsInner(getTestName(false) + ".txt");
  }

  public void testExtends() throws Throwable {
    doTest();
  }

  public void testImplements() throws Throwable {
    doTest();
  }

  public void testMethod() throws Throwable {
    doTest();
  }

  public void testTypeParameter() throws Throwable {
    doTest();
  }

  public void testClassHelper() throws Throwable {
    doTest();
  }
}
