package com.intellij.plugins.haxe.ide.inlay;

import com.intellij.codeInsight.hints.declarative.InlayHintsProvider;
import com.intellij.plugins.haxe.ide.hint.types.HaxeInlayUntypedParameterHintsProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Inlay hints: untyped parameter")
public class HaxeUntypedParameterInlayTest extends HaxeInlayTestBase {

  InlayHintsProvider hintsProvider = new HaxeInlayUntypedParameterHintsProvider();

  @Override
  protected  String getBasePath() {
    return "/inlay/haxe.untyped.parameter.type/";
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
  @Test
  @DisplayName("untyped with generics")
  public void testUntypedWithGenerics() throws Exception {
    doTest(hintsProvider);
  }
  @Test
  @DisplayName("dynamic method assign inlay")
  public void testDynamicMethodAssignInlay() throws Exception {
    doTest(hintsProvider);
  }
  @Test
  @DisplayName("untyped with recursive constraint generics")
  public void testUntypedWithRecursiveConstraintGenerics() throws Exception {
    doTest(hintsProvider);
  }


}
