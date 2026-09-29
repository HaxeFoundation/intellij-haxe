package com.intellij.plugins.haxe.v2.testing;

import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * The utest framework: a test class transitively implements {@code utest.ITest}
 * ({@code utest.Test} implements it, so extends-Test project bases resolve through
 * the same chain); a test is a public instance method named {@code test*} or
 * {@code spec*} on such a class. Activation/filter defines follow utest's built-in
 * TeamCity reporter and {@code UTEST_PATTERN} single-test filter.
 */
public final class UtestFramework implements HaxeTestFramework {

  private static final Set<String> ITEST_MARKER = Set.of("utest.ITest");
  private static final List<String> TEST_METHOD_PREFIXES = List.of("test", "spec");

  @Override
  public boolean isTestClass(@NotNull HaxeClass haxeClass) {
    if (DumbService.isDumb(haxeClass.getProject())) return false;
    try {
      HaxeClassModel model = haxeClass.getModel();
      return model.isClass() && HaxeTestSupertypes.inheritsAny(model, ITEST_MARKER);
    }
    catch (IndexNotReadyException e) {
      // dumb mode can begin mid-walk; detection degrades to "not a test" rather than throwing
      return false;
    }
  }

  @Override
  public boolean isTestMethod(@NotNull HaxeMethod method) {
    if (!hasTestMethodPrefix(method.getName())) return false;
    if (DumbService.isDumb(method.getProject())) return false;
    try {
      HaxeMethodModel model = method.getModel();
      // visibility is irrelevant: utest's TestBuilder collects every
      // non-static function whose name carries a test prefix
      if (model.isConstructor() || model.isStatic()) return false;
      HaxeClassModel declaringClass = model.getDeclaringClass();
      return declaringClass != null && isTestClass(declaringClass.haxeClass);
    }
    catch (IndexNotReadyException e) {
      return false;
    }
  }

  @Override
  public @NotNull String libraryName() {
    return "utest";
  }

  @Override
  public boolean supportsInterp() {
    return true;
  }

  /** Or null when extraction fails - the run then uses utest's batch reporter only. */
  @Override
  public @Nullable String reporterClasspath() {
    return HaxeTestReporterFiles.classpath(
      "/testing/utestLiveReporter/", "utest-live-reporter",
      List.of("intellij_utest/Macro.hx", "intellij_utest/LiveReporter.hx"));
  }

  /**
   * utest ships its own TeamCity batch reporter ({@code -D teamcity}); the
   * shipped live reporter streams per-test events on top and the converter
   * deduplicates the batch replay, so the live half is an optional
   * enhancement the toggle controls.
   */
  @Override
  public @NotNull List<String> reportingArgs(@Nullable String suiteName,
                                             @Nullable String reporterClasspath,
                                             boolean liveReporting) {
    List<String> arguments = new ArrayList<>(List.of("-D", "teamcity"));
    // utest's reporter derives its root suite name from a target #if chain
    // that lacks several targets (an HL run reads "Target: Undefined") - the
    // suite-name define overrides it with the build's real target
    arguments.addAll(HaxeTestReporterArgs.suiteNameDefine(suiteName));
    if (liveReporting) {
      // the suite define is already in place - the live half adds only the injection
      arguments.addAll(HaxeTestReporterArgs.macroReporterArgs(null, reporterClasspath, "intellij_utest.Macro.init()"));
    }
    return arguments;
  }

  @Override
  public @NotNull List<String> filterArgs(@Nullable String pattern) {
    return pattern == null ? List.of() : List.of("-D", "UTEST_PATTERN=" + pattern);
  }

  @Override
  public @Nullable String singleRunTemplate(boolean singleTest) {
    // one template serves both: the method narrows via UTEST_PATTERN
    return "singleSuite.hx";
  }

  @Override
  public @NotNull List<String> singleRunFilterArgs(@NotNull String methodName) {
    // the runner matches the pattern against "ClassName.methodName" - the
    // dot-prefixed end anchor selects exactly the method within the
    // template's single suite
    return filterArgs("\\." + methodName + "$");
  }

  private static boolean hasTestMethodPrefix(@NotNull String name) {
    return TEST_METHOD_PREFIXES.stream().anyMatch(name::startsWith);
  }
}
