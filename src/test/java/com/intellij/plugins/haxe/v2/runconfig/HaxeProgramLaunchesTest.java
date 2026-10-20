package com.intellij.plugins.haxe.v2.runconfig;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.SearchScopeProvidingRunProfile;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.execution.console.HaxeLaunchSearchScope;
import com.intellij.plugins.haxe.runner.debugger.browser.BrowserRunConfiguration;
import com.intellij.plugins.haxe.runner.debugger.flash.AirRunConfiguration;
import com.intellij.plugins.haxe.runner.debugger.flash.FlashRunConfiguration;
import com.intellij.plugins.haxe.runner.debugger.hashlink.HashLinkRunConfiguration;
import com.intellij.plugins.haxe.runner.debugger.hxcpp.intellij.HxcppIntellijRunConfiguration;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapRunConfigurationBase;
import com.intellij.plugins.haxe.runner.debugger.interp.InterpRunConfiguration;
import com.intellij.plugins.haxe.runner.neko.NekoRunConfiguration;
import com.intellij.plugins.haxe.v2.buildsystem.*;
import com.intellij.plugins.haxe.v2.runconfig.HaxeLaunchKind.Parameter;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFileInfo.HaxeDefine;
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildSystem;
import com.intellij.plugins.haxe.v2.buildtools.HaxeBuildWorkDirectories;
import com.intellij.plugins.haxe.util.HaxeSdkUtilBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@DisplayName("Run configurations: program launches")
public class HaxeProgramLaunchesTest extends HaxeCodeInsightFixtureTestCase {
  private static final String INTERP_HXML_NAME = "interp.hxml";
  private static final String INTERP_HXML_SOURCE = """
    -cp src
    -main Main
    --interp
    """;

  /** (compile target, target output, build file type, selected lime/nme target flag, expected configuration class). */
  static final List<Arguments> LAUNCH_SPECS = List.of(
    // the selected flag decides AIR vs Flash, not an "/air/bin/" segment of the output path
    arguments(HaxeTarget.FLASH, "bin/air/bin/Game.swf", HaxeBuildFileType.LIME, "air", AirRunConfiguration.class),
    arguments(HaxeTarget.FLASH, "bin/airdist/bin/Game.swf", HaxeBuildFileType.OPENFL, "air", AirRunConfiguration.class),
    arguments(HaxeTarget.FLASH, "bin/air/bin/Game.swf", HaxeBuildFileType.LIME, "flash", FlashRunConfiguration.class),
    arguments(HaxeTarget.FLASH, "out/game.swf", HaxeBuildFileType.HXML, null, FlashRunConfiguration.class),
    arguments(HaxeTarget.FLASH, "out/game.swc", HaxeBuildFileType.HXML, null, null),
    arguments(HaxeTarget.HL, "out/game.hl", HaxeBuildFileType.HXML, null, HashLinkRunConfiguration.class),
    // HL/C output is a source directory, not runnable bytecode
    arguments(HaxeTarget.HL, "out/main.c", HaxeBuildFileType.HXML, null, null),
    arguments(HaxeTarget.CPP, "bin/windows/bin/Game.exe", HaxeBuildFileType.LIME, "windows", HxcppIntellijRunConfiguration.class),
    // a plain hxml C++ build launches its executable; the configuration resolves the name per launch
    arguments(HaxeTarget.CPP, "out/cpp", HaxeBuildFileType.HXML, null, HxcppIntellijRunConfiguration.class),
    arguments(HaxeTarget.NEKO, "out/game.n", HaxeBuildFileType.HXML, null, NekoRunConfiguration.class),
    arguments(HaxeTarget.NEKO, "bin/neko/bin/Game.exe", HaxeBuildFileType.LIME, "neko", NekoRunConfiguration.class),
    // the interpreter writes no output; "" is what launchOutput hands over
    arguments(HaxeTarget.INTERP, "", HaxeBuildFileType.HXML, null, InterpRunConfiguration.class),
    arguments(HaxeTarget.JAVA_SCRIPT, "out/client.js", HaxeBuildFileType.HXML, null, BrowserRunConfiguration.class),
    arguments(HaxeTarget.JAVA_SCRIPT, "bin/html5/bin/Game.js", HaxeBuildFileType.LIME, "html5", BrowserRunConfiguration.class),
    // electron runs the output in Electron, which no configuration launches
    arguments(HaxeTarget.JAVA_SCRIPT, "bin/electron/bin/Game.js", HaxeBuildFileType.LIME, "electron", null));

  /** (hxml source, expected Build & run kinds in row order). */
  static final List<Arguments> JS_LAUNCH_KINDS = List.of(
    arguments("-main Main\n-js out/client.js", List.of(HaxeLaunchKind.BROWSER, HaxeLaunchKind.NODE)),
    arguments("-main Main\n-lib hxnodejs\n-js out/server.js", List.of(HaxeLaunchKind.NODE, HaxeLaunchKind.BROWSER)),
    arguments("-main Main\n-D nodejs\n-js out/server.js", List.of(HaxeLaunchKind.NODE, HaxeLaunchKind.BROWSER)));

  @TempDir
  Path projectDirectory;

  @Override
  protected String getBasePath() {
    return "";
  }

  @AfterEach
  public void removeCreatedConfigurations() {
    RunManager runManager = RunManager.getInstance(getProject());
    List<RunnerAndConfigurationSettings> created = runManager.getAllSettings()
      .stream()
      .filter(settings -> settings.getConfiguration() instanceof DapRunConfigurationBase)
      .toList();
    created.forEach(runManager::removeConfiguration);
  }

  @ParameterizedTest(name = "{0} {1} ({2}, {3})")
  @FieldSource("LAUNCH_SPECS")
  public void testSpecFor(HaxeTarget target,
                          String targetOutput,
                          HaxeBuildFileType type,
                          String targetFlag,
                          Class<?> expectedConfiguration) {
    HaxeLaunchKind kind = HaxeProgramLaunches.kindFor(target, targetOutput, type, targetFlag, List.of());
    assertEquals(expectedConfiguration, kind == null ? null : kind.configurationClass());
  }

  @Test
  @DisplayName("interpreter build launches through the haxe interpreter")
  public void testInterpreterBuildLaunchesThroughTheHaxeInterpreter() throws IOException {
    HaxeBuildFile buildFile = interpHxml();
    HaxeBuildFileInfo info = HxmlFileParser.parse(INTERP_HXML_SOURCE);

    List<HaxeLaunchKind> launchKinds = HaxeProgramLaunches.launchKinds(getProject(), info, buildFile);

    assertEquals(List.of(HaxeLaunchKind.INTERP), launchKinds);
    assertEquals("Haxe Interpreter", HaxeLaunchKind.INTERP.displayName());
    assertEquals("", HaxeProgramLaunches.launchOutput(info));
  }

  @ParameterizedTest(name = "{0}")
  @FieldSource("JS_LAUNCH_KINDS")
  @DisplayName("javascript hxml launches in a browser and on node, a node build node first")
  public void testJavascriptHxmlLaunchesInABrowserAndOnNode(String source, List<HaxeLaunchKind> expectedKinds) throws IOException {
    HaxeBuildFile buildFile = hxml("web.hxml", source);
    HaxeBuildFileInfo info = HxmlFileParser.parse(source);

    List<HaxeLaunchKind> kinds = HaxeProgramLaunches.launchKinds(getProject(), info, buildFile);

    assertEquals(expectedKinds, kinds);
  }

  @Test
  @DisplayName("node runs the script from its folder on the resolved runtime")
  public void testNodeRunsTheScriptFromItsFolderOnTheResolvedRuntime() throws IOException {
    HaxeBuildFile buildFile = hxml("server.hxml", "-main Main\n-js out/server.js");

    Map<Parameter, String> parameters =
      HaxeProgramLaunches.defaultParameters(getProject(), buildFile, HaxeLaunchKind.NODE, "out/server.js");

    // the VFS spells the folder with its long name; a temp dir may come as a short 8.3 alias on windows
    Path script = Path.of(buildFile.file().getParent().getPath()).resolve("out/server.js");
    assertEquals(Map.of(
      Parameter.EXECUTABLE, "",
      Parameter.PROGRAM_ARGUMENTS, script.toString(),
      Parameter.WORKING_DIRECTORY, script.getParent().toString()), parameters);
  }

  @Test
  @DisplayName("interpreter runs without a build step")
  public void testInterpreterRunsWithoutABuildStep() {
    assertFalse(HaxeLaunchKind.INTERP.compilesBeforeLaunch());
    assertTrue(HaxeLaunchKind.HASHLINK.compilesBeforeLaunch());
  }

  @Test
  @DisplayName("interpreter configuration runs the hxml like the build action")
  public void testInterpreterConfigurationRunsTheHxmlLikeTheBuildAction() throws IOException {
    HaxeBuildFile buildFile = interpHxml();

    InterpRunConfiguration configuration = interpConfiguration(findOrCreate(buildFile));

    Path hxmlFolder = Path.of(buildFile.file().getParent().getPath());
    assertEquals(INTERP_HXML_NAME, configuration.getCompilerArguments());
    assertEquals(hxmlFolder, configuration.resolveWorkingDirectory());
    assertTrue(configuration.getBeforeRunTasks().isEmpty(), "compiling is the run - no build step before it");
  }

  @Test
  @DisplayName("the interpreter configuration scopes console links by its hxml")
  public void testTheInterpreterConfigurationScopesConsoleLinksByItsHxml() throws IOException {
    HaxeBuildFile buildFile = interpHxml();
    InterpRunConfiguration configuration = interpConfiguration(findOrCreate(buildFile));

    List<VirtualFile> preferred = HaxeLaunchSearchScope.preferredDirectoriesOf(configuration.getSearchScope());

    assertEquals(List.of(buildFile.file().getParent()), preferred, "the hxml's folder: its work directory and implicit classpath");
  }

  @Test
  @DisplayName("a build and run configuration scopes console links by its build step")
  public void testABuildAndRunConfigurationScopesConsoleLinksByItsBuildStep() throws IOException {
    HaxeBuildFile buildFile = interpHxml();
    RunnerAndConfigurationSettings settings =
      buildAndRun(buildFile, HaxeTarget.HL, "out/game.hl");

    assertNotNull(settings);
    RunConfiguration configuration = settings.getConfiguration();
    assertInstanceOf(SearchScopeProvidingRunProfile.class, configuration);
    List<VirtualFile> preferred =
      HaxeLaunchSearchScope.preferredDirectoriesOf(((SearchScopeProvidingRunProfile)configuration).getSearchScope());
    assertEquals(List.of(buildFile.file().getParent()), preferred, "the build step's hxml folder");
  }

  @Test
  @DisplayName("interpreter configuration is reused")
  public void testInterpreterConfigurationIsReused() throws IOException {
    HaxeBuildFile buildFile = interpHxml();

    RunnerAndConfigurationSettings first = findOrCreate(buildFile);
    RunnerAndConfigurationSettings second = findOrCreate(buildFile);

    assertSame(first, second);
  }

  @Test
  @DisplayName("a launching action gets a configuration of its own keyed by its name")
  public void testALaunchingActionGetsAConfigurationOfItsOwnKeyedByItsName() throws IOException {
    HaxeBuildFile buildFile = interpHxml();
    RunnerAndConfigurationSettings buildAndRun =
      buildAndRun(buildFile, HaxeTarget.HL, "out/game.hl");
    assertNotNull(buildAndRun);

    RunnerAndConfigurationSettings release = HaxeProgramLaunches.findOrCreate(
      getProject(), myFixture.getModule(), buildFile, HaxeLaunchKind.HASHLINK, "out/game.hl", "release", Map.of(), false);
    RunnerAndConfigurationSettings releaseAgain = HaxeProgramLaunches.findOrCreate(
      getProject(), myFixture.getModule(), buildFile, HaxeLaunchKind.HASHLINK, "out/game.hl", "release", Map.of(), false);

    assertNotSame(buildAndRun, release, "a different build action is a different configuration");
    assertSame(release, releaseAgain);
    assertEquals(INTERP_HXML_NAME + " release (HashLink)", release.getName(), "the action's configuration carries its name");
    assertEquals(INTERP_HXML_NAME + " (HashLink)", buildAndRun.getName());
    HaxeActionBeforeRunTaskProvider.Task step = buildStepOf(release);
    assertEquals("release", step.getActionName());
    assertFalse(step.isInjectDebugArguments());
  }

  @Test
  @DisplayName("parameters default from the build file and a blank override keeps the default")
  public void testParametersDefaultFromTheBuildFileAndABlankOverrideKeepsTheDefault() throws IOException {
    HaxeBuildFile buildFile = interpHxml();
    Path hlFile = Path.of(buildFile.file().getParent().getPath()).resolve("out/game.hl");
    Map<Parameter, String> defaults =
      HaxeProgramLaunches.defaultParameters(getProject(), buildFile, HaxeLaunchKind.HASHLINK, "out/game.hl");
    assertEquals(Map.of(Parameter.HL_FILE, hlFile.toString()), defaults);

    HashLinkRunConfiguration defaulted = hashLinkConfiguration(HaxeProgramLaunches.findOrCreate(
      getProject(), myFixture.getModule(), buildFile, HaxeLaunchKind.HASHLINK, "out/game.hl", "release",
      Map.of(Parameter.HL_FILE, "  "), true));
    assertEquals(hlFile.toString(), defaulted.getHlFilePath());

    HashLinkRunConfiguration overridden = hashLinkConfiguration(HaxeProgramLaunches.findOrCreate(
      getProject(), myFixture.getModule(), buildFile, HaxeLaunchKind.HASHLINK, "out/game.hl", "final",
      Map.of(Parameter.HL_FILE, "dist/final.hl"), true));
    assertEquals("dist/final.hl", overridden.getHlFilePath());
  }

  @Test
  @DisplayName("an existing configuration gets the action as its build step once")
  public void testAnExistingConfigurationGetsTheActionAsItsBuildStepOnce() throws IOException {
    HaxeBuildFile buildFile = interpHxml();
    RunConfiguration existing = Objects.requireNonNull(
      buildAndRun(buildFile, HaxeTarget.HL, "out/game.hl")).getConfiguration();
    assertEquals(1, existing.getBeforeRunTasks().size(), "the Build & run step");

    HaxeProgramLaunches.ensureBuildStep(existing, buildFile, "release", false);
    HaxeProgramLaunches.ensureBuildStep(existing, buildFile, "release", false);

    assertEquals(List.of("release", "Build"), stepActionsOf(existing), "added once, ahead of the existing steps");
  }

  @Test
  @DisplayName("renaming an action retargets its build steps and removing it drops them")
  public void testRenamingAnActionRetargetsItsBuildStepsAndRemovingItDropsThem() throws IOException {
    HaxeBuildFile buildFile = interpHxml();
    String path = buildFile.file().getPath();
    RunConfiguration configuration = Objects.requireNonNull(
      buildAndRun(buildFile, HaxeTarget.HL, "out/game.hl")).getConfiguration();
    HaxeProgramLaunches.ensureBuildStep(configuration, buildFile, "release", true);

    HaxeProgramLaunches.retargetBuildSteps(getProject(), path, "release", "final");
    assertEquals(List.of("final", "Build"), stepActionsOf(configuration));

    HaxeProgramLaunches.retargetBuildSteps(getProject(), path, "final", null);
    assertEquals(List.of("Build"), stepActionsOf(configuration));
  }

  @Test
  @DisplayName("an hxml C++ build linking a library has no launch")
  public void testAnHxmlCppBuildLinkingALibraryHasNoLaunch() {
    List<HaxeDefine> staticLink = List.of(new HaxeDefine("static_link", null));

    assertNull(HaxeProgramLaunches.kindFor(HaxeTarget.CPP, "out/cpp", HaxeBuildFileType.HXML, null, staticLink));
  }

  @Test
  @DisplayName("a created hxml C++ configuration runs the executable its build step writes")
  public void testACreatedHxmlCppConfigurationRunsTheExecutableItsBuildStepWrites() throws IOException {
    HaxeBuildFile buildFile = hxml("cpp.hxml", "--main Main\n-cpp bin/cpp\n");
    RunnerAndConfigurationSettings settings = HaxeProgramLaunches.findOrCreate(
      getProject(), myFixture.getModule(), buildFile, HaxeLaunchKind.HXCPP, "bin/cpp", "Build", Map.of(), true);
    HxcppIntellijRunConfiguration configuration = (HxcppIntellijRunConfiguration)settings.getConfiguration();

    Path cppFolder = HaxeBuildWorkDirectories.resolve(getProject(), buildFile.file(), "bin/cpp");
    assertEquals("${executable}", configuration.getExecutablePath());
    assertEquals(cppFolder.resolve(HaxeSdkUtilBase.getExecutableName("Main")), configuration.resolveExecutableOrNull());
  }

  /** (kind, target output, expected default parameters relative to the hxml folder). */
  static final List<Arguments> HXML_DEFAULTS = List.of(
    arguments(HaxeLaunchKind.BROWSER, "out/web/game.js", Map.of(Parameter.CONTENT_ROOT, "out/web")),
    arguments(HaxeLaunchKind.FLASH, "out/game.swf", Map.of(Parameter.SWF, "out/game.swf")),
    // the neko runtime runs the bytecode: no executable, the .n as the argument
    arguments(HaxeLaunchKind.NEKO, "out/game.n",
              Map.of(Parameter.EXECUTABLE, "", Parameter.PROGRAM_ARGUMENTS, "out/game.n", Parameter.WORKING_DIRECTORY, "out")),
    arguments(HaxeLaunchKind.INTERP, "",
              Map.of(Parameter.WORKING_DIRECTORY, "", Parameter.COMPILER_ARGUMENTS, INTERP_HXML_NAME)));

  @ParameterizedTest(name = "{0}")
  @FieldSource("HXML_DEFAULTS")
  @DisplayName("hxml defaults per kind")
  public void testHxmlDefaultsPerKind(HaxeLaunchKind kind, String targetOutput, Map<Parameter, String> relativeExpected)
    throws IOException {
    HaxeBuildFile buildFile = interpHxml();
    Path folder = Path.of(buildFile.file().getParent().getPath());
    Map<Parameter, String> expected = new EnumMap<>(Parameter.class);
    relativeExpected.forEach((parameter, value) -> expected.put(parameter, resolvedDefault(folder, parameter, value)));

    assertEquals(expected, HaxeProgramLaunches.defaultParameters(getProject(), buildFile, kind, targetOutput));
  }

  /**
   * Path-valued defaults come back absolute under the hxml folder; the empty
   * executable and the file argument stay as they are. The interpreter's
   * working directory is the build work directory as the VFS spells it
   * (forward slashes), not a resolved output path.
   */
  private static String resolvedDefault(Path folder, Parameter parameter, String value) {
    if (value.isEmpty()) {
      return parameter == Parameter.WORKING_DIRECTORY ? FileUtil.toSystemIndependentName(folder.toString()) : value;
    }
    return parameter == Parameter.COMPILER_ARGUMENTS ? value : folder.resolve(value).toString();
  }

  private static List<String> stepActionsOf(RunConfiguration configuration) {
    return configuration.getBeforeRunTasks().stream()
      .map(task -> ((HaxeActionBeforeRunTaskProvider.Task)task).getActionName())
      .toList();
  }

  private static HaxeActionBeforeRunTaskProvider.Task buildStepOf(RunnerAndConfigurationSettings settings) {
    return (HaxeActionBeforeRunTaskProvider.Task)settings.getConfiguration().getBeforeRunTasks().getFirst();
  }

  private static HashLinkRunConfiguration hashLinkConfiguration(RunnerAndConfigurationSettings settings) {
    return (HashLinkRunConfiguration)settings.getConfiguration();
  }

  private HaxeBuildFile hxml(String name, String source) throws IOException {
    Path hxml = Files.writeString(projectDirectory.resolve(name), source);
    VirtualFile file = Objects.requireNonNull(LocalFileSystem.getInstance().refreshAndFindFileByNioFile(hxml));
    return new HaxeBuildFile(file, HaxeBuildFileType.HXML);
  }

  private HaxeBuildFile interpHxml() throws IOException {
    Path hxml = Files.writeString(projectDirectory.resolve(INTERP_HXML_NAME), INTERP_HXML_SOURCE);
    VirtualFile file = Objects.requireNonNull(LocalFileSystem.getInstance().refreshAndFindFileByNioFile(hxml));
    return new HaxeBuildFile(file, HaxeBuildFileType.HXML);
  }

  private RunnerAndConfigurationSettings findOrCreate(HaxeBuildFile buildFile) {
    return buildAndRun(buildFile, HaxeTarget.INTERP, "");
  }

  /** The Build & run row's configuration: the target's kind, the file's default build action, every parameter at its default. */
  private RunnerAndConfigurationSettings buildAndRun(HaxeBuildFile buildFile, HaxeTarget target, String targetOutput) {
    HaxeLaunchKind kind = Objects.requireNonNull(HaxeProgramLaunches.kindFor(target, targetOutput, buildFile.type(), null, List.of()));
    String buildAction = HaxeBuildSystem.of(buildFile.type()).defaultBuildActionName();
    return HaxeProgramLaunches.findOrCreate(getProject(), myFixture.getModule(), buildFile, kind, targetOutput, buildAction, Map.of(), true);
  }

  private static InterpRunConfiguration interpConfiguration(RunnerAndConfigurationSettings settings) {
    return (InterpRunConfiguration)Objects.requireNonNull(settings).getConfiguration();
  }
}
