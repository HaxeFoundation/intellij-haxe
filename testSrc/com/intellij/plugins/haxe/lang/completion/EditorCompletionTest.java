package com.intellij.plugins.haxe.lang.completion;

public class EditorCompletionTest extends HaxeCompletionTestBase {
  public EditorCompletionTest() {
    super("completion", "editor");
  }

  public void testGenericBrace1() {
    doTest('<');
  }

  public void testGenericBrace2() {
    doTest('<');
  }

  public void testGenericBrace3() {
    doTest('<');
  }

  public void testLess() {
    doTest('<');
  }
}
