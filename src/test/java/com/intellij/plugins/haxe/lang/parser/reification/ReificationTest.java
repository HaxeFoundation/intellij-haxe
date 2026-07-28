package com.intellij.plugins.haxe.lang.parser.reification;

import com.intellij.plugins.haxe.lang.parser.statements.StatementTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Parsing: reification")
public class ReificationTest extends StatementTestBase {
  public ReificationTest() {
    super("reification");
  }

  @Test
  @DisplayName("basic expression reifications")
  public void testBasicExpressionReifications() throws Throwable {
    doTest(true);
  }
  @Test
  @DisplayName("basic type reifications")
  public void testBasicTypeReifications() throws Throwable {
    doTest(true);
  }
  @Test
  @DisplayName("reifications in loops")
  public void testReificationsInLoops() throws Throwable {
    doTest(true);
  }
  @Test
  @DisplayName("declaration reifications")
  public void testDeclarationReifications() throws Throwable {
    doTest(true);
  }
}
