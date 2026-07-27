package com.intellij.plugins.haxe.ide.inlay;

import com.intellij.codeInsight.hints.declarative.InlayHintsProvider;
import com.intellij.plugins.haxe.ide.hint.types.HaxeInlayReturnTypeHintsProvider;
import org.junit.jupiter.api.Test;

public class HaxeReturnTypeInlayTest extends HaxeInlayTestBase {

  InlayHintsProvider hintsProvider = new HaxeInlayReturnTypeHintsProvider();

  @Override
  protected  String getBasePath() {
    return "/inlay/haxe.return.type/";
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

  @Test
  public void testReturnTypeGenerics() throws Exception {
    doTest(hintsProvider);
  }
  @Test
  public void testReturnTypeMacros() throws Exception {
    doTest(hintsProvider);
  }
  @Test
  public void testReturnTypeHints() throws Exception {
    doTest(hintsProvider);
  }


}
