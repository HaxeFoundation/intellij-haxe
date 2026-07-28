package com.intellij.plugins.haxe.lang.completion;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author: Fedor.Korotkov
 */
@DisplayName("Completion: metadata")
public class MetadataCompletionTest extends HaxeCompletionTestBase {
  public MetadataCompletionTest() {
    super("completion", "metadata");
  }

  @Test
  @DisplayName("null safety")
  public void testNullSafety() throws Throwable {
    doTestInclude("haxe/macro/Compiler.hx");
  }



}
