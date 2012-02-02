package com.intellij.plugins.haxe.lang.parser.statements;

/**
 * @author fedor.korotkov
 */
public class BreakTest extends StatementTestBase {
  public BreakTest() {
    super("break");
  }

  public void testSimple() throws Throwable {
    doTest(true);
  }
}
