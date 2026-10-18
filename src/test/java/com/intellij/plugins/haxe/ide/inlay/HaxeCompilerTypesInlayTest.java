package com.intellij.plugins.haxe.ide.inlay;

import com.intellij.codeInsight.hints.declarative.InlayHintsProvider;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerInlayService;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerTypeInlayProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

/** The compiler provider anchors a seeded answer at every declaration kind it covers and shows nothing without one. */
@DisplayName("Inlay hints: compiler types")
public class HaxeCompilerTypesInlayTest extends HaxeInlayTestBase {

  InlayHintsProvider hintsProvider = new HaxeCompilerTypeInlayProvider();

  @Override
  protected String getBasePath() {
    return "/inlay/haxe.compiler.types/";
  }

  @Test
  @DisplayName("seeded compiler types")
  public void testSeededCompilerTypes() throws Exception {
    HaxeCompilerInlayService service = HaxeCompilerInlayService.getInstance(myFixture.getProject());
    service.useTestTypeTexts(Map.of(
      "count", "Int",
      "run", "String",
      "size", "Int",
      "sum", "Int",
      "local", "Bool",
      "item", "String",
      "k", "String",
      "val", "Int",
      "v", "Int"));
    try {
      doTest(hintsProvider);
    } finally {
      service.useTestTypeTexts(null);
    }
  }
}
