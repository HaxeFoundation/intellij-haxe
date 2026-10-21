package com.intellij.plugins.haxe.v2.runconfig;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeLightFixtureTestCase;
import com.intellij.plugins.haxe.runner.HaxeRunConfigurationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Run configurations: run configuration build file tracker")
public class HaxeRunConfigurationBuildFileTrackerTest extends HaxeLightFixtureTestCase {

  private RunnerAndConfigurationSettings settings;

  @Override
  protected String getBasePath() {
    return "";
  }

  /** The run manager belongs to the shared light project. */
  @Override
  protected void tearDown() throws Exception {
    try {
      if (settings != null) RunManager.getInstance(getProject()).removeConfiguration(settings);
    }
    finally {
      super.tearDown();
    }
  }

  @Test
  @DisplayName("renaming a build file carries the configuration and its before-run step")
  public void testRenamingABuildFileCarriesTheConfigurationAndItsBeforeRunStep() throws IOException {
    VirtualFile buildFile = myFixture.addFileToProject("app/build.hxml", "-main Main").getVirtualFile();
    HaxeActionRunConfiguration configuration = addActionConfiguration(buildFile.getPath());
    HaxeActionBeforeRunTaskProvider.Task step = new HaxeActionBeforeRunTaskProvider.Task();
    step.setBuildFilePath(buildFile.getPath());
    configuration.setBeforeRunTasks(List.of(step));

    WriteAction.runAndWait(() -> buildFile.rename(this, "app.hxml"));

    assertEquals(buildFile.getPath(), configuration.getBuildFilePath());
    assertEquals(buildFile.getPath(), step.getBuildFilePath());
  }

  @Test
  @DisplayName("moving the folder above a build file carries the configuration")
  public void testMovingTheFolderAboveABuildFileCarriesTheConfiguration() throws IOException {
    VirtualFile buildFile = myFixture.addFileToProject("app/build.hxml", "-main Main").getVirtualFile();
    VirtualFile target = myFixture.getTempDirFixture().findOrCreateDir("projects");
    HaxeActionRunConfiguration configuration = addActionConfiguration(buildFile.getPath());

    WriteAction.runAndWait(() -> buildFile.getParent().move(this, target));

    assertEquals(buildFile.getPath(), configuration.getBuildFilePath());
  }

  private HaxeActionRunConfiguration addActionConfiguration(String buildFilePath) {
    RunManager runManager = RunManager.getInstance(getProject());
    settings = runManager.createConfiguration("build", actionFactory());
    HaxeActionRunConfiguration configuration = (HaxeActionRunConfiguration)settings.getConfiguration();
    configuration.setBuildFilePath(buildFilePath);
    runManager.addConfiguration(settings);
    return configuration;
  }

  private static ConfigurationFactory actionFactory() {
    ConfigurationType type = ConfigurationTypeUtil.findConfigurationType(HaxeRunConfigurationType.class);
    return Arrays.stream(type.getConfigurationFactories())
      .filter(factory -> factory instanceof HaxeActionConfigurationFactory)
      .findFirst()
      .orElseThrow();
  }
}
