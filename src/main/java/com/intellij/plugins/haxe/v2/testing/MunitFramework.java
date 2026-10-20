package com.intellij.plugins.haxe.v2.testing;

import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.metadata.psi.HaxeMeta;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The munit (MassiveUnit) framework: a test is a public instance method
 * carrying runtime {@code @Test} (or {@code @AsyncTest}) metadata; a test
 * class is any class declaring one. Detection is purely syntactic — munit
 * requires no marker interface.
 *
 * munit has no TeamCity reporter of its own, so the shipped
 * {@code intellij_munit} client (attached into {@code massive.munit.TestRunner}
 * by its build macro) IS the IDE's result channel, not an optional
 * enhancement. Wire facts (munit 2.3.5): the runner hangs
 * on the eval interpreter (munit predates it); the classic TestMain exits 0
 * even on failures (its delayed completion handler misses the process end),
 * so verdicts come from the events alone; munit has no test-filter define.
 */
public final class MunitFramework implements HaxeTestFramework {

  private static final List<String> TEST_METADATA = List.of("Test", "AsyncTest");

  @Override
  public @NotNull String libraryName() {
    return "munit";
  }

  @Override
  public boolean supportsInterp() {
    return false;
  }

  @Override
  public boolean isTestClass(@NotNull HaxeClass haxeClass) {
    return haxeClass.getModel().isClass()
           && haxeClass.getHaxeMethodsSelf(null).stream().anyMatch(MunitFramework::hasTestMetadata);
  }

  @Override
  public boolean isTestMethod(@NotNull HaxeMethod method) {
    if (!hasTestMetadata(method)) return false;
    HaxeMethodModel model = method.getModel();
    return !model.isConstructor() && !model.isStatic() && model.isPublic();
  }

  /** Or null when extraction fails - the run then reports to the console only. */
  @Override
  public @Nullable String reporterClasspath() {
    return HaxeTestReporterFiles.classpath(
      "/testing/munitLiveReporter/", "munit-live-reporter",
      List.of("intellij_munit/Macro.hx", "intellij_munit/LiveClient.hx"));
  }

  @Override
  public @NotNull List<String> reportingArgs(@Nullable String suiteName,
                                             @Nullable String reporterClasspath,
                                             boolean liveReporting) {
    // the injected client is munit's only result channel - liveReporting cannot opt out
    return HaxeTestReporterArgs.macroReporterArgs(suiteName, reporterClasspath, "intellij_munit.Macro.init()");
  }

  @Override
  public @NotNull List<String> filterArgs(@Nullable String pattern) {
    // munit has no filter mechanism at all - single runs narrow through the
    // generated TestSuite template instead
    return List.of();
  }

  @Override
  public @Nullable String singleRunTemplate(boolean singleTest) {
    // both granularities ride the generated TestSuite; the method narrows
    // via the intellij_munit macro's TestClassHelper patch (see
    // singleRunFilterArgs)
    return "singleSuite.hx";
  }

  @Override
  public @NotNull List<String> singleRunFilterArgs(@NotNull String methodName) {
    // read by intellij_munit.Macro: patches munit's runtime test collection
    // to register only this method
    return List.of("-D", "intellij_munit_test=" + methodName);
  }

  private static boolean hasTestMetadata(@NotNull HaxeMethod method) {
    return method.getMetadataList(HaxeMeta.RUN_TIME).stream()
      .anyMatch(MunitFramework::isTestMetadata);
  }

  private static boolean isTestMetadata(@NotNull HaxeMeta meta) {
    return TEST_METADATA.stream().anyMatch(meta::isType);
  }
}
