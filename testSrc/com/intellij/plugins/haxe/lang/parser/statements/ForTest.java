package com.intellij.plugins.haxe.lang.parser.statements;

/**
 * @author fedor.korotkov
 */
public class ForTest extends StatementTestBase {
  public ForTest() {
    super("for");
  }

  public void testSimple() throws Throwable {
    doTest(true);
  }
}
