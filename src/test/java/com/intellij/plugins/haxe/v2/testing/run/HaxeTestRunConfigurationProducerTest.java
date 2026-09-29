package com.intellij.plugins.haxe.v2.testing.run;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.ConfigurationFromContext;
import com.intellij.execution.actions.RunConfigurationProducer;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Context-menu runs over a conventional utest build ({@code proj/test.hxml}
 * with {@code -cp .}) and project-local {@code utest} stubs, so detection
 * needs no haxelib.
 */
@DisplayName("Test runner: run configuration producer")
public class HaxeTestRunConfigurationProducerTest extends HaxeCodeInsightFixtureTestCase {

  private PsiFile alphaTest;
  private PsiFile helper;

  @Override
  protected String getBasePath() {
    return "/testing/runner/";
  }

  @BeforeEach
  public void addProject() {
    myFixture.addFileToProject("proj/utest/ITest.hx", "package utest;\ninterface ITest {}\n");
    myFixture.addFileToProject("proj/utest/Test.hx", "package utest;\nclass Test implements ITest {}\n");
    myFixture.addFileToProject("proj/test.hxml", "-cp .\n-lib utest\n--main AllTests\n--interp\n");
    alphaTest = myFixture.addFileToProject("proj/cases/AlphaTest.hx", """
      package cases;
      class AlphaTest extends utest.Test {
        public function new() {}
        function testOne() {}
      }
      """);
    myFixture.addFileToProject("proj/cases/BetaTest.hx", """
      package cases;
      class BetaTest extends utest.Test {
        public function new() {}
        function testTwo() {}
      }
      """);
    helper = myFixture.addFileToProject("proj/cases/Helper.hx", """
      package cases;
      class Helper {}
      """);
    myFixture.addFileToProject("proj/other/Plain.hx", "package other;\nclass Plain {}\n");
  }

  @Test
  @DisplayName("directory runs every suite below it")
  public void testDirectoryRunsEverySuiteBelowIt() {
    HaxeTestRunConfiguration configuration = produced(alphaTest.getContainingDirectory());

    assertNotNull(configuration, "a directory with suites offers a run");
    assertEquals(List.of("cases.AlphaTest", "cases.BetaTest"), configuration.getTestClasses());
    assertEquals("", configuration.getTestMethod());
    assertEquals("Tests in 'cases'", configuration.getName());
    assertTrue(configuration.getBuildFilePath().endsWith("proj/test.hxml"), "the conventional build owns the directory");
  }

  @Test
  @DisplayName("file runs its suite")
  public void testFileRunsItsSuite() {
    HaxeTestRunConfiguration configuration = produced(alphaTest);

    assertNotNull(configuration);
    assertEquals(List.of("cases.AlphaTest"), configuration.getTestClasses());
    assertEquals("AlphaTest (test.hxml)", configuration.getName(), "a single suite takes the generated name");
  }

  @Test
  @DisplayName("locations without suites offer nothing")
  public void testLocationsWithoutSuitesOfferNothing() {
    PsiDirectory other = helper.getContainingDirectory().getParentDirectory().findSubdirectory("other");
    assertNotNull(other);

    assertNull(produced(helper), "a plain class is no suite");
    assertNull(produced(other), "a directory without suites offers no run");
  }

  @Test
  @DisplayName("produced configuration is recognized as from its context")
  public void testProducedConfigurationIsRecognizedAsFromItsContext() {
    ConfigurationContext context = contextOf(alphaTest.getContainingDirectory());
    ConfigurationFromContext fromContext = producer().createConfigurationFromContext(context);
    assertNotNull(fromContext);

    HaxeTestRunConfiguration configuration = (HaxeTestRunConfiguration)fromContext.getConfiguration();
    assertTrue(producer().isConfigurationFromContext(configuration, context));
    assertFalse(producer().isConfigurationFromContext(configuration, contextOf(alphaTest)), "a file selection is a different run");
  }

  @Nullable
  private HaxeTestRunConfiguration produced(PsiElement location) {
    ConfigurationFromContext fromContext = producer().createConfigurationFromContext(contextOf(location));
    return fromContext == null ? null : (HaxeTestRunConfiguration)fromContext.getConfiguration();
  }

  private ConfigurationContext contextOf(PsiElement location) {
    DataContext dataContext = SimpleDataContext.builder()
      .add(CommonDataKeys.PROJECT, getProject())
      .add(CommonDataKeys.PSI_ELEMENT, location)
      .add(PlatformCoreDataKeys.PSI_ELEMENT_ARRAY, new PsiElement[]{location})
      .build();
    return ConfigurationContext.getFromContext(dataContext, ActionPlaces.PROJECT_VIEW_POPUP);
  }

  private static HaxeTestRunConfigurationProducer producer() {
    return RunConfigurationProducer.getInstance(HaxeTestRunConfigurationProducer.class);
  }
}
