package com.intellij.plugins.haxe.v2.runconfig;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.runner.debugger.interp.InterpRunConfiguration;
import com.intellij.plugins.haxe.v2.buildsystem.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Run configurations: program launches")
public class HaxeProgramLaunchesTest extends HaxeCodeInsightFixtureTestCase {
  private static final String INTERP_HXML_NAME = "interp.hxml";
  private static final String INTERP_HXML_SOURCE = """
    -cp src
    -main Main
    --interp
    """;

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

  @Test
  @DisplayName("interpreter build launches through the haxe interpreter")
  public void testInterpreterBuildLaunchesThroughTheHaxeInterpreter() {
    HaxeBuildFileInfo info = HxmlFileParser.parse(INTERP_HXML_SOURCE);

    String launchKind = HaxeProgramLaunches.launchKind(info, HaxeBuildFileType.HXML);

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
