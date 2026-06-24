package com.intellij.plugins.haxe.ide.inlay.semantic;

import com.intellij.codeInsight.hints.declarative.InlayHintsProvider;
import com.intellij.plugins.haxe.ide.hint.types.HaxeInlayLocalVariableHintsProvider;
import com.intellij.plugins.haxe.ide.inlay.HaxeInlayTestBase;
import org.junit.Test;

/**
 * Tests that use inlay to verify type evaluations without beeing affected by assign hints.
 */
public class HaxeInlayForSemanticTest extends HaxeInlayTestBase {

  InlayHintsProvider hintsProvider = new HaxeInlayLocalVariableHintsProvider();

  @Override
  protected  String getBasePath() {
    return "/inlay/semantic/";
  }

  @Override
  public void setUp() throws Exception {
    useHaxeToolkit();
    super.setUp();
    setTestStyleSettings(2);
  }

  // test to generate preview used in inlay settings example
  @Test
  public void testOperatorDynamicTest() throws Exception {
    doTest(hintsProvider);
  }


}
