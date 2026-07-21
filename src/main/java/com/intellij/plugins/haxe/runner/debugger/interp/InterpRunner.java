package com.intellij.plugins.haxe.runner.debugger.interp;

import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.GenericProgramRunner;
import org.jetbrains.annotations.NotNull;

/**
 * Plain Run for the Haxe interpreter configuration; keys on
 * {@link InterpRunConfiguration} only, so the legacy runners never see it.
 */
public class InterpRunner extends GenericProgramRunner<RunnerSettings> {
  public static final String RUNNER_ID = "HaxeInterpRunner";

  @NotNull
  @Override
  public String getRunnerId() {
    return RUNNER_ID;
  }

  @Override
  public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
    return DefaultRunExecutor.EXECUTOR_ID.equals(executorId) && profile instanceof InterpRunConfiguration;
  }
}
