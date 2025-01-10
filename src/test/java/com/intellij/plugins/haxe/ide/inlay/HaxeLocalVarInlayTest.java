package com.intellij.plugins.haxe.ide.inlay;

import com.intellij.codeInsight.hints.declarative.InlayHintsProvider;
import com.intellij.plugins.haxe.ide.hint.types.HaxeInlayLocalVariableHintsProvider;
import org.junit.Test;

public class HaxeLocalVarInlayTest extends HaxeInlayTestBase {

  InlayHintsProvider hintsProvider = new HaxeInlayLocalVariableHintsProvider();

  @Override
  protected  String getBasePath() {
    return "/inlay/haxe.local.variable/";
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
  public void testSimpleVarHints() throws Exception {
    doTest(hintsProvider);
  }

  @Test
  public void testComplexVarHints() throws Exception {
    doTest(hintsProvider);
  }

  @Test
  public void testLocalVarMacros() throws Exception {
    doTest(hintsProvider);
  }
  @Test
  public void testOptionalFieldsHints() throws Exception {
    doTest(hintsProvider);
  }


}
