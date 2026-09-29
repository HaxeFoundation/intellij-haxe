package com.intellij.plugins.haxe.v2.runconfig;

import com.intellij.execution.Executor;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/** Launches a stored run configuration the way the main toolbar does. */
public final class HaxeConfigurationLaunches {

  private HaxeConfigurationLaunches() {
  }

  /** Selects the configuration in the dropdown and runs it under the executor. */
  public static void runSelected(@NotNull Project project,
                                 @NotNull RunnerAndConfigurationSettings settings,
                                 @NotNull Executor executor) {
    RunManager.getInstance(project).setSelectedConfiguration(settings);
    // ExecutionUtil (not ProgramRunnerUtil) routes through restartRunProfile,
    // which enforces single-instance configurations with the stop-and-rerun dialog
    ExecutionUtil.runConfiguration(settings, executor);
  }
}
