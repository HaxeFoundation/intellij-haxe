package com.intellij.plugins.haxe.lang.parser.statements;

/**
 * @author fedor.korotkov
 */
public class ExceptionTest extends StatementTestBase {
  public ExceptionTest() {
    super("exception");
  }

  public void testSimple() throws Throwable {
    doTest(true);
  }

  public void testTryCatch() throws Throwable {
    doTest(true);
  }

  public void testMultipleTryCatch() throws Throwable {
    doTest(true);
  }
}
