package com.intellij.plugins.haxe.ide;

import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevel;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevelUtil;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevel.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Language-level annotations, one fixture per test: each file under
 * testData/annotation.languagelevel carries the expected error/warning markup
 * for the level its test selects. Covers gating of level-dependent semantics,
 * "requires Haxe X" for newer syntax and "removed in Haxe X" for retired
 * constructs.
 */
@DisplayName("Annotation: language level features")
public class HaxeLanguageLevelGatingTest extends HaxeSemanticAnnotatorTestBase {

  @Override
  public void tearDown() throws Exception {
    // project-level setting survives into sibling tests otherwise
    setLevel(HaxeLanguageLevel.latest());
    super.tearDown();
  }

  @Override
  protected String getBasePath() {
    return "/annotation.languagelevel/";
  }

  private void setLevel(HaxeLanguageLevel level) {
    HaxeCompilerSettings.getInstance(myFixture.getProject()).setDefaultLanguageLevel(level);
  }

  private void doTestAtLevel(HaxeLanguageLevel level) throws Exception {
    setLevel(level);
    // pre-4.2 `is` semantics are driven by the LEVEL; the 4.1-compat
    // inspection option defaults to off
    doTestSkippingInspections(null);
  }

  private void doFixTestAtLevel(HaxeLanguageLevel level, String fixText) throws Exception {
    setLevel(level);
    doTestActions(fixText);
  }

  // ---- 3.4 ----

  @Test
  @DisplayName("final keyword below 4.0")
  public void testFinalKeywordBelow40() throws Exception {
    doTestAtLevel(HAXE_3_4);
  }

  @Test
  @DisplayName("arrow function below 4.0")
  public void testArrowFunctionBelow40() throws Exception {
    doTestAtLevel(HAXE_3_4);
  }

  @Test
  @DisplayName("enum abstract below 4.0")
  public void testEnumAbstractBelow40() throws Exception {
    doTestAtLevel(HAXE_3_4);
  }

  @Test
  @DisplayName("custom accessor names below 4.0")
  public void testCustomAccessorNamesBelow40() throws Exception {
    doTestAtLevel(HAXE_3_4);
  }

  // ---- 4.0 ----

  @Test
  @DisplayName("final keyword at 4.0")
  public void testFinalKeywordAt40() throws Exception {
    doTestAtLevel(HAXE_4_0);
  }

  @Test
  @DisplayName("arrow function at 4.0")
  public void testArrowFunctionAt40() throws Exception {
    doTestAtLevel(HAXE_4_0);
  }

  @Test
  @DisplayName("enum abstract at 4.0")
  public void testEnumAbstractAt40() throws Exception {
    doTestAtLevel(HAXE_4_0);
  }

  @Test
  @DisplayName("custom accessor names removed at 4.0")
  public void testCustomAccessorNamesRemovedAt40() throws Exception {
    doTestAtLevel(HAXE_4_0);
  }

  @Test
  @DisplayName("untyped catch below 4.1")
  public void testUntypedCatchBelow41() throws Exception {
    doTestAtLevel(HAXE_4_0);
  }

  @Test
  @DisplayName("Std.is not deprecated below 4.1")
  public void testStdIsNotDeprecatedBelow41() throws Exception {
    doTestAtLevel(HAXE_4_0);
  }

  // ---- 4.1 ----

  @Test
  @DisplayName("untyped catch at 4.1")
  public void testUntypedCatchAt41() throws Exception {
    doTestAtLevel(HAXE_4_1);
  }

  @Test
  @DisplayName("abstract method below 4.2")
  public void testAbstractMethodBelow42() throws Exception {
    doTestAtLevel(HAXE_4_1);
  }

  @Test
  @DisplayName("abstract modifier below 4.2")
  public void testAbstractModifierBelow42() throws Exception {
    doTestAtLevel(HAXE_4_1);
  }

  @Test
  @DisplayName("module level function below 4.2")
  public void testModuleLevelFunctionBelow42() throws Exception {
    doTestAtLevel(HAXE_4_1);
  }

  @Test
  @DisplayName("is expression pre 4.2 semantics")
  public void testIsExpressionPre42Semantics() throws Exception {
    doTestAtLevel(HAXE_4_1);
  }

  @Test
  @DisplayName("Std.is deprecated at 4.1")
  public void testStdIsDeprecatedAt41() throws Exception {
    doTestAtLevel(HAXE_4_1);
  }

  // ---- 4.2 ----

  @Test
  @DisplayName("abstract method in non abstract class at 4.2")
  public void testAbstractMethodInNonAbstractClassAt42() throws Exception {
    doTestAtLevel(HAXE_4_2);
  }

  @Test
  @DisplayName("abstract class at 4.2")
  public void testAbstractClassAt42() throws Exception {
    doTestAtLevel(HAXE_4_2);
  }

  @Test
  @DisplayName("module level function at 4.2")
  public void testModuleLevelFunctionAt42() throws Exception {
    doTestAtLevel(HAXE_4_2);
  }

  @Test
  @DisplayName("is expression at 4.2")
  public void testIsExpressionAt42() throws Exception {
    doTestAtLevel(HAXE_4_2);
  }

  @Test
  @DisplayName("null coalescing below 4.3")
  public void testNullCoalescingBelow43() throws Exception {
    doTestAtLevel(HAXE_4_2);
  }

  @Test
  @DisplayName("default type parameters below 4.3")
  public void testDefaultTypeParametersBelow43() throws Exception {
    doTestAtLevel(HAXE_4_2);
  }

  @Test
  @DisplayName("safe navigation below 4.3")
  public void testSafeNavigationBelow43() throws Exception {
    doTestAtLevel(HAXE_4_2);
  }

  // ---- 4.3 ----

  @Test
  @DisplayName("null coalescing at 4.3")
  public void testNullCoalescingAt43() throws Exception {
    doTestAtLevel(HAXE_4_3);
  }

  @Test
  @DisplayName("default type parameters at 4.3")
  public void testDefaultTypeParametersAt43() throws Exception {
    doTestAtLevel(HAXE_4_3);
  }

  @Test
  @DisplayName("safe navigation at 4.3")
  public void testSafeNavigationAt43() throws Exception {
    doTestAtLevel(HAXE_4_3);
  }

  @Test
  @DisplayName("binary literals warn below 5.0")
  public void testBinaryLiteralsWarnBelow50() throws Exception {
    doTestAtLevel(HAXE_4_3);
  }

  // ---- 5.0 ----

  @Test
  @DisplayName("binary literals at 5.0")
  public void testBinaryLiteralsAt50() throws Exception {
    doTestAtLevel(HAXE_5_0);
  }

  // ---- conditional-compilation version (haxe_ver define source) ----

  @Test
  @DisplayName("conditional haxe version follows the language level")
  public void testConditionalHaxeVersionFollowsTheLanguageLevel() {
    setLevel(HAXE_4_1);
    assertEquals("4.1.0", HaxeLanguageLevelUtil.getHaxeVersion(myFixture.getProject(), null));
  }

  @Test
  @DisplayName("conditional haxe version falls back to the level without an sdk")
  public void testConditionalHaxeVersionFallsBackToTheLevelWithoutAnSdk() {
    HaxeCompilerSettings settings = HaxeCompilerSettings.getInstance(myFixture.getProject());
    settings.setUseLanguageLevelForConditionals(false);
    try {
      setLevel(HAXE_4_2);
      // the light project registers no haxe SDK, so the level is the fallback
      assertEquals("4.2.0", HaxeLanguageLevelUtil.getHaxeVersion(myFixture.getProject(), null));
    }
    finally {
      settings.setUseLanguageLevelForConditionals(true);
    }
  }

  // ---- syntax migrations: old metadata forms vs their 4.0 keywords ----

  @Test
  @DisplayName("enum abstract meta not deprecated below 4.0")
  public void testEnumAbstractMetaBelow40() throws Exception {
    doTestAtLevel(HAXE_3_4);
  }

  @Test
  @DisplayName("enum abstract meta deprecated at 4.0")
  public void testEnumAbstractMetaDeprecatedAt40() throws Exception {
    doTestAtLevel(HAXE_4_0);
  }

  @Test
  @DisplayName("final meta deprecated at 4.0")
  public void testFinalMetaDeprecatedAt40() throws Exception {
    doTestAtLevel(HAXE_4_0);
  }

  @Test
  @DisplayName("extern meta deprecated at 4.0")
  public void testExternMetaDeprecatedAt40() throws Exception {
    doTestAtLevel(HAXE_4_0);
  }

  @Test
  @DisplayName("extern field modifier below 4.0")
  public void testExternFieldModifierBelow40() throws Exception {
    doTestAtLevel(HAXE_3_4);
  }

  @Test
  @DisplayName("extern field modifier at 4.0")
  public void testExternFieldModifierAt40() throws Exception {
    doTestAtLevel(HAXE_4_0);
  }

  // ---- syntax migrations: renamed std APIs ----

  @Test
  @DisplayName("Std.isOfType below 4.1")
  public void testStdIsOfTypeBelow41() throws Exception {
    doTestAtLevel(HAXE_4_0);
  }

  @Test
  @DisplayName("untyped __js__ deprecated at 4.0")
  public void testUntypedJsDeprecatedAt40() throws Exception {
    doTestAtLevel(HAXE_4_0);
  }

  // ---- syntax migration quick fixes, both directions ----

  @Test
  @DisplayName("fix enum abstract meta to keyword")
  public void testFixEnumAbstractMetaToKeyword() throws Exception {
    doFixTestAtLevel(HAXE_4_0, "Replace with enum abstract");
  }

  @Test
  @DisplayName("fix enum abstract to meta")
  public void testFixEnumAbstractToMeta() throws Exception {
    doFixTestAtLevel(HAXE_3_4, "Replace with @:enum");
  }

  @Test
  @DisplayName("fix final field to var")
  public void testFixFinalFieldToVar() throws Exception {
    doFixTestAtLevel(HAXE_3_4, "Replace with var");
  }

  @Test
  @DisplayName("fix final method to meta")
  public void testFixFinalMethodToMeta() throws Exception {
    doFixTestAtLevel(HAXE_3_4, "Replace with @:final");
  }

  @Test
  @DisplayName("fix final meta to keyword")
  public void testFixFinalMetaToKeyword() throws Exception {
    doFixTestAtLevel(HAXE_4_0, "Replace with final");
  }

  @Test
  @DisplayName("fix extern modifier to meta")
  public void testFixExternModifierToMeta() throws Exception {
    doFixTestAtLevel(HAXE_3_4, "Replace with @:extern");
  }

  @Test
  @DisplayName("fix extern meta to keyword")
  public void testFixExternMetaToKeyword() throws Exception {
    doFixTestAtLevel(HAXE_4_0, "Replace with extern");
  }

  @Test
  @DisplayName("fix Std.is to Std.isOfType")
  public void testFixStdIsToIsOfType() throws Exception {
    doFixTestAtLevel(HAXE_4_3, "Replace with Std.isOfType");
  }

  @Test
  @DisplayName("fix Std.isOfType to Std.is")
  public void testFixStdIsOfTypeToStdIs() throws Exception {
    doFixTestAtLevel(HAXE_3_4, "Replace with Std.is");
  }

  @Test
  @DisplayName("fix untyped js to Syntax.code")
  public void testFixUntypedJsToSyntaxCode() throws Exception {
    doFixTestAtLevel(HAXE_4_0, "Replace with js.Syntax.code");
  }

  @Test
  @DisplayName("fix js Syntax.code to untyped")
  public void testFixJsSyntaxCodeToUntyped() throws Exception {
    doFixTestAtLevel(HAXE_3_4, "Replace with untyped __js__");
  }
}
