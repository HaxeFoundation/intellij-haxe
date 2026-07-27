package com.intellij.plugins.haxe.ide.inlay;

import com.intellij.codeInsight.hints.declarative.InlayHintsProvider;
import com.intellij.plugins.haxe.ide.hint.types.HaxeInlayFieldHintsProvider;
import org.junit.jupiter.api.Test;

public class HaxeFieldTypeInlayTest extends HaxeInlayTestBase {

  InlayHintsProvider hintsProvider = new HaxeInlayFieldHintsProvider();

  @Override
  protected  String getBasePath() {
    return "/inlay/haxe.field.type/";
  }

  @Override
  public void setUp() throws Exception {
    useHaxeToolkit();
    super.setUp();
    setTestStyleSettings(2);
  }

  // test to generate preview used in inlay settings example
  @Test
  public void testPreview() throws Exception {
    doTest(hintsProvider);
  }


}
