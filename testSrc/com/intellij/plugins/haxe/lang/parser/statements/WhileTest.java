package com.intellij.plugins.haxe.lang.parser.statements;

public class WhileTest extends StatementTestBase {
  public WhileTest() {
    super("while");
  }

  public void testSimple() throws Throwable {
    doTest(true);
  }

  public void testCondition() throws Throwable {
    doTest(true);
  }

  public void testDo() throws Throwable {
    doTest(true);
  }
}
