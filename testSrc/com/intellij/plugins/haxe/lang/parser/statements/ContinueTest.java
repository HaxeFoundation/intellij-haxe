package com.intellij.plugins.haxe.lang.parser.statements;

/**
 * @author fedor.korotkov
 */
public class ContinueTest extends StatementTestBase {
  public ContinueTest() {
    super("continue");
  }

  public void testSimple() throws Throwable {
    doTest(true);
  }
}
