package com.intellij.plugins.haxe.v2.testing;

import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** tink_unittest marks its test classes with {@code @:asserts} - detection reads that metadata. */
@DisplayName("Test detection: tink_unittest")
public class TinkDetectionTest extends HaxeTestFrameworkDetectionTestBase {

  private final TinkFramework framework = new TinkFramework();

  @Override
  protected String getBasePath() {
    return "/testing/";
  }

  private void configureFixtureProject() {
    myFixture.configureByFiles("cases/TinkStyleTest.hx", "cases/NotATest.hx");
  }

  @Test
  @DisplayName("asserts class is detected")
  public void testAssertsClassIsDetected() {
    configureFixtureProject();
    assertTrue(framework.isTestClass(classByQName("cases.TinkStyleTest")));
  }

  @Test
  @DisplayName("plain class is not detected")
  public void testPlainClassIsNotDetected() {
    configureFixtureProject();
    assertFalse(framework.isTestClass(classByQName("cases.NotATest")));
  }

  @Test
  @DisplayName("public instance methods of an asserts class are tests")
  public void testPublicInstanceMethodsOfAnAssertsClassAreTests() {
    configureFixtureProject();
    HaxeClass tinkTest = classByQName("cases.TinkStyleTest");
    assertTrue(framework.isTestMethod(methodOf(tinkTest, "addsNumbers")));
    assertFalse(framework.isTestMethod(methodOf(tinkTest, "notPublic")));
    assertFalse(framework.isTestMethod(methodOf(tinkTest, "staticFactory")));
  }

  @Test
  @DisplayName("case location resolves through the file when the bare class name is packaged")
  public void testCaseLocationResolvesThroughTheFileWhenTheBareClassNameIsPackaged() {
    configureFixtureProject();
    // tink's builder loses the class's package, so the reporter's hint
    // carries the source file - the bare name fails and the file pins it
    PsiElement resolved = framework.resolveTestLocation(getProject(), GlobalSearchScope.allScope(getProject()),
                                                        "haxe:tink", "cases/TinkStyleTest.hx::TinkStyleTest.addsNumbers");
    assertNotNull(resolved, "the case's file, class and method must resolve");
    HaxeMethod method = assertInstanceOf(HaxeMethod.class, resolved, "navigation lands on the test method");
    assertEquals("addsNumbers", method.getName());

    PsiElement byFullName = framework.resolveTestLocation(getProject(), GlobalSearchScope.allScope(getProject()),
                                                          "haxe:test", "cases.TinkStyleTest.addsNumbers");
    assertTrue(byFullName instanceof HaxeMethod, "the inherited name-based resolution still serves haxe:test");
  }
}
