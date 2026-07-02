package com.intellij.plugins.haxe.lang.completion;


import org.junit.Test;

/**
 * @author: Fedor.Korotkov
 */
public class MetadataCompletionTest extends HaxeCompletionTestBase {
  public MetadataCompletionTest() {
    super("completion", "metadata");
  }

  @Test
  public void testNullSafety() throws Throwable {
    doTestInclude("haxe/macro/Compiler.hx");
  }



}
