package com.intellij.plugins.haxe.ide.inlay;

import com.intellij.codeInsight.hints.declarative.InlayHintsProvider;
import com.intellij.plugins.haxe.ide.hint.types.HaxeInlayCaptureVariableHintsProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Inlay hints: capture variable")
public class HaxeCaptureVariableInlayTest extends HaxeInlayTestBase {

  InlayHintsProvider hintsProvider = new HaxeInlayCaptureVariableHintsProvider();

  @Override
  protected  String getBasePath() {
    return "/inlay/haxe.capture.variable/";
  }

  @Override
  public void setUp() throws Exception {
    useHaxeToolkit();
    super.setUp();
    setTestStyleSettings(2);
  }

  // test to generate preview used in inlay settings example
  @Test
  @DisplayName("preview")
  public void testPreview() throws Exception {
    doTest(hintsProvider);
  }


}
