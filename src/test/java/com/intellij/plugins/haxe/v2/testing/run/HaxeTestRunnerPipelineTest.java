package com.intellij.plugins.haxe.v2.testing.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.testframework.sm.runner.GeneralTestEventsProcessor;
import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import com.intellij.execution.testframework.sm.runner.TestProxyPrinterProvider;
import com.intellij.execution.testframework.sm.runner.events.*;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.v2.testing.HaxeTestFrameworks;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileType;
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildSystem;
import com.intellij.plugins.haxe.v2.buildtools.HaxeCompileCommands;
import com.intellij.plugins.haxe.v2.testing.run.HaxeTestLaunchPlanner.Plan;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * The full vertical slice, live (self-skips without haxe on the PATH):
 * configuration -> plan -> real {@code haxe --interp} run -> the run's
 * converter (with locationHint injection) -> platform-parsed SM test events.
 * The first test uses a self-contained TeamCity-printing main mirroring
 * utest's BATCH reporter shapes (no haxelib dependency - this is the fallback
 * path when the live-reporter extraction fails); the second compiles against
 * real utest (self-skips when not installed) and proves the shipped live
 * reporter: streamed events and per-test output attribution.
 */
@DisplayName("Test runner: SM pipeline (live)")
public class HaxeTestRunnerPipelineTest extends HaxeCodeInsightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "/testing/runner/";
  }

  @Test
  @Timeout(120)
  @DisplayName("run produces parsed test events with locations")
  public void testRunProducesParsedTestEventsWithLocations() throws Exception {
    assumeTrue(haxeAvailable(), "haxe not on PATH - skipping live test-runner pipeline test");

    myFixture.copyFileToProject("src/TestMain.hx");
    VirtualFile buildFile = myFixture.copyFileToProject("test.hxml");

    HaxeTestRunConfiguration configuration = newConfiguration(buildFile.getPath());
    assertDoesNotThrow(configuration::checkConfiguration, "the interp fixture must validate");

    Plan plan = HaxeTestLaunchPlanner.plan(getProject(), buildFile.getPath(), null);
    assertTrue(plan.singleStage(), "the fixture targets --interp");

    RecordingEventsProcessor recorder = runThroughConverter(configuration, plan, 0);

    assertTrue(recorder.startedSuites.contains("cases.SampleTest"),
               "suite event missing, got: " + recorder.startedSuites);
    assertTrue(recorder.startedTests.contains("cases.SampleTest.testPasses"),
               "passing test start missing, got: " + recorder.startedTests);
    assertTrue(recorder.failedTests.contains("cases.SampleTest.testFails"),
               "failing test event missing, got: " + recorder.failedTests);
    // utest emits no testFinished after a failure - the converter synthesizes it
    assertTrue(recorder.finishedTests.contains("cases.SampleTest.testFails"),
               "the failed test must still finish, got: " + recorder.finishedTests);
    String location = recorder.locationsByTest.get("cases.SampleTest.testPasses");
    assertNotNull(location, "the converter must have injected the location hint");
    assertTrue(location.startsWith("haxe:test://cases.SampleTest.testPasses?build="),
               "the hint carries the name and the run's tests build file: " + location);
  }

  @Test
  @Timeout(120)
  @DisplayName("live reporter streams events and the batch replay is deduplicated")
  public void testLiveReporterStreamsEventsAndTheBatchReplayIsDeduplicated() throws Exception {
    assumeTrue(haxeAvailable(), "haxe not on PATH - skipping real-utest pipeline test");
    assumeTrue(utestAvailable(), "utest haxelib not installed - skipping real-utest pipeline test");

    myFixture.copyDirectoryToProject("utest", "utest");
    VirtualFile buildFile = myFixture.findFileInTempDir("utest/test.hxml");
    assertNotNull(buildFile, "the utest fixture must be copied");

    Plan plan = HaxeTestLaunchPlanner.plan(getProject(), buildFile.getPath(), null);
    // utest exits 1: the assertion-less test counts as a warning and warnings make its stats not-ok
    RecordingEventsProcessor recorder = runThroughConverter(newConfiguration(buildFile.getPath()), plan, 1);

    // live names carry no leading dot (utest's BATCH spells the default
    // package as `.LiveCase` - those replays must all be swallowed)
    assertTrue(recorder.startedSuites.contains("LiveCase"), "class suite expected, got: " + recorder.startedSuites);
    assertEquals(List.of("LiveCase.testTraces", "LiveCase.testPasses", "LiveCase.testNoAsserts"),
                 recorder.finishedTests,
                 "each test finishes exactly once - the batch replay is deduplicated");
    assertFalse(recorder.startedSuites.contains(".LiveCase"),
                "the batch's suite replay must be swallowed, got: " + recorder.startedSuites);

    // output printed DURING a live-reported test arrives between its
    // started/finished events and attributes to it
    String tracedOutput = recorder.outputByTest.get("LiveCase.testTraces");
    assertNotNull(tracedOutput, "trace must be attributed to testTraces, got: " + recorder.outputByTest);
    assertTrue(tracedOutput.contains("hello from the traced test"), "trace text expected, got: " + tracedOutput);

    assertTrue(recorder.failedTests.contains("LiveCase.testNoAsserts"),
               "warnings-only test fails, as in the VSCode adapter");
    assertFalse(recorder.failedTests.contains("LiveCase.testPasses"),
                "the silent passing test must stay green");
  }

  @Test
  @Timeout(120)
  @DisplayName("flash run streams events through adl")
  public void testFlashRunStreamsEventsThroughAdl() throws Exception {
    assumeTrue(haxeAvailable(), "haxe not on PATH - skipping flash pipeline test");
    assumeTrue(utestAvailable(), "utest haxelib not installed - skipping flash pipeline test");
    assumeTrue(adlAvailable(), "AIR_SDK not set (or has no adl) - skipping flash pipeline test");

    myFixture.copyDirectoryToProject("utest", "utest");
    VirtualFile buildFile = myFixture.findFileInTempDir("utest/swf.hxml");
    assertNotNull(buildFile, "the swf fixture must be copied");

    compileTestsBuild(buildFile);

    Plan plan = HaxeTestLaunchPlanner.plan(getProject(), buildFile.getPath(), null);
    assertEquals("-nodebug", plan.command().get(1), "adl's default debug-launch mode swallows trace output");
    // the injected reporter's completion handler runs FIRST (attached in
    // Runner's constructor) and exits with 0 - the batch replay never prints
    RecordingEventsProcessor recorder = runThroughConverter(newConfiguration(buildFile.getPath()), plan, 0);

    assertTrue(recorder.startedSuites.contains("LiveCase"), "class suite expected, got: " + recorder.startedSuites);
    assertEquals(List.of("LiveCase.testTraces", "LiveCase.testPasses", "LiveCase.testNoAsserts"),
                 recorder.finishedTests,
                 "each live-reported test finishes exactly once, got: " + recorder.finishedTests);
    // trace() rides the hooked native trace, so adl forwards it and the
    // converter attributes it between the test's started/finished events
    String tracedOutput = recorder.outputByTest.get("LiveCase.testTraces");
    assertNotNull(tracedOutput, "trace must be attributed to testTraces, got: " + recorder.outputByTest);
    assertTrue(tracedOutput.contains("hello from the traced test"), "trace text expected, got: " + tracedOutput);
  }

  @Test
  @Timeout(120)
  @DisplayName("munit run streams events through the injected client")
  public void testMunitRunStreamsEventsThroughTheInjectedClient() throws Exception {
    assumeTrue(haxeAvailable(), "haxe not on PATH - skipping real-munit pipeline test");
    assumeTrue(munitAvailable(), "munit haxelib not installed - skipping real-munit pipeline test");
    assumeTrue(nekoAvailable(), "neko not on PATH - skipping real-munit pipeline test");

    myFixture.copyDirectoryToProject("munit", "munit");
    VirtualFile buildFile = myFixture.findFileInTempDir("munit/test.hxml");
    assertNotNull(buildFile, "the munit fixture must be copied");

    assertEquals("munit", HaxeTestFrameworks.forBuildFile(getProject(), buildFile.getPath()).libraryName());
    compileTestsBuild(buildFile);

    Plan plan = HaxeTestLaunchPlanner.plan(getProject(), buildFile.getPath(), null);
    // munit's classic TestMain exits 0 even on failures (its delayed
    // completion handler misses the process end) - verdicts are events-only
    RecordingEventsProcessor recorder = runThroughConverter(newConfiguration(buildFile.getPath()), plan, 0);

    assertTrue(recorder.startedSuites.contains("cases.MunitCase"),
               "class suite expected, got: " + recorder.startedSuites);
    boolean allFinishedOnce = recorder.finishedTests.containsAll(
      List.of("cases.MunitCase.testPasses", "cases.MunitCase.testFails", "cases.MunitCase.testIgnored"))
      && recorder.finishedTests.size() == 3;
    assertTrue(allFinishedOnce, "each test finishes exactly once, got: " + recorder.finishedTests);
    assertTrue(recorder.failedTests.contains("cases.MunitCase.testFails"),
               "failing test event missing, got: " + recorder.failedTests);
    assertFalse(recorder.failedTests.contains("cases.MunitCase.testPasses"), "the passing test must stay green");
    // munit clients hear about a test AFTER it ran; the live client buffers
    // the hijacked traces and replays them as the reported test's own output
    String tracedOutput = recorder.outputByTest.get("cases.MunitCase.testPasses");
    assertNotNull(tracedOutput, "trace must be attributed to the test, got: " + recorder.outputByTest);
    assertTrue(tracedOutput.contains("hello from the passing test"), "trace text expected, got: " + tracedOutput);
    String location = recorder.locationsByTest.get("cases.MunitCase.testPasses");
    assertNotNull(location, "the converter must have injected the location hint");
    assertTrue(location.startsWith("haxe:test://cases.MunitCase.testPasses?build="),
               "the hint carries the name and the run's tests build file: " + location);
  }

  @Test
  @Timeout(120)
  @DisplayName("buddy run reports the nested tree with attributed traces")
  public void testBuddyRunReportsTheNestedTreeWithAttributedTraces() throws Exception {
    assumeTrue(haxeAvailable(), "haxe not on PATH - skipping real-buddy pipeline test");
    assumeTrue(buddyAvailable(), "buddy haxelib not installed - skipping real-buddy pipeline test");

    myFixture.copyDirectoryToProject("buddy", "buddy");
    VirtualFile buildFile = myFixture.findFileInTempDir("buddy/test.hxml");
    assertNotNull(buildFile, "the buddy fixture must be copied");

    assertEquals("buddy", HaxeTestFrameworks.forBuildFile(getProject(), buildFile.getPath()).libraryName());
    Plan plan = HaxeTestLaunchPlanner.plan(getProject(), buildFile.getPath(), null);
    // buddy's generated main exits 1 on a failing spec - verified live
    RecordingEventsProcessor recorder = runThroughConverter(newConfiguration(buildFile.getPath()), plan, 1);

    assertTrue(recorder.startedSuites.contains("A calculator"),
               "describe suite expected, got: " + recorder.startedSuites);
    assertTrue(recorder.startedSuites.contains("nested memory bank"),
               "nested describe suite expected, got: " + recorder.startedSuites);
    assertTrue(recorder.finishedTests.containsAll(List.of("adds numbers", "fails sometimes", "stores values")),
               "specs must finish, got: " + recorder.finishedTests);
    assertTrue(recorder.failedTests.contains("fails sometimes"),
               "failing spec event missing, got: " + recorder.failedTests);
    assertFalse(recorder.failedTests.contains("adds numbers"), "the passing spec must stay green");

    // buddy captures a spec's traces itself; the reporter replays them as
    // that spec's testStdOut - attribution without any stdout parsing
    String tracedOutput = recorder.outputByTest.get("traces while working");
    assertNotNull(tracedOutput, "trace must be attributed to its spec, got: " + recorder.outputByTest);
    assertTrue(tracedOutput.contains("a trace from buddy"), "trace text expected, got: " + tracedOutput);

    // the reporter's hints carry the it() call site's file and the description
    String specLocation = recorder.locationsByTest.get("adds numbers");
    assertNotNull(specLocation, "spec location hint expected, got: " + recorder.locationsByTest);
    boolean specHintShape = specLocation.startsWith("haxe:buddy://") && specLocation.endsWith("::adds numbers");
    assertTrue(specHintShape, "file-plus-description hint expected: " + specLocation);
  }

  @Test
  @Timeout(120)
  @DisplayName("tink run streams events through the default reporter patch")
  public void testTinkRunStreamsEventsThroughTheDefaultReporterPatch() throws Exception {
    assumeTrue(haxeAvailable(), "haxe not on PATH - skipping real-tink pipeline test");
    assumeTrue(tinkAvailable(), "tink_unittest haxelib not installed - skipping real-tink pipeline test");

    myFixture.copyDirectoryToProject("tink", "tink");
    VirtualFile buildFile = myFixture.findFileInTempDir("tink/test.hxml");
    assertNotNull(buildFile, "the tink fixture must be copied");

    assertEquals("tink_unittest", HaxeTestFrameworks.forBuildFile(getProject(), buildFile.getPath()).libraryName());
    Plan plan = HaxeTestLaunchPlanner.plan(getProject(), buildFile.getPath(), null);
    // Runner.exit reports the failure count as the exit code - verified live
    RecordingEventsProcessor recorder = runThroughConverter(newConfiguration(buildFile.getPath()), plan, 1);

    assertTrue(recorder.startedSuites.contains("TinkCase"), "case suite expected, got: " + recorder.startedSuites);
    boolean allFinishedOnce = recorder.finishedTests.containsAll(List.of("passes", "fails"))
                              && recorder.finishedTests.size() == 2;
    assertTrue(allFinishedOnce, "each test finishes exactly once, got: " + recorder.finishedTests);
    assertTrue(recorder.failedTests.contains("fails"), "failing test event missing, got: " + recorder.failedTests);
    assertFalse(recorder.failedTests.contains("passes"), "the passing test must stay green");
    String caseLocation = recorder.locationsByTest.get("passes");
    assertNotNull(caseLocation, "case location hint expected, got: " + recorder.locationsByTest);
    boolean caseHintShape = caseLocation.startsWith("haxe:tink://") && caseLocation.endsWith("::TinkCase.passes");
    assertTrue(caseHintShape, "file-plus-name hint from the case's PosInfos expected: " + caseLocation);
  }

  @Test
  @Timeout(120)
  @DisplayName("gutter single test narrows munit through the patched collection")
  public void testGutterSingleTestNarrowsMunitThroughThePatchedCollection() throws Exception {
    assumeTrue(haxeAvailable(), "haxe not on PATH - skipping single-run pipeline test");
    assumeTrue(munitAvailable(), "munit haxelib not installed - skipping single-run pipeline test");
    assumeTrue(nekoAvailable(), "neko not on PATH - skipping single-run pipeline test");

    myFixture.copyDirectoryToProject("munit", "munit");
    VirtualFile buildFile = myFixture.findFileInTempDir("munit/test.hxml");
    assertNotNull(buildFile, "the munit fixture must be copied");

    HaxeTestRunConfiguration configuration = newConfiguration(buildFile.getPath());
    configuration.setSingleRun("cases.MunitCase", "testPasses");
    compileSingleRun(configuration);

    Plan plan = HaxeTestLaunchPlanner.planFor(configuration);
    RecordingEventsProcessor recorder = runThroughConverter(configuration, plan, 0);
    assertEquals(List.of("cases.MunitCase.testPasses"), recorder.finishedTests,
                 "only the selected method registers (the macro guards addTest)");
    assertTrue(recorder.failedTests.isEmpty(), "the failing sibling must not run: " + recorder.failedTests);
  }

  @Test
  @Timeout(120)
  @DisplayName("gutter single test compiles the template and runs only the selected utest method")
  public void testGutterSingleTestCompilesTheTemplateAndRunsOnlyTheSelectedUtestMethod() throws Exception {
    assumeTrue(haxeAvailable(), "haxe not on PATH - skipping single-run pipeline test");
    assumeTrue(utestAvailable(), "utest haxelib not installed - skipping single-run pipeline test");

    myFixture.copyDirectoryToProject("utest", "utest");
    VirtualFile buildFile = myFixture.findFileInTempDir("utest/test.hxml");
    assertNotNull(buildFile, "the utest fixture must be copied");

    HaxeTestRunConfiguration configuration = newConfiguration(buildFile.getPath());
    // LiveCase is a secondary type of the UtestMain module - the module rides the reference
    configuration.setSingleRun("UtestMain.LiveCase", "testPasses");
    Plan plan = HaxeTestLaunchPlanner.planFor(configuration);
    assertTrue(plan.singleStage(), "the interp single run compiles-and-runs as one process");

    RecordingEventsProcessor recorder = runThroughConverter(configuration, plan, 0);
    assertEquals(List.of("LiveCase.testPasses"), recorder.finishedTests,
                 "only the selected method runs (UTEST_PATTERN narrows the generated suite)");
    assertTrue(recorder.failedTests.isEmpty(), "the selected test passes, got: " + recorder.failedTests);
  }

  @Test
  @Timeout(120)
  @DisplayName("gutter single test excludes the other tink cases")
  public void testGutterSingleTestExcludesTheOtherTinkCases() throws Exception {
    assumeTrue(haxeAvailable(), "haxe not on PATH - skipping single-run pipeline test");
    assumeTrue(tinkAvailable(), "tink_unittest haxelib not installed - skipping single-run pipeline test");

    myFixture.copyDirectoryToProject("tink", "tink");
    VirtualFile buildFile = myFixture.findFileInTempDir("tink/test.hxml");
    assertNotNull(buildFile, "the tink fixture must be copied");

    HaxeTestRunConfiguration configuration = newConfiguration(buildFile.getPath());
    configuration.setSingleRun("TinkCase", "passes");
    Plan plan = HaxeTestLaunchPlanner.planFor(configuration);

    // exit 0: only the passing case executes; the failing one is excluded by
    // the template's include-mode flip and stays out of the tree entirely
    RecordingEventsProcessor recorder = runThroughConverter(configuration, plan, 0);
    assertEquals(List.of("passes"), recorder.finishedTests,
                 "only the selected case appears - excluded ones are not reported");
    assertTrue(recorder.failedTests.isEmpty(),
               "the failing case must be excluded, not run: " + recorder.failedTests);
  }

  /** The single-run compile the before-run step would perform: the generated template main over the build's classpaths. */
  private void compileSingleRun(@NotNull HaxeTestRunConfiguration configuration) throws ExecutionException {
    HaxeCompileCommands.Resolved resolved = configuration.resolveSingleRunCompile();
    assertNotNull(resolved, "the single-run compile must resolve");
    runCompile(resolved, "single-run compile");
  }

  /** The artifact compile the before-run step would perform: the tests build with the framework's reporting args. */
  private void compileTestsBuild(@NotNull VirtualFile buildFile) throws ExecutionException {
    String reportingArguments =
      HaxeTestCompileArguments.compileArguments(getProject(), buildFile.getPath(), null);
    HaxeCompileCommands.Resolved resolved = HaxeCompileCommands.resolveAction(
      getProject(), buildFile.getPath(), HaxeBuildSystem.of(HaxeBuildFileType.HXML).defaultBuildActionName(),
      reportingArguments);
    assertNotNull(resolved, "the tests build must resolve to a compile command");
    runCompile(resolved, "tests compile");
  }

  /** Runs a resolved compile command to completion and asserts it succeeds. */
  private static void runCompile(@NotNull HaxeCompileCommands.Resolved resolved, @NotNull String what)
    throws ExecutionException {
    GeneralCommandLine commandLine = new GeneralCommandLine(resolved.command())
      .withWorkDirectory(resolved.workDirectory());
    ProcessOutput output = new CapturingProcessHandler(commandLine).runProcess(90_000);
    assertFalse(output.isTimeout(), "the " + what + " must finish");
    assertEquals(0, output.getExitCode(), what + " failed:\n" + output.getStdout() + output.getStderr());
  }

  @NotNull
  private HaxeTestRunConfiguration newConfiguration(@NotNull String buildFilePath) {
    HaxeTestRunConfigurationType type = new HaxeTestRunConfigurationType();
    HaxeTestRunConfiguration configuration =
      new HaxeTestRunConfiguration(getProject(), type.getFactory(), "pipeline");
    configuration.setBuildFilePath(buildFilePath);
    return configuration;
  }

  /** Runs the plan's process, streaming its output through the run's real converter into a recording processor. */
  @NotNull
  private RecordingEventsProcessor runThroughConverter(@NotNull HaxeTestRunConfiguration configuration,
                                                       @NotNull Plan plan,
                                                       int expectedExitCode) throws ExecutionException {
    HaxeTestConsoleProperties properties =
      new HaxeTestConsoleProperties(configuration, DefaultRunExecutor.getRunExecutorInstance());
    HaxeTestEventsConverter converter = new HaxeTestEventsConverter("HaxeUnitTests", properties);
    RecordingEventsProcessor recorder = new RecordingEventsProcessor();
    converter.setTestingStartedHandler(() -> { });
    converter.setProcessor(recorder);
    converter.startTesting();
    try {
      GeneralCommandLine commandLine = new GeneralCommandLine(plan.command())
        .withWorkDirectory(plan.workDirectory());
      CapturingProcessHandler handler = new CapturingProcessHandler(commandLine);
      handler.addProcessListener(new ProcessListener() {
        @Override
        public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
          converter.process(event.getText(), outputType);
        }
      });
      ProcessOutput output = handler.runProcess(90_000);
      assertFalse(output.isTimeout(), "the interp run must finish");
      assertEquals(expectedExitCode, output.getExitCode(),
                   "unexpected run outcome:\n" + output.getStdout() + output.getStderr());
      converter.flushBufferOnProcessTermination(output.getExitCode());
      return recorder;
    }
    finally {
      Disposer.dispose(properties);
    }
  }

  /** Captures the platform-parsed SM events; navigation urls prove the locationHint injection ran. */
  private final class RecordingEventsProcessor extends GeneralTestEventsProcessor {
    final List<String> startedSuites = new CopyOnWriteArrayList<>();
    final List<String> startedTests = new CopyOnWriteArrayList<>();
    final List<String> finishedTests = new CopyOnWriteArrayList<>();
    final List<String> failedTests = new CopyOnWriteArrayList<>();
    final Map<String, String> locationsByTest = new ConcurrentHashMap<>();
    /**
     * Output attributed to a test, keyed by that test - explicit testStdOut
     * events (the converter's batch-shape attribution) merged with uncaptured
     * output arriving between a test's started and finished (the live shape).
     */
    final Map<String, String> outputByTest = new ConcurrentHashMap<>();
    private volatile String runningTest;

    private RecordingEventsProcessor() {
      super(getProject(), "HaxeUnitTests", new SMTestProxy.SMRootTestProxy());
    }

    @Override
    public void onStartTesting() { }

    @Override
    public void onTestsCountInSuite(int count) { }

    @Override
    public void onTestStarted(@NotNull TestStartedEvent event) {
      startedTests.add(event.getName());
      runningTest = event.getName();
      if (event.getLocationUrl() != null) {
        locationsByTest.put(event.getName(), event.getLocationUrl());
      }
    }

    @Override
    public void onTestFinished(@NotNull TestFinishedEvent event) {
      finishedTests.add(event.getName());
      runningTest = null;
    }

    @Override
    public void onTestFailure(@NotNull TestFailedEvent event) {
      failedTests.add(event.getName());
    }

    @Override
    public void onTestIgnored(@NotNull TestIgnoredEvent event) { }

    @Override
    public void onTestOutput(@NotNull TestOutputEvent event) {
      outputByTest.merge(event.getName(), event.getText(), String::concat);
    }

    @Override
    public void onSuiteStarted(@NotNull TestSuiteStartedEvent event) {
      startedSuites.add(event.getName());
    }

    @Override
    public void onSuiteFinished(@NotNull TestSuiteFinishedEvent event) { }

    @Override
    public void onUncapturedOutput(@NotNull String text, Key outputType) {
      String current = runningTest;
      if (current != null) {
        outputByTest.merge(current, text, String::concat);
      }
    }

    @Override
    public void onError(@NotNull String localizedMessage, @Nullable String stackTrace, boolean isCritical) { }

    @Override
    public void onTestsReporterAttached() { }

    @Override
    public void setPrinterProvider(@NotNull TestProxyPrinterProvider printerProvider) { }
  }
}
