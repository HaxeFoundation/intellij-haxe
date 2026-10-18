package com.intellij.plugins.haxe.ide.inlay;

import com.intellij.codeInsight.hints.declarative.InlayHintsProvider;
import com.intellij.plugins.haxe.ide.hint.types.HaxeInlayLocalVariableHintsProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Inlay hints: local var")
public class HaxeLocalVarInlayTest extends HaxeInlayTestBase {

  InlayHintsProvider hintsProvider = new HaxeInlayLocalVariableHintsProvider();

  @Override
  protected  String getBasePath() {
    return "/inlay/haxe.local.variable/";
  }

  @Test
  @DisplayName("simple var hints")
  public void testSimpleVarHints() throws Exception {
    doTest(hintsProvider);
  }

  @Test
  @DisplayName("complex var hints")
  public void testComplexVarHints() throws Exception {
    doTest(hintsProvider);
  }

  @Test
  @DisplayName("local var macros")
  public void testLocalVarMacros() throws Exception {
    doTest(hintsProvider);
  }
  @Test
  @DisplayName("optional fields hints")
  public void testOptionalFieldsHints() throws Exception {
    doTest(hintsProvider);
  }

  @Test
  @DisplayName("complex monomorph hints")
  public void testComplexMonomorphHints() throws Exception {
    doTest(hintsProvider);
  }

  @Test
  @DisplayName("value expression hints")
  public void testValueExpressionHints() throws Exception {
    doTest(hintsProvider);
  }

  @Test
  @DisplayName("type from usage hints")
  public void testTypeFromUsageHints() throws Exception {
    doTest(hintsProvider);
  }

  @Test
  @DisplayName("abstract forward hints")
  public void testAbstractForwardHints() throws Exception {
    doTest(hintsProvider);
  }

  @Test
  @DisplayName("local var from untyped parameters")
  public void testLocalVarFromUntypedParameters() throws Exception {
    doTest(hintsProvider);
  }
}
