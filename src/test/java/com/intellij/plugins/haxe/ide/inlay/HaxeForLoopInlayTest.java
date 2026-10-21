package com.intellij.plugins.haxe.ide.inlay;

import com.intellij.codeInsight.hints.declarative.InlayHintsProvider;
import com.intellij.plugins.haxe.ide.hint.types.HaxeInlayForLoopHintsProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Inlay hints: for loop")
public class HaxeForLoopInlayTest extends HaxeInlayTestBase {

  InlayHintsProvider hintsProvider = new HaxeInlayForLoopHintsProvider();

  @Override
  protected  String getBasePath() {
    return "/inlay/haxe.loop.type/";
  }

  @Test
  @DisplayName("loop generics")
  public void testLoopGenerics() throws Exception {
    doTest(hintsProvider);
  }


}
