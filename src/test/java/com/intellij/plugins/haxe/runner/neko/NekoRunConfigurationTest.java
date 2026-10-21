package com.intellij.plugins.haxe.runner.neko;

import com.intellij.openapi.project.ProjectUtil;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.runner.HaxeRunConfigurationType;
import java.nio.file.Path;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Debugger: neko run configuration")
public class NekoRunConfigurationTest extends HaxeCodeInsightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("blank working directory defaults to the executable folder")
  public void testBlankWorkingDirectoryDefaultsToTheExecutableFolder() {
    NekoRunConfiguration configuration = newConfiguration();
    configuration.setModule(myFixture.getModule());
    Path moduleDir = Path.of(Objects.requireNonNull(ProjectUtil.guessModuleDir(myFixture.getModule())).getPath());
    configuration.setExecutablePath("bin/app.n");

    configuration.setWorkingDirectory("");

    assertEquals(moduleDir.resolve("bin"), configuration.resolveWorkingDirectory());
  }

  @Test
  @DisplayName("blank working directory with the runtime defaults to the script folder")
  public void testBlankWorkingDirectoryWithTheRuntimeDefaultsToTheScriptFolder() {
    NekoRunConfiguration configuration = newConfiguration();
    configuration.setModule(myFixture.getModule());
    Path moduleDir = Path.of(Objects.requireNonNull(ProjectUtil.guessModuleDir(myFixture.getModule())).getPath());
    configuration.setExecutablePath("");
    configuration.setProgramArguments("out/app.n --flag");

    configuration.setWorkingDirectory("");

    assertEquals(moduleDir.resolve("out"), configuration.resolveWorkingDirectory());
  }

  private NekoRunConfiguration newConfiguration() {
    HaxeRunConfigurationType type = HaxeRunConfigurationType.getInstance();
    return new NekoRunConfiguration("neko", getProject(), new NekoConfigurationFactory(type));
  }
}
