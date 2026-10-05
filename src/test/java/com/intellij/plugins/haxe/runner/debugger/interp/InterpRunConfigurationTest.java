package com.intellij.plugins.haxe.runner.debugger.interp;

import com.intellij.openapi.project.ProjectUtil;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.runner.HaxeRunConfigurationType;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Debugger: interpreter run configuration")
public class InterpRunConfigurationTest extends HaxeCodeInsightFixtureTestCase {
  private static final String BUILD_HXML_NAME = "build.hxml";

  @TempDir
  Path workDirectory;

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("interp flag as an argument declares interp")
  public void testInterpFlagAsAnArgumentDeclaresInterp() {
    assertTrue(InterpRunConfiguration.declaresInterp(List.of("-cp", "src", "-main", "Main", "--interp"), workDirectory));
    assertFalse(InterpRunConfiguration.declaresInterp(List.of("-cp", "src", "-main", "Main"), workDirectory));
  }

  @Test
  @DisplayName("hxml with interp target declares interp")
  public void testHxmlWithInterpTargetDeclaresInterp() throws IOException {
    writeHxml(BUILD_HXML_NAME, """
      -cp src
      -main Main
      --interp
      """);

    assertTrue(InterpRunConfiguration.hxmlDeclaresInterp(BUILD_HXML_NAME, workDirectory));
    assertTrue(InterpRunConfiguration.declaresInterp(List.of(BUILD_HXML_NAME, "-D", "verbose"), workDirectory));
  }

  @Test
  @DisplayName("hxml with another target does not declare interp")
  public void testHxmlWithAnotherTargetDoesNotDeclareInterp() throws IOException {
    writeHxml(BUILD_HXML_NAME, """
      -cp src
      -main Main
      -js bin/app.js
      """);

    assertFalse(InterpRunConfiguration.hxmlDeclaresInterp(BUILD_HXML_NAME, workDirectory));
  }

  @Test
  @DisplayName("hxml with interp in a later section declares interp")
  public void testHxmlWithInterpInALaterSectionDeclaresInterp() throws IOException {
    writeHxml(BUILD_HXML_NAME, """
      -cp src
      -main Main
      -js bin/app.js
      --next
      -main Tool
      --interp
      """);

    assertTrue(InterpRunConfiguration.hxmlDeclaresInterp(BUILD_HXML_NAME, workDirectory));
  }

  @Test
  @DisplayName("hxml including an interp hxml declares interp")
  public void testHxmlIncludingAnInterpHxmlDeclaresInterp() throws IOException {
    writeHxml("common.hxml", """
      -cp src
      --interp
      """);
    writeHxml(BUILD_HXML_NAME, """
      common.hxml
      -main Main
      """);

    assertTrue(InterpRunConfiguration.hxmlDeclaresInterp(BUILD_HXML_NAME, workDirectory));
  }

  @Test
  @DisplayName("missing hxml does not declare interp")
  public void testMissingHxmlDoesNotDeclareInterp() {
    assertFalse(InterpRunConfiguration.hxmlDeclaresInterp("absent.hxml", workDirectory));
    assertFalse(InterpRunConfiguration.hxmlDeclaresInterp("bad:name.hxml", workDirectory));
  }

  @Test
  @DisplayName("working directory resolves against the module")
  public void testWorkingDirectoryResolvesAgainstTheModule() {
    InterpRunConfiguration configuration = newConfiguration();
    configuration.setModule(myFixture.getModule());
    Path moduleDir = moduleDir();

    configuration.setWorkingDirectory("bin");
    assertEquals(moduleDir.resolve("bin"), configuration.resolveWorkingDirectory());

    configuration.setWorkingDirectory(workDirectory.toString());
    assertEquals(workDirectory, configuration.resolveWorkingDirectory());

    configuration.setWorkingDirectory("");
    assertEquals(moduleDir, configuration.resolveWorkingDirectory());
  }

  @Test
  @DisplayName("working directory falls back to the project without a module")
  public void testWorkingDirectoryFallsBackToTheProjectWithoutAModule() {
    InterpRunConfiguration configuration = newConfiguration();
    Path projectBase = Path.of(getProject().getBasePath());

    configuration.setWorkingDirectory("bin");
    assertEquals(projectBase.resolve("bin"), configuration.resolveWorkingDirectory());

    configuration.setWorkingDirectory("");
    assertEquals(projectBase, configuration.resolveWorkingDirectory());
  }

  private Path moduleDir() {
    return Path.of(Objects.requireNonNull(ProjectUtil.guessModuleDir(myFixture.getModule())).getPath());
  }

  private void writeHxml(String name, String source) throws IOException {
    Files.writeString(workDirectory.resolve(name), source);
  }

  private InterpRunConfiguration newConfiguration() {
    HaxeRunConfigurationType type = HaxeRunConfigurationType.getInstance();
    return new InterpRunConfiguration("interp", getProject(), new InterpConfigurationFactory(type));
  }
}
