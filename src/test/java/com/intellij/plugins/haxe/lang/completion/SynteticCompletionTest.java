package com.intellij.plugins.haxe.lang.completion;

import org.junit.Test;

public class SynteticCompletionTest extends HaxeCompletionTestBase {
  public SynteticCompletionTest() {
    super("completion", "synthetic");
  }

  @Test
  public void testBindOnFunctionType() throws Throwable {
    doTest();
  }

  @Test
  public void testBindOnMethod() throws Throwable {
    doTest();
  }

  @Test
  public void testCodeOnStringLiteral() throws Throwable {
    doTest();
  }

}
