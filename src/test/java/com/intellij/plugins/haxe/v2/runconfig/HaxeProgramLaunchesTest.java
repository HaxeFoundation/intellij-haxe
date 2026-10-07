package com.intellij.plugins.haxe.v2.runconfig;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.runner.debugger.flash.AirRunConfiguration;
import com.intellij.plugins.haxe.runner.debugger.flash.FlashRunConfiguration;
import com.intellij.plugins.haxe.runner.debugger.hashlink.HashLinkRunConfiguration;
import com.intellij.plugins.haxe.runner.debugger.hxcpp.intellij.HxcppIntellijRunConfiguration;
import com.intellij.plugins.haxe.runner.debugger.interp.InterpRunConfiguration;
import com.intellij.plugins.haxe.runner.neko.NekoRunConfiguration;
import com.intellij.plugins.haxe.v2.buildsystem.*;
import com.intellij.plugins.haxe.v2.runconfig.HaxeProgramLaunches.LaunchSpec;
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
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    // a plain hxml -debug build renames the executable; no single path serves run and debug
    arguments(HaxeTarget.CPP, "out/cpp", HaxeBuildFileType.HXML, null, null),
    arguments(HaxeTarget.NEKO, "out/game.n", HaxeBuildFileType.HXML, null, NekoRunConfiguration.class),
    arguments(HaxeTarget.NEKO, "bin/neko/bin/Game.exe", HaxeBuildFileType.LIME, "neko", NekoRunConfiguration.class),
    // the interpreter writes no output; "" is what launchOutput hands over
    arguments(HaxeTarget.INTERP, "", HaxeBuildFileType.HXML, null, InterpRunConfiguration.class));

  @TempDir
  Path projectDirectory;

  @Override
  protected String getBasePath() {
    return "";
  }

  @AfterEach
  public void removeInterpreterConfigurations() {
    RunManager runManager = RunManager.getInstance(getProject());
    List<RunnerAndConfigurationSettings> created = runManager.getAllSettings()
      .stream()
      .filter(settings -> settings.getConfiguration() instanceof InterpRunConfiguration)
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
    LaunchSpec spec = HaxeProgramLaunches.specFor(target, targetOutput, type, targetFlag);
    assertEquals(expectedConfiguration, spec == null ? null : spec.configurationClass());
  }

  @Test
  @DisplayName("interpreter build launches through the haxe interpreter")
  public void testInterpreterBuildLaunchesThroughTheHaxeInterpreter() throws IOException {
    HaxeBuildFile buildFile = interpHxml();
    HaxeBuildFileInfo info = HxmlFileParser.parse(INTERP_HXML_SOURCE);

    String launchKind = HaxeProgramLaunches.launchKind(getProject(), info, buildFile);

    assertEquals("Haxe Interpreter", launchKind);
    assertEquals("", HaxeProgramLaunches.launchOutput(info));
  }

  @Test
  @DisplayName("interpreter runs without a build step")
  public void testInterpreterRunsWithoutABuildStep() {
    assertFalse(HaxeProgramLaunches.compilesBeforeLaunch(HaxeTarget.INTERP));
    assertTrue(HaxeProgramLaunches.compilesBeforeLaunch(HaxeTarget.HL));
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
  @DisplayName("interpreter configuration is reused")
  public void testInterpreterConfigurationIsReused() throws IOException {
    HaxeBuildFile buildFile = interpHxml();

    RunnerAndConfigurationSettings first = findOrCreate(buildFile);
    RunnerAndConfigurationSettings second = findOrCreate(buildFile);

    assertSame(first, second);
  }

  private HaxeBuildFile interpHxml() throws IOException {
    Path hxml = Files.writeString(projectDirectory.resolve(INTERP_HXML_NAME), INTERP_HXML_SOURCE);
    VirtualFile file = Objects.requireNonNull(LocalFileSystem.getInstance().refreshAndFindFileByNioFile(hxml));
    return new HaxeBuildFile(file, HaxeBuildFileType.HXML);
  }

  private RunnerAndConfigurationSettings findOrCreate(HaxeBuildFile buildFile) {
    return HaxeProgramLaunches.findOrCreate(getProject(), myFixture.getModule(), buildFile, HaxeTarget.INTERP, "");
  }

  private static InterpRunConfiguration interpConfiguration(RunnerAndConfigurationSettings settings) {
    return (InterpRunConfiguration)Objects.requireNonNull(settings).getConfiguration();
  }
}
