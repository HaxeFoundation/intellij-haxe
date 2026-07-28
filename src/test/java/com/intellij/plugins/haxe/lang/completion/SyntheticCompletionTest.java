package com.intellij.plugins.haxe.lang.completion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Completion: synthetic")
public class SyntheticCompletionTest extends HaxeCompletionTestBase {
  public SyntheticCompletionTest() {
    super("completion", "synthetic");
  }

  @Test
  @DisplayName("bind on function type")
  public void testBindOnFunctionType() throws Throwable {
    doTest();
  }

  @Test
  @DisplayName("bind on method")
  public void testBindOnMethod() throws Throwable {
    doTest();
  }

  @Test
  @DisplayName("code on string literal")
  public void testCodeOnStringLiteral() throws Throwable {
    doTest();
  }

}
