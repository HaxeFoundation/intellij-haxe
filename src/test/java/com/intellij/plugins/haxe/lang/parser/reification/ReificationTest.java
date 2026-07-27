package com.intellij.plugins.haxe.lang.parser.reification;

import com.intellij.plugins.haxe.lang.parser.statements.StatementTestBase;
import org.junit.jupiter.api.Test;

public class ReificationTest extends StatementTestBase {
  public ReificationTest() {
    super("reification");
  }

  @Test
  public void testBasicExpressionReifications() throws Throwable {
    doTest(true);
  }
  @Test
  public void testBasicTypeReifications() throws Throwable {
    doTest(true);
  }
  @Test
  public void testReificationsInLoops() throws Throwable {
    doTest(true);
  }
  @Test
  public void testDeclarationReifications() throws Throwable {
    doTest(true);
  }
}
