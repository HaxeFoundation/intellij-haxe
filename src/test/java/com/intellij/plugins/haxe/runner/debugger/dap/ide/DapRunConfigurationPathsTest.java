package com.intellij.plugins.haxe.runner.debugger.dap.ide;

import com.intellij.openapi.project.ProjectUtil;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.runner.HaxeRunConfigurationType;
import com.intellij.plugins.haxe.runner.neko.NekoConfigurationFactory;
import com.intellij.plugins.haxe.runner.neko.NekoRunConfiguration;
import java.nio.file.Path;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The module-relative path rule every DAP run configuration inherits, probed through the neko one. */
@DisplayName("Debugger: run configuration paths")
public class DapRunConfigurationPathsTest extends HaxeCodeInsightFixtureTestCase {

  @TempDir
  Path absoluteDirectory;

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("relative path resolves against the module folder")
  public void testRelativePathResolvesAgainstTheModuleFolder() {
    NekoRunConfiguration configuration = newConfiguration();
    configuration.setModule(myFixture.getModule());
    Path moduleDir = Path.of(Objects.requireNonNull(ProjectUtil.guessModuleDir(myFixture.getModule())).getPath());

    configuration.setWorkingDirectory("out");

    assertEquals(moduleDir.resolve("out"), configuration.resolveWorkingDirectory());
  }

  @Test
  @DisplayName("absolute path stays as typed")
  public void testAbsolutePathStaysAsTyped() {
    NekoRunConfiguration configuration = newConfiguration();
    configuration.setModule(myFixture.getModule());

    configuration.setWorkingDirectory(absoluteDirectory.toString());

    assertEquals(absoluteDirectory, configuration.resolveWorkingDirectory());
  }

  @Test
  @DisplayName("relative path resolves against the project folder without a module")
  public void testRelativePathResolvesAgainstTheProjectFolderWithoutAModule() {
    NekoRunConfiguration configuration = newConfiguration();
    Path projectBase = Path.of(Objects.requireNonNull(getProject().getBasePath()));

    configuration.setWorkingDirectory("bin");

    assertEquals(projectBase.resolve("bin"), configuration.resolveWorkingDirectory());
  }

  private NekoRunConfiguration newConfiguration() {
    HaxeRunConfigurationType type = HaxeRunConfigurationType.getInstance();
    return new NekoRunConfiguration("neko", getProject(), new NekoConfigurationFactory(type));
  }
}
