package com.intellij.plugins.haxe.lang.parser.declarations;

public class VarOrConstDeclarationTest extends DeclarationTestBase {
  public VarOrConstDeclarationTest() {
    super("variable");
  }

  public void testAssign() throws Throwable {
    doTest(true);
  }

  public void testAssignArray() throws Throwable {
    doTest(true);
  }

  public void testBoolExpression() throws Throwable {
    doTest(true);
  }

  public void testExpression() throws Throwable {
    doTest(true);
  }

  public void testSimple() throws Throwable {
    doTest(true);
  }

  public void testTemplate() throws Throwable {
    doTest(true);
  }

  public void testConstants() throws Throwable {
    doTest(true);
  }
}
