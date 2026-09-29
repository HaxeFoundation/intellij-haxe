package com.intellij.plugins.haxe.v2.testing;

import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fixtures mirror the crypto project's shape: a project-local {@code unit.Test}
 * base extending a stub {@code utest.Test} (implementing a stub
 * {@code utest.ITest}), so detection is exercised over a transitive chain, not a
 * direct extends.
 */
@DisplayName("Test detection: utest")
public class UtestDetectionTest extends HaxeTestFrameworkDetectionTestBase {

  private final UtestFramework framework = new UtestFramework();

  @Override
  protected String getBasePath() {
    return "/testing/";
  }

  private void configureFixtureProject() {
    myFixture.configureByFiles("utest/ITest.hx", "utest/Test.hx", "unit/Test.hx",
                               "cases/MathTest.hx", "cases/InterfaceStyleTest.hx", "cases/NotATest.hx");
  }

  @Test
  @DisplayName("class through project local base is detected")
  public void testClassThroughProjectLocalBaseIsDetected() {
    configureFixtureProject();
    assertTrue(framework.isTestClass(classByQName("cases.MathTest")));
  }

  @Test
  @DisplayName("interface style class is detected")
  public void testInterfaceStyleClassIsDetected() {
    configureFixtureProject();
    assertTrue(framework.isTestClass(classByQName("cases.InterfaceStyleTest")));
  }

  @Test
  @DisplayName("plain class is not detected")
  public void testPlainClassIsNotDetected() {
    configureFixtureProject();
    assertFalse(framework.isTestClass(classByQName("cases.NotATest")));
  }

  @Test
  @DisplayName("marker interface itself is not detected")
  public void testMarkerInterfaceItselfIsNotDetected() {
    configureFixtureProject();
    assertFalse(framework.isTestClass(classByQName("utest.ITest")));
  }

  @Test
  @DisplayName("methods with test and spec prefix are detected")
  public void testMethodsWithTestAndSpecPrefixAreDetected() {
    configureFixtureProject();
    HaxeClass mathTest = classByQName("cases.MathTest");
    assertTrue(framework.isTestMethod(methodOf(mathTest, "testAddition")));
    assertTrue(framework.isTestMethod(methodOf(mathTest, "specRounding")));
    assertTrue(framework.isTestMethod(methodOf(classByQName("cases.InterfaceStyleTest"), "testDirect")));
  }

  @Test
  @DisplayName("helper and static methods are not detected")
  public void testHelperAndStaticMethodsAreNotDetected() {
    configureFixtureProject();
    HaxeClass mathTest = classByQName("cases.MathTest");
    assertFalse(framework.isTestMethod(methodOf(mathTest, "helperCompute")), "no test/spec prefix");
    // visibility is irrelevant: utest's TestBuilder collects every
    // non-static function whose name carries a test prefix
    assertTrue(framework.isTestMethod(methodOf(mathTest, "testPrivateSetup")), "private prefixed methods ARE tests");
    assertFalse(framework.isTestMethod(methodOf(mathTest, "testStaticFactory")), "not an instance method");
  }

  @Test
  @DisplayName("test shaped method on plain class is not detected")
  public void testTestShapedMethodOnPlainClassIsNotDetected() {
    configureFixtureProject();
    assertFalse(framework.isTestMethod(methodOf(classByQName("cases.NotATest"), "testLooking")));
  }
}
