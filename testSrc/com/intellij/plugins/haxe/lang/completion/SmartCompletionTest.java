package com.intellij.plugins.haxe.lang.completion;

/**
 * @author: Fedor.Korotkov
 */
public class SmartCompletionTest extends HaxeCompletionTestBase {
  public SmartCompletionTest() {
    super("completion", "smart");
  }

  public void testEnum1() throws Throwable {
    doTest();
  }
}
