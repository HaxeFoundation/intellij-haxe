package com.intellij.plugins.haxe.lang.parser.statements;

/**
 * @author fedor.korotkov
 */
public class SwitchTest extends StatementTestBase {
  public SwitchTest() {
    super("switch");
  }

  public void testSimple() throws Throwable {
    doTest(true);
  }
}
