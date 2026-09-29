package com.intellij.plugins.haxe.v2.testing.run;

import com.intellij.execution.ExecutionException;
import com.intellij.openapi.util.io.NioFiles;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.util.HaxeSdkUtilBase;
import com.intellij.plugins.haxe.v2.buildtools.HaxeCompileCommands;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeBuildToolSettings;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeSectionSelectionStore;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeTargetSelectionStore;
import com.intellij.plugins.haxe.v2.testing.HaxeTestFrameworks;
import com.intellij.plugins.haxe.v2.testing.run.HaxeTestLaunchPlanner.Plan;
import com.intellij.util.execution.ParametersListUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Plans are derived from hxml fixtures only - nothing is compiled. Fixtures live
 * in {@code testData/testing/runner/}; the artifact-target files under
 * {@code targets/} exist purely to be parsed.
 */
@DisplayName("Test runner: launch planner")
public class HaxeTestLaunchPlannerTest extends HaxeLightFixtureTestCase {

  // the planner reads and writes REAL files (LocalFileSystem lookups,
  // generated mains, redirected artifacts) which the light project's
  // in-memory temp filesystem cannot back: each test copies its fixtures
  // into a real scratch directory instead
  private Path fixturesRoot;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    fixturesRoot = Files.createTempDirectory("haxe-planner-fixtures");
    VfsRootAccess.allowRootAccess(getTestRootDisposable(), fixturesRoot.toString());
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      // the shared light project keeps service state between tests
      HaxeTargetSelectionStore.getInstance(getProject()).loadState(new HaxeTargetSelectionStore.State());
      HaxeSectionSelectionStore.getInstance(getProject()).loadState(new HaxeSectionSelectionStore.State());
      NioFiles.deleteRecursively(fixturesRoot);
    }
    finally {
      super.tearDown();
    }
  }

  @Override
  protected String getBasePath() {
    return "/testing/runner/";
  }

  /** Copies the named fixture into the scratch directory and returns its real path. */
  private String fixturePath(String relativePath) {
    try {
      Path target = fixturesRoot.resolve(relativePath);
      Files.createDirectories(target.getParent());
      Files.copy(Path.of(getTestDataPath(), relativePath), target, StandardCopyOption.REPLACE_EXISTING);
      VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(target);
      assertNotNull(file, "fixture must be visible to the vfs: " + target);
      return file.getPath();
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Test
  @DisplayName("interp build is a single stage carrying the reporting define")
  public void testInterpBuildIsASingleStageCarryingTheReportingDefine() throws ExecutionException {
    String path = fixturePath("test.hxml");
    Plan plan = HaxeTestLaunchPlanner.plan(getProject(), path, null, false);
    assertTrue(plan.singleStage(), "interp compiles and runs as one process");
    assertTrue(plan.command().containsAll(List.of("-D", "teamcity")), "activation define missing: " + plan.command());
    assertTrue(plan.command().contains("test.hxml"), "the compile must reference the tests build file");
  }

  @Test
  @DisplayName("filter pattern becomes the utest pattern define")
  public void testFilterPatternBecomesTheUtestPatternDefine() throws ExecutionException {
    String path = fixturePath("test.hxml");
    Plan plan = HaxeTestLaunchPlanner.plan(getProject(), path, "SampleTest.testPasses", false);
    assertTrue(plan.command().contains("UTEST_PATTERN=SampleTest.testPasses"),
               "filter define missing: " + plan.command());
  }

  @Test
  @DisplayName("single suite run swaps the entry point for the generated main")
  public void testSingleSuiteRunSwapsTheEntryPointForTheGeneratedMain() throws Exception {
    String path = fixturePath("test.hxml");
    HaxeTestSingleRuns.SingleRun singleRun = new HaxeTestSingleRuns.SingleRun(List.of("cases.SampleTest"), null);
    Plan plan = HaxeTestLaunchPlanner.planSingle(getProject(), path, singleRun, false);
    List<String> command = plan.command();

    assertTrue(plan.singleStage(), "the interp single run compiles-and-runs as one process");
    assertFalse(command.contains("test.hxml"), "the build file reference is replaced by its expanded section");
    assertFalse(command.contains("TestMain"), "the build's own main is stripped: " + command);
    int mainFlag = command.indexOf("--main");
    assertEquals(HaxeTestSingleRuns.MAIN_CLASS, command.get(mainFlag + 1), "the generated main takes over");
    assertEquals(mainFlag, command.lastIndexOf("--main"), "exactly one main flag survives");

    String generatedRoot = command.get(command.indexOf("--main") - 1);
    Path generatedMain = Path.of(generatedRoot, HaxeTestSingleRuns.MAIN_CLASS + ".hx");
    assertTrue(Files.isRegularFile(generatedMain), "generated main written: " + generatedMain);
    String source = Files.readString(generatedMain);
    assertTrue(source.contains("new cases.SampleTest()"), "the suite class is substituted: " + source);
  }

  @Test
  @DisplayName("single test run adds the anchored utest pattern")
  public void testSingleTestRunAddsTheAnchoredUtestPattern() throws ExecutionException {
    String path = fixturePath("test.hxml");
    HaxeTestSingleRuns.SingleRun singleRun = new HaxeTestSingleRuns.SingleRun(List.of("cases.SampleTest"), "testPasses");
    Plan plan = HaxeTestLaunchPlanner.planSingle(getProject(), path, singleRun, false);
    assertTrue(plan.command().contains("UTEST_PATTERN=\\.testPasses$"),
               "anchored method pattern expected: " + plan.command());
  }

  @Test
  @DisplayName("single run redirects the artifact away from the tests output")
  public void testSingleRunRedirectsTheArtifactAwayFromTheTestsOutput() throws ExecutionException {
    String path = fixturePath("targets/hl.hxml");
    HaxeTestSingleRuns.SingleRun singleRun = new HaxeTestSingleRuns.SingleRun(List.of("cases.SampleTest"), null);
    Plan plan = HaxeTestLaunchPlanner.planSingle(getProject(), path, singleRun, false);
    assertFalse(plan.singleStage(), "artifact targets keep the compile in the before-run step");
    assertTrue(plan.command().get(1).endsWith("single.hl"),
               "the run must launch the redirected artifact: " + plan.command());
    assertFalse(plan.command().get(1).endsWith("tests.hl"),
                "the real tests artifact must not be touched: " + plan.command());
  }

  @Test
  @DisplayName("munit single method narrows through the macro define")
  public void testMunitSingleMethodNarrowsThroughTheMacroDefine() throws ExecutionException {
    String path = fixturePath("targets/munit-neko.hxml");
    HaxeTestSingleRuns.SingleRun singleRun = new HaxeTestSingleRuns.SingleRun(List.of("CalculatorTest"), "testAdd");
    HaxeTestLaunchPlanner.planSingle(getProject(), path, singleRun, false);

    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
    assertNotNull(file);
    HaxeCompileCommands.Resolved compile = HaxeTestLaunchPlanner.singleRunCompile(
      getProject(), file, HaxeTestFrameworks.forBuildFile(getProject(), path), singleRun);
    assertNotNull(compile, "the single-run compile must resolve");
    assertTrue(compile.command().contains("intellij_munit_test=testAdd"),
               "the macro's narrowing define must ride the compile: " + compile.command());
  }

  @Test
  @DisplayName("lime single run launches the packaged binary like the whole build")
  public void testLimeSingleRunLaunchesThePackagedBinaryLikeTheWholeBuild() throws ExecutionException {
    String path = fixturePath("targets/lime-project.xml");
    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
    HaxeTargetSelectionStore.getInstance(getProject()).setSelectedTargetId(file, "Neko");
    HaxeTestSingleRuns.SingleRun singleRun = new HaxeTestSingleRuns.SingleRun(List.of("CalculatorTest"), null);

    Plan single = HaxeTestLaunchPlanner.planSingle(getProject(), path, singleRun, false);
    Plan whole = HaxeTestLaunchPlanner.plan(getProject(), path, null, false);
    assertFalse(single.singleStage(), "the compile stays in the before-run step");
    assertEquals(HaxeTarget.NEKO, single.target());
    assertEquals(whole.command(), single.command(), "the single run launches the same packaged binary");
    assertEquals(whole.workDirectory(), single.workDirectory());
  }

  @Test
  @DisplayName("lime single run compile arguments override the app main with the generated one")
  public void testLimeSingleRunCompileArgumentsOverrideTheAppMainWithTheGeneratedOne() throws IOException {
    String path = fixturePath("targets/lime-project.xml");
    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
    HaxeTargetSelectionStore.getInstance(getProject()).setSelectedTargetId(file, "Neko");
    HaxeTestSingleRuns.SingleRun singleRun = new HaxeTestSingleRuns.SingleRun(List.of("CalculatorTest"), "testAdd");

    String joined = HaxeTestCompileArguments.compileArguments(getProject(), path, null, singleRun);
    List<String> arguments = ParametersListUtil.parse(joined);

    assertTrue(arguments.contains("--app-main=" + HaxeTestSingleRuns.MAIN_CLASS),
               "the generated main overrides the app's: " + arguments);
    assertTrue(arguments.contains("-Dteamcity_suite_name=Target: Neko"), "the suite label names the target: " + arguments);
    boolean narrowed = arguments.stream().anyMatch(argument -> argument.startsWith("-DUTEST_PATTERN=") && argument.contains("testAdd"));
    assertTrue(narrowed, "a single test rides the framework's narrowing define: " + arguments);
    Path generatedMain = generatedSourceRoot(arguments).resolve(HaxeTestSingleRuns.MAIN_CLASS + ".hx");
    assertTrue(Files.isRegularFile(generatedMain), "the generated main is written: " + generatedMain);
    assertTrue(Files.readString(generatedMain).contains("new CalculatorTest()"), "the template carries the selected suite");
  }

  @Test
  @DisplayName("lime html5 build plans a browser hosted run over the packaged web root")
  public void testLimeHtml5BuildPlansABrowserHostedRunOverThePackagedWebRoot() throws Exception {
    String path = fixturePath("targets/lime-project.xml");
    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
    HaxeTargetSelectionStore.getInstance(getProject()).setSelectedTargetId(file, "HTML5");

    Path indexHtml = fixturesRoot.resolve("targets/export/html5/bin/index.html");
    Files.createDirectories(indexHtml.getParent());
    Files.writeString(indexHtml, "<html></html>");
    LocalFileSystem.getInstance().refreshAndFindFileByNioFile(indexHtml);

    Plan plan = HaxeTestLaunchPlanner.plan(getProject(), path, null, false);
    assertTrue(plan.browserHosted(), "html5 tests run in a served browser page");
    assertEquals(HaxeTarget.JAVA_SCRIPT, plan.target());
    assertTrue(plan.command().get(0).replace('\\', '/').endsWith("export/html5/bin"),
               "the packaged web root is what gets served: " + plan.command());
  }

  @Test
  @DisplayName("html5 single run plans a browser hosted run over the packaged web root")
  public void testHtml5SingleRunPlansABrowserHostedRunOverThePackagedWebRoot() throws Exception {
    String path = fixturePath("targets/lime-project.xml");
    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
    HaxeTargetSelectionStore.getInstance(getProject()).setSelectedTargetId(file, "HTML5");
    HaxeTestSingleRuns.SingleRun singleRun = new HaxeTestSingleRuns.SingleRun(List.of("CalculatorTest"), null);

    Plan plan = HaxeTestLaunchPlanner.planSingle(getProject(), path, singleRun, false);
    assertTrue(plan.browserHosted(), "the packaged html5 output is served like the whole build");
    assertTrue(plan.command().get(0).replace('\\', '/').endsWith("export/html5/bin"),
               "the packaged web root is what gets served: " + plan.command());
  }

  @Test
  @DisplayName("single runs on nmml builds are refused")
  public void testSingleRunsOnNmmlBuildsAreRefused() {
    String path = fixturePath("targets/tests.nmml");
    HaxeTestSingleRuns.SingleRun singleRun = new HaxeTestSingleRuns.SingleRun(List.of("CalculatorTest"), null);
    assertThrows(ExecutionException.class,
                 () -> HaxeTestLaunchPlanner.planSingle(getProject(), path, singleRun, false));
  }

  @Test
  @DisplayName("hl build launches the bytecode artifact")
  public void testHlBuildLaunchesTheBytecodeArtifact() throws ExecutionException {
    String path = fixturePath("targets/hl.hxml");
    Plan plan = HaxeTestLaunchPlanner.plan(getProject(), path, null, false);
    assertFalse(plan.singleStage(), "artifact targets compile in the before-run step");
    // HlExecutableResolver may find a full path (env/PATH-dependent) or fall
    // back to the bare name - either way the launched binary is hl
    String executableName = Path.of(plan.command().get(0)).getFileName().toString();
    boolean hlLauncher = executableName.equals("hl") || executableName.equals("hl.exe");
    assertTrue(hlLauncher, "hl launcher expected: " + plan.command());
    assertTrue(plan.command().get(1).endsWith("tests.hl"), "artifact path expected: " + plan.command());
  }

  @Test
  @DisplayName("jvm jar launches through java")
  public void testJvmJarLaunchesThroughJava() throws ExecutionException {
    String path = fixturePath("targets/jvm.hxml");
    Plan plan = HaxeTestLaunchPlanner.plan(getProject(), path, null, false);
    assertEquals(HaxeSdkUtilBase.getExecutableName("java"), plan.command().get(0));
    assertEquals("-jar", plan.command().get(1));
    assertTrue(plan.command().get(2).endsWith("tests.jar"));
  }

  @Test
  @DisplayName("cpp executable is named after the main class")
  public void testCppExecutableIsNamedAfterTheMainClass() throws ExecutionException {
    String path = fixturePath("targets/cpp.hxml");
    Plan plan = HaxeTestLaunchPlanner.plan(getProject(), path, null, false);
    assertEquals(1, plan.command().size());
    assertTrue(plan.command().get(0).endsWith(HaxeSdkUtilBase.getExecutableName("TestMain")),
               "hxcpp names the binary after the main class simple name: " + plan.command());
  }

  @Test
  @DisplayName("cpp binary flavor follows the compile arguments")
  public void testCppBinaryFlavorFollowsTheCompileArguments() throws ExecutionException {
    // hxcpp appends -debug to the binary when the build compiles with --debug;
    // the flavor is decided by the arguments, never by what sits on disk
    String debugBinary = HaxeSdkUtilBase.getExecutableName("TestMain-debug");

    Plan debugHxml = HaxeTestLaunchPlanner.plan(getProject(), fixturePath("targets/cpp-debug.hxml"), null, false);
    assertTrue(debugHxml.command().get(0).endsWith(debugBinary),
               "a --debug hxml produces the -debug binary even for plain runs: " + debugHxml.command());

    Plan debugExecutor = HaxeTestLaunchPlanner.planForDebug(getProject(), fixturePath("targets/cpp.hxml"), null);
    assertTrue(debugExecutor.command().get(0).endsWith(debugBinary),
               "the debug executor's compile additions rename the binary: " + debugExecutor.command());
  }

  @Test
  @DisplayName("js runs under node and hints when hxnodejs is absent")
  public void testJsRunsUnderNodeAndHintsWhenHxnodejsIsAbsent() throws ExecutionException {
    Plan withLib = HaxeTestLaunchPlanner.plan(getProject(), fixturePath("targets/js-node.hxml"), null, true);
    assertEquals(HaxeSdkUtilBase.getExecutableName("node"), withLib.command().get(0));
    assertNull(withLib.hint(), "hxnodejs declared - no hint");

    Plan plain = HaxeTestLaunchPlanner.plan(getProject(), fixturePath("targets/js-plain.hxml"), null, true);
    assertEquals(HaxeSdkUtilBase.getExecutableName("node"), plain.command().get(0));
    assertNotNull(plain.hint(), "plain js should carry the hxnodejs hint");
  }

  @Test
  @DisplayName("js without node on path is refused")
  public void testJsWithoutNodeOnPathIsRefused() {
    String path = fixturePath("targets/js-plain.hxml");
    assertThrows(ExecutionException.class, () -> HaxeTestLaunchPlanner.plan(getProject(), path, null, false));
  }

  @Test
  @DisplayName("non host runnable target is refused")
  public void testNonHostRunnableTargetIsRefused() {
    String path = fixturePath("targets/cs.hxml");
    assertThrows(ExecutionException.class, () -> HaxeTestLaunchPlanner.plan(getProject(), path, null, false));
  }

  @Test
  @DisplayName("swf runs under adl when an air sdk resolves and is refused otherwise")
  public void testSwfRunsUnderAdlWhenAnAirSdkResolvesAndIsRefusedOtherwise() throws ExecutionException {
    String path = fixturePath("targets/swf.hxml");
    // the AIR_SDK env fallback is the only adl source in the test fixture
    // (no Flex/AIR SDK entry exists in its SDK table)
    if (!adlAvailable()) {
      assertThrows(ExecutionException.class, () -> HaxeTestLaunchPlanner.plan(getProject(), path, null, false));
      return;
    }

    Plan plan = HaxeTestLaunchPlanner.plan(getProject(), path, null, false);
    assertEquals(HaxeTarget.FLASH, plan.target());
    assertTrue(plan.command().get(0).endsWith(HaxeSdkUtilBase.getExecutableName("adl")),
               "adl hosts the swf: " + plan.command());
    assertEquals("-nodebug", plan.command().get(1), "the default debug-launch mode swallows trace output");
    assertTrue(plan.command().get(2).endsWith("application.xml"), "generated descriptor expected: " + plan.command());
    assertTrue(plan.command().get(3).replace('\\', '/').endsWith("/bin"),
               "the content root is the swf's directory: " + plan.command());

    Plan debug = HaxeTestLaunchPlanner.planForDebug(getProject(), path, null);
    assertFalse(debug.command().contains("-nodebug"),
                "a debug launch keeps adl's default mode - the -debug swf dials the waiting fdb: " + debug.command());
    assertTrue(debug.command().get(1).endsWith("application.xml"),
               "the descriptor follows adl directly in the debug shape: " + debug.command());
  }

  @Test
  @DisplayName("flash build forces the live reporter into the compile")
  public void testFlashBuildForcesTheLiveReporterIntoTheCompile() {
    withLiveReportingDisabled(() -> {
      // the adl-hosted lane needs the injected reporter for stdout output and the exit call
      String arguments = HaxeTestCompileArguments.compileArguments(getProject(), fixturePath("targets/swf.hxml"), null);
      assertTrue(arguments.contains("intellij_utest.Macro.init()"), "reporter injection missing: " + arguments);
    });
  }

  @Test
  @DisplayName("chained hxml defaults to its first section")
  public void testChainedHxmlDefaultsToItsFirstSection() throws ExecutionException {
    String path = fixturePath("targets/chained.hxml");

    // default selection is the first section (interp): a single compile-and-run
    // process built from the section's own lines - the file argument is replaced,
    // so the appended reporting arguments belong to this section, and the
    // sibling hl build never compiles
    Plan interpPlan = HaxeTestLaunchPlanner.plan(getProject(), path, null, false);
    assertTrue(interpPlan.singleStage(), "first section is interp");
    assertTrue(interpPlan.command().contains("--interp"), "section arguments expected: " + interpPlan.command());
    assertTrue(interpPlan.command().containsAll(List.of("-lib", "utest")),
               "the --each block applies to every section: " + interpPlan.command());
    assertFalse(interpPlan.command().contains("chained.hxml"), "file argument must be replaced: " + interpPlan.command());
    assertFalse(interpPlan.command().contains("-hl"), "sibling section must not compile: " + interpPlan.command());
  }

  @Test
  @DisplayName("stored section selection picks the sibling section")
  public void testStoredSectionSelectionPicksTheSiblingSection() throws ExecutionException {
    String path = fixturePath("targets/chained.hxml");
    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);

    // selections are stored by section identity - inline sections carry the
    // build file's own name with an occurrence suffix
    HaxeSectionSelectionStore.getInstance(getProject()).setSelectedSection(file, "chained.hxml#2");
    Plan hlPlan = HaxeTestLaunchPlanner.plan(getProject(), path, null, false);
    assertFalse(hlPlan.singleStage(), "the hl section compiles in the before-run step");
    assertTrue(hlPlan.command().get(1).endsWith("tests.hl"), "artifact path expected: " + hlPlan.command());
  }

  @Test
  @DisplayName("munit build is detected from its lib and compiles with the injected client")
  public void testMunitBuildIsDetectedFromItsLibAndCompilesWithTheInjectedClient() throws ExecutionException {
    String path = fixturePath("targets/munit-neko.hxml");
    assertEquals("munit", HaxeTestFrameworks.forBuildFile(getProject(), path).libraryName());

    String arguments = HaxeTestCompileArguments.compileArguments(getProject(), path, null);
    assertTrue(arguments.contains("--macro intellij_munit.Macro.init()"),
               "the injected client is munit's result channel: " + arguments);
    assertFalse(arguments.contains("-D teamcity "), "utest's batch define has no meaning for munit: " + arguments);

    Plan plan = HaxeTestLaunchPlanner.plan(getProject(), path, null, false);
    assertEquals(HaxeTarget.NEKO, plan.target());
  }

  @Test
  @DisplayName("buddy build is detected from its lib and compiles with the reporter define")
  public void testBuddyBuildIsDetectedFromItsLibAndCompilesWithTheReporterDefine() throws ExecutionException {
    String path = fixturePath("targets/buddy-interp.hxml");
    assertEquals("buddy", HaxeTestFrameworks.forBuildFile(getProject(), path).libraryName());

    String arguments = HaxeTestCompileArguments.compileArguments(getProject(), path, null);
    assertTrue(arguments.contains("-D reporter=intellij_buddy.TcReporter"),
               "buddy's reporter override selects the shipped reporter: " + arguments);

    Plan plan = HaxeTestLaunchPlanner.plan(getProject(), path, null, false);
    assertTrue(plan.singleStage(), "buddy runs on the interpreter");
  }

  @Test
  @DisplayName("tink build is detected from its lib and compiles with the injected reporter")
  public void testTinkBuildIsDetectedFromItsLibAndCompilesWithTheInjectedReporter() throws ExecutionException {
    String path = fixturePath("targets/tink-interp.hxml");
    assertEquals("tink_unittest", HaxeTestFrameworks.forBuildFile(getProject(), path).libraryName());

    String arguments = HaxeTestCompileArguments.compileArguments(getProject(), path, null);
    assertTrue(arguments.contains("--macro intellij_tink.Macro.init()"),
               "the injected reporter is tink's result channel: " + arguments);
    assertFalse(arguments.contains("-D teamcity "), "utest's batch define has no meaning for tink: " + arguments);

    Plan plan = HaxeTestLaunchPlanner.plan(getProject(), path, null, false);
    assertTrue(plan.singleStage(), "tink runs on the interpreter");
  }

  @Test
  @DisplayName("munit on the interpreter is refused")
  public void testMunitOnTheInterpreterIsRefused() {
    // munit predates the eval target and hangs there - verified live
    String path = fixturePath("targets/munit-interp.hxml");
    ExecutionException refusal =
      assertThrows(ExecutionException.class, () -> HaxeTestLaunchPlanner.plan(getProject(), path, null, false));
    assertTrue(refusal.getMessage().contains("munit"), refusal.getMessage());
  }

  @Test
  @DisplayName("test debuggability follows the shared target support")
  public void testTestDebuggabilityFollowsTheSharedTargetSupport() {
    // the target-to-lane matrix itself is HaxeDebugSupport's pin; covered here
    // is only what the planner adds on top of it
    assertTrue(HaxeTestLaunchPlanner.isDebuggableTarget(getProject(), fixturePath("test.hxml")),
               "no target flag means interp - the eval lane debugs it");

    // the CURRENT target selection is read on every ask, never cached
    String limePath = fixturePath("targets/lime-project.xml");
    VirtualFile limeFile = LocalFileSystem.getInstance().findFileByPath(limePath);
    HaxeTargetSelectionStore.getInstance(getProject()).setSelectedTargetId(limeFile, "Neko");
    assertFalse(HaxeTestLaunchPlanner.isDebuggableTarget(getProject(), limePath), "neko has no debugger lane");
    HaxeTargetSelectionStore.getInstance(getProject()).setSelectedTargetId(limeFile, "Windows");
    assertTrue(HaxeTestLaunchPlanner.isDebuggableTarget(getProject(), limePath),
               "lime desktop builds debug through the hxcpp lane");
  }

  @Test
  @DisplayName("single stage detection separates interp from artifact builds")
  public void testSingleStageDetectionSeparatesInterpFromArtifactBuilds() {
    assertTrue(HaxeTestLaunchPlanner.isSingleStage(getProject(), fixturePath("test.hxml")));
    assertFalse(HaxeTestLaunchPlanner.isSingleStage(getProject(), fixturePath("targets/hl.hxml")));
    assertTrue(HaxeTestLaunchPlanner.isSingleStage(getProject(), "missing/nowhere.hxml"),
               "unresolvable files get no compile step - checkConfiguration reports the problem");
  }

  @Test
  @DisplayName("compile arguments join activation filter suite name and reporter injection")
  public void testCompileArgumentsJoinActivationFilterSuiteNameAndReporterInjection() {
    // the suite-name define overrides utest's target #if chain, which lacks
    // several targets (HL runs would read "Target: Undefined")
    String interp = fixturePath("test.hxml");
    String arguments = HaxeTestCompileArguments.compileArguments(getProject(), interp, null);
    assertTrue(arguments.startsWith("-D teamcity -D \"teamcity_suite_name=Target: Interpretation\""),
               "activation and suite-name defines expected: " + arguments);
    assertTrue(arguments.contains("--macro intellij_utest.Macro.init()"),
               "the live reporter injection is on by default: " + arguments);

    String filtered = HaxeTestCompileArguments.compileArguments(getProject(), fixturePath("targets/hl.hxml"), "A.testX");
    assertTrue(filtered.startsWith("-D teamcity -D \"teamcity_suite_name=Target: HashLink\""),
               "activation and suite name lead: " + filtered);
    assertTrue(filtered.endsWith("-D UTEST_PATTERN=A.testX"),
               "the filter define trails the reporting block: " + filtered);
  }

  @Test
  @DisplayName("lime tests build launches the packaged binary")
  public void testLimeTestsBuildLaunchesThePackagedBinary() throws ExecutionException {
    String path = fixturePath("targets/lime-project.xml");
    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
    HaxeTargetSelectionStore.getInstance(getProject()).setSelectedTargetId(file, "Neko");

    Plan plan = HaxeTestLaunchPlanner.plan(getProject(), path, null, false);
    assertFalse(plan.singleStage(), "lime builds compile in the before-run step");
    assertEquals(HaxeTarget.NEKO, plan.target());
    String binary = plan.command().get(0).replace('\\', '/');
    assertTrue(binary.endsWith("export/neko/bin/" + HaxeSdkUtilBase.getExecutableName("LimeTests")),
               "lime packages `<app path>/<target>/bin/<app file>`: " + plan.command());
  }

  @Test
  @DisplayName("lime compile arguments use the tool forwarding spelling")
  public void testLimeCompileArgumentsUseTheToolForwardingSpelling() {
    String path = fixturePath("targets/lime-project.xml");
    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
    HaxeTargetSelectionStore.getInstance(getProject()).setSelectedTargetId(file, "Neko");

    String arguments = HaxeTestCompileArguments.compileArguments(getProject(), path, "A.testX");
    // ATTACHED defines: the two-word -D spelling trips a lime bug duplicating the value
    assertTrue(arguments.startsWith("-Dteamcity \"-Dteamcity_suite_name=Target: Neko\""),
               "lime spelling expected: " + arguments);
    assertTrue(arguments.endsWith("-DUTEST_PATTERN=A.testX"),
               "the filter define trails the reporting block: " + arguments);
    assertTrue(arguments.contains("--source="), "the reporter classpath rides lime's --source: " + arguments);
    assertTrue(arguments.contains("\"--haxeflag=--macro intellij_utest.Macro.init()\""),
               "the reporter macro rides lime's --haxeflag: " + arguments);
  }

  @Test
  @DisplayName("lime hl package exposes its boot bytecode for the debugger")
  public void testLimeHlPackageExposesItsBootBytecodeForTheDebugger() throws ExecutionException {
    String path = fixturePath("targets/lime-project.xml");
    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
    HaxeTargetSelectionStore.getInstance(getProject()).setSelectedTargetId(file, "HashLink");

    Plan plan = HaxeTestLaunchPlanner.plan(getProject(), path, null, false);
    assertEquals(HaxeTarget.HL, plan.target());
    assertNull(HaxeTestLaunchPlanner.hlArtifact(plan), "a lime package is not the bare [hl, artifact] shape");
    Path boot = HaxeTestLaunchPlanner.packagedHlBoot(plan);
    assertNotNull(boot, "the packaged runtime debugs its hlboot.dat");
    assertTrue(boot.toString().replace('\\', '/').endsWith("export/hl/bin/hlboot.dat"), boot.toString());
  }

  @Test
  @DisplayName("non host lime targets are rejected")
  public void testNonHostLimeTargetsAreRejected() {
    String path = fixturePath("targets/lime-project.xml");
    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
    HaxeTargetSelectionStore.getInstance(getProject()).setSelectedTargetId(file, "Android");

    assertThrows(ExecutionException.class, () -> HaxeTestLaunchPlanner.plan(getProject(), path, null, false),
                 "mobile targets have no host to run tests on");
  }

  @Test
  @DisplayName("nme tests build launches the packaged neko launcher")
  public void testNmeTestsBuildLaunchesThePackagedNekoLauncher() throws ExecutionException {
    String path = fixturePath("targets/tests.nmml");
    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
    HaxeTargetSelectionStore.getInstance(getProject()).setSelectedTargetId(file, "Neko");

    Plan plan = HaxeTestLaunchPlanner.plan(getProject(), path, null, false);
    assertFalse(plan.singleStage(), "nme builds compile in the before-run step");
    assertEquals(HaxeTarget.NEKO, plan.target());
    String binary = plan.command().get(0).replace('\\', '/');
    assertTrue(binary.contains("-neko/NmeTests/"),
               "nme packages `<root>/<host>-neko/<app>/<launcher>`: " + plan.command());
    assertTrue(binary.endsWith("/" + HaxeSdkUtilBase.getExecutableName("NmeTests")), binary);
  }

  @Test
  @DisplayName("nme compile arguments use the tool forwarding spelling")
  public void testNmeCompileArgumentsUseTheToolForwardingSpelling() {
    String path = fixturePath("targets/tests.nmml");
    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
    HaxeTargetSelectionStore.getInstance(getProject()).setSelectedTargetId(file, "Neko");

    String arguments = HaxeTestCompileArguments.compileArguments(getProject(), path, null);
    assertTrue(arguments.startsWith("-Dteamcity \"-Dteamcity_suite_name=Target: Neko\""),
               "attached defines expected: " + arguments);
    // nme swallows single-dash haxe flags; the classpath must ride --class-path
    assertTrue(arguments.contains("\"--class-path "), "classpath spelling expected: " + arguments);
    assertTrue(arguments.contains("\"--macro intellij_utest.Macro.init()\""),
               "the reporter macro rides a double-dash token: " + arguments);
  }

  @Test
  @DisplayName("munit packaged builds inject the munit client in the tool spelling")
  public void testMunitPackagedBuildsInjectTheMunitClientInTheToolSpelling() {
    VirtualFile limeFile = LocalFileSystem.getInstance().findFileByPath(fixturePath("targets/munit-lime-project.xml"));
    HaxeTargetSelectionStore.getInstance(getProject()).setSelectedTargetId(limeFile, "Neko");
    String lime = HaxeTestCompileArguments.compileArguments(getProject(), limeFile.getPath(), null);
    assertTrue(lime.contains("\"--haxeflag=--macro intellij_munit.Macro.init()\""),
               "munit's client rides lime's --haxeflag: " + lime);
    assertFalse(lime.contains("-Dteamcity "), "utest's batch define has no meaning for munit: " + lime);

    VirtualFile nmeFile = LocalFileSystem.getInstance().findFileByPath(fixturePath("targets/munit-tests.nmml"));
    HaxeTargetSelectionStore.getInstance(getProject()).setSelectedTargetId(nmeFile, "Neko");
    String nme = HaxeTestCompileArguments.compileArguments(getProject(), nmeFile.getPath(), null);
    assertTrue(nme.contains("\"--macro intellij_munit.Macro.init()\""),
               "munit's client rides an nme double-dash token: " + nme);
  }

  @Test
  @DisplayName("disabling live test reporting drops the injection")
  public void testDisablingLiveTestReportingDropsTheInjection() {
    withLiveReportingDisabled(() ->
      assertEquals("-D teamcity -D \"teamcity_suite_name=Target: Interpretation\"",
                   HaxeTestCompileArguments.compileArguments(getProject(), fixturePath("test.hxml"), null)));
  }

  /** Runs the body with live test reporting OFF, restoring the setting after. */
  private void withLiveReportingDisabled(Runnable body) {
    HaxeBuildToolSettings settings = HaxeBuildToolSettings.getInstance(getProject());
    boolean before = settings.isLiveTestReporting();
    settings.setLiveTestReporting(false);
    try {
      body.run();
    }
    finally {
      settings.setLiveTestReporting(before);
    }
  }

  /** The `--source=` entry holding the generated main (the reporter classpath is the other one). */
  private static Path generatedSourceRoot(List<String> arguments) {
    return arguments.stream()
      .filter(argument -> argument.startsWith("--source="))
      .map(argument -> Path.of(argument.substring("--source=".length())))
      .filter(directory -> Files.exists(directory.resolve(HaxeTestSingleRuns.MAIN_CLASS + ".hx")))
      .findFirst()
      .orElseThrow(() -> new AssertionError("no --source= entry holds the generated main: " + arguments));
  }
}
