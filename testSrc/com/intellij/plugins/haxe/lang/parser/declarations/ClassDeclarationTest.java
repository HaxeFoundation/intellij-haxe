package com.intellij.plugins.haxe.lang.parser.declarations;

public class ClassDeclarationTest extends DeclarationTestBase {
  public ClassDeclarationTest() {
    super("class");
  }

  public void testExtends() throws Throwable {
    doTest(true);
  }

  public void testExtendsImplements() throws Throwable {
    doTest(true);
  }

  public void testImplements() throws Throwable {
    doTest(true);
  }

  public void testMultiextends() throws Throwable {
    doTest(true);
  }

  public void testSimple() throws Throwable {
    doTest(true);
  }

  public void testConstraint() throws Throwable {
    doTest(true);
  }

  public void testNativeRandom() throws Throwable {
    doTest(true);
  }
}
