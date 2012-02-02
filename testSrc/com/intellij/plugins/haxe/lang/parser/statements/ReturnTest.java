package com.intellij.plugins.haxe.lang.parser.statements;

/**
 * @author fedor.korotkov
 */
public class ReturnTest extends StatementTestBase {
  public ReturnTest() {
    super("return");
  }

  public void testValue() throws Throwable {
    doTest(true);
  }

  public void testVoid() throws Throwable {
    doTest(true);
  }
}
