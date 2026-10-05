package com.intellij.plugins.haxe.runner.neko;

import com.intellij.openapi.project.ProjectUtil;
import com.intellij.plugins.haxe.HaxeCodeInsightFixtureTestCase;
import com.intellij.plugins.haxe.runner.HaxeRunConfigurationType;
import java.nio.file.Path;
import java.util.Objects;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Debugger: neko run configuration")
public class NekoRunConfigurationTest extends HaxeCodeInsightFixtureTestCase {

  @TempDir
  Path absoluteDirectory;

  @Override
  protected String getBasePath() {
    return "";
  }

  @Test
  @DisplayName("working directory resolves against the module like the executable")
  public void testWorkingDirectoryResolvesAgainstTheModuleLikeTheExecutable() {
    NekoRunConfiguration configuration = newConfiguration();
    configuration.setModule(myFixture.getModule());
    Path moduleDir = Path.of(Objects.requireNonNull(ProjectUtil.guessModuleDir(myFixture.getModule())).getPath());
    configuration.setExecutablePath("bin/app.n");

    configuration.setWorkingDirectory("out");
    assertEquals(moduleDir.resolve("out"), configuration.resolveWorkingDirectory());

    configuration.setWorkingDirectory(absoluteDirectory.toString());
    assertEquals(absoluteDirectory, configuration.resolveWorkingDirectory());

    configuration.setWorkingDirectory("");
    assertEquals(moduleDir.resolve("bin"), configuration.resolveWorkingDirectory());
  }

  private NekoRunConfiguration newConfiguration() {
    HaxeRunConfigurationType type = HaxeRunConfigurationType.getInstance();
    return new NekoRunConfiguration("neko", getProject(), new NekoConfigurationFactory(type));
  }
}
