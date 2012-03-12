package com.intellij.plugins.haxe.lang.parser.declarations;

public class FunctionDeclarationTest extends DeclarationTestBase {
  public FunctionDeclarationTest() {
    super("function");
  }

  public void testBadParameters() throws Throwable {
    doTest(true);
  }

  public void testConstructor() throws Throwable {
    doTest(true);
  }

  public void testNoReturnType() throws Throwable {
    doTest(true);
  }

  public void testParameter() throws Throwable {
    doTest(true);
  }

  public void testParameters() throws Throwable {
    doTest(true);
  }

  public void testSimple() throws Throwable {
    doTest(true);
  }

  public void testGeneric() throws Throwable {
    doTest(true);
  }

  public void testMacro() throws Throwable {
    doTest(true);
  }
}
