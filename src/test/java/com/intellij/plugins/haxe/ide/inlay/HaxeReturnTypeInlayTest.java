package com.intellij.plugins.haxe.ide.inlay;

import com.intellij.codeInsight.hints.declarative.InlayHintsProvider;
import com.intellij.plugins.haxe.ide.hint.types.HaxeInlayReturnTypeHintsProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Inlay hints: return type")
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
  @DisplayName("preview")
  public void testPreview() throws Exception {
    doTest(hintsProvider);
  }

  @Test
  @DisplayName("return type generics")
  public void testReturnTypeGenerics() throws Exception {
    doTest(hintsProvider);
  }
  @Test
  @DisplayName("return type macros")
  public void testReturnTypeMacros() throws Exception {
    doTest(hintsProvider);
  }
  @Test
  @DisplayName("return type hints")
  public void testReturnTypeHints() throws Exception {
    doTest(hintsProvider);
  }

  @Test
  @DisplayName("return type from untyped parameters")
  public void testReturnTypeFromUntypedParameters() throws Exception {
    doTest(hintsProvider);
  }
}
