package com.intellij.plugins.haxe.v2.testing;

import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** munit needs no marker interface - detection reads the runtime {@code @Test} metadata off the methods. */
@DisplayName("Test detection: munit")
public class MunitDetectionTest extends HaxeTestFrameworkDetectionTestBase {

  private final MunitFramework framework = new MunitFramework();

  @Override
  protected String getBasePath() {
    return "/testing/";
  }

  private void configureFixtureProject() {
    myFixture.configureByFiles("cases/MunitStyleTest.hx", "cases/NotATest.hx");
  }

  @Test
  @DisplayName("class with test metadata methods is detected")
  public void testClassWithTestMetadataMethodsIsDetected() {
    configureFixtureProject();
    assertTrue(framework.isTestClass(classByQName("cases.MunitStyleTest")));
  }

  @Test
  @DisplayName("plain class is not detected")
  public void testPlainClassIsNotDetected() {
    configureFixtureProject();
    assertFalse(framework.isTestClass(classByQName("cases.NotATest")));
  }

  @Test
  @DisplayName("test and async test metadata methods are detected")
  public void testTestAndAsyncTestMetadataMethodsAreDetected() {
    configureFixtureProject();
    HaxeClass munitTest = classByQName("cases.MunitStyleTest");
    assertTrue(framework.isTestMethod(methodOf(munitTest, "addsNumbers")));
    assertTrue(framework.isTestMethod(methodOf(munitTest, "loadsAsync")));
    assertTrue(framework.isTestMethod(methodOf(munitTest, "skipped")), "@Ignore still marks a test");
  }

  @Test
  @DisplayName("non public static and unannotated methods are not detected")
  public void testNonPublicStaticAndUnannotatedMethodsAreNotDetected() {
    configureFixtureProject();
    HaxeClass munitTest = classByQName("cases.MunitStyleTest");
    assertFalse(framework.isTestMethod(methodOf(munitTest, "notPublic")));
    assertFalse(framework.isTestMethod(methodOf(munitTest, "staticFactory")));
    assertFalse(framework.isTestMethod(methodOf(munitTest, "plainHelper")));
  }
}
