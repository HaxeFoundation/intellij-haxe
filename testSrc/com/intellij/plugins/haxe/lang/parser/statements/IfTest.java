package com.intellij.plugins.haxe.lang.parser.statements;

public class IfTest extends StatementTestBase {
  public IfTest() {
    super("if");
  }

  public void testSimple() throws Throwable {
    doTest(true);
  }

  public void testIfElse() throws Throwable {
    doTest(true);
  }

  public void testMultipleIfElse() throws Throwable {
    doTest(true);
  }

  public void testCondition() throws Throwable {
    doTest(true);
  }

  public void testConditionalCompilation() throws Throwable {
    doTest(true);
  }
}
