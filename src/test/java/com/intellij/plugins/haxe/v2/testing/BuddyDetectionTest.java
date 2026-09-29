package com.intellij.plugins.haxe.v2.testing;

import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fixtures use a stub {@code buddy.BuddySuite}/{@code buddy.SingleSuite}
 * chain, so detection is exercised over a transitive extends, not a direct
 * one.
 */
@DisplayName("Test detection: buddy")
public class BuddyDetectionTest extends HaxeTestFrameworkDetectionTestBase {

  private final BuddyFramework framework = new BuddyFramework();

  @Override
  protected String getBasePath() {
    return "/testing/";
  }

  private void configureFixtureProject() {
    myFixture.configureByFiles("buddy/BuddySuite.hx", "buddy/SingleSuite.hx",
                               "cases/BuddyStyleSuite.hx", "cases/NotATest.hx");
  }

  @Test
  @DisplayName("suite through single suite is detected")
  public void testSuiteThroughSingleSuiteIsDetected() {
    configureFixtureProject();
    assertTrue(framework.isTestClass(classByQName("cases.BuddyStyleSuite")));
  }

  @Test
  @DisplayName("plain class is not detected")
  public void testPlainClassIsNotDetected() {
    configureFixtureProject();
    assertFalse(framework.isTestClass(classByQName("cases.NotATest")));
  }

  @Test
  @DisplayName("spec location resolves to the description literal in its file")
  public void testSpecLocationResolvesToTheDescriptionLiteralInItsFile() {
    configureFixtureProject();
    PsiElement resolved = framework.resolveTestLocation(getProject(), GlobalSearchScope.allScope(getProject()),
                                                        "haxe:buddy", "cases/BuddyStyleSuite.hx::adds numbers");
    assertNotNull(resolved, "the spec's file and description must resolve");
    assertEquals("BuddyStyleSuite.hx", resolved.getContainingFile().getName());
    assertTrue(resolved.getText().contains("adds numbers"),
               "navigation lands on the description literal, got: " + resolved.getText());

    var foreignProtocolLocation = framework.resolveTestLocation(getProject(), GlobalSearchScope.allScope(getProject()),
                                                                "haxe:test", "cases.BuddyStyleSuite");
    assertNull(foreignProtocolLocation, "buddy answers only its own protocol");
  }
}
