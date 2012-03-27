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

  public void testPrivateMethod() throws Throwable {
    myFixture.configureByFiles("PrivateMethod.hx", "com/util/ClassFactory.hx");
    doTestVariantsInner("PrivateMethod.txt");
  }

  public void testPublicGetter() throws Throwable {
    myFixture.configureByFiles("PublicGetter.hx", "com/util/ClassFactory.hx");
    doTestVariantsInner("PublicGetter.txt");
  }

  public void testSelfPrivateMethod() throws Throwable {
    doTest();
  }

  public void testStdType1() throws Throwable {
    myFixture.configureByFiles("StdType1.hx", "std/String.hx");
    doTestVariantsInner("StdType1.txt");
  }

  public void testStdType2() throws Throwable {
    myFixture.configureByFiles("StdType2.hx", "std/String.hx", "std/Array.hx");
    doTestVariantsInner("StdType2.txt");
  }
}
