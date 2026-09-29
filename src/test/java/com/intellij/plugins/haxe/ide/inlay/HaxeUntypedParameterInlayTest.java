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

  // The following fixtures encode the compiler's monomorph-binding order:
  // body usage first, call-site argument types only for what the body leaves open.

  @Test
  @DisplayName("call site binds unused parameter")
  public void testCallSiteBindsUnusedParameter() throws Exception {
    doTest(hintsProvider);
  }

  @Test
  @DisplayName("body binds before call site")
  public void testBodyBindsBeforeCallSite() throws Exception {
    doTest(hintsProvider);
  }

  @Test
  @DisplayName("self recursive only stays unknown")
  public void testSelfRecursiveOnlyStaysUnknown() throws Exception {
    doTest(hintsProvider);
  }

  @Test
  @DisplayName("generic argument not informative")
  public void testGenericArgumentNotInformative() throws Exception {
    doTest(hintsProvider);
  }

  @Test
  @DisplayName("chained call site binding")
  public void testChainedCallSiteBinding() throws Exception {
    doTest(hintsProvider);
  }

  @Test
  @DisplayName("lambda argument typed by bound type parameter")
  public void testLambdaArgumentTypedByBoundTypeParameter() throws Exception {
    doTest(hintsProvider);
  }

  @Test
  @DisplayName("deep chain exhausts in flight budget")
  public void testDeepChainExhaustsInFlightBudget() throws Exception {
    doTest(hintsProvider);
  }

  @Test
  @DisplayName("call site answer translated to own type parameter")
  public void testCallSiteAnswerTranslatedToOwnTypeParameter() throws Exception {
    doTest(hintsProvider);
  }

  @Test
  @DisplayName("hole evaluation recursion same argument twice")
  public void testHoleEvaluationRecursionSameArgumentTwice() throws Exception {
    doTest(hintsProvider);
  }

  @Test
  @DisplayName("generic parameter binds hole from other argument")
  public void testGenericParameterBindsHoleFromOtherArgument() throws Exception {
    doTest(hintsProvider);
  }
}
