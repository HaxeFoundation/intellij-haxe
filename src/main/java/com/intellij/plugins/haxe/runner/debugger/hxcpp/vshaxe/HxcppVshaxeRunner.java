package com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.GenericProgramRunner;
import com.intellij.execution.ui.ExecutionUiService;
import com.intellij.execution.ui.RunContentDescriptor;
import org.jetbrains.annotations.NotNull;

/**
 * Plain Run for the dedicated HXCPP configuration (experimental). Keys on
 * {@link HxcppVshaxeRunConfiguration} only, so the legacy Haxe runners are never
 * involved in an HXCPP launch and vice versa.
 */
public class HxcppVshaxeRunner extends GenericProgramRunner<RunnerSettings> {
  public static final String RUNNER_ID = "HxcppVshaxeRunner";

  @NotNull
  @Override
  public String getRunnerId() {
    return RUNNER_ID;
  }

  @Override
  public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
    return DefaultRunExecutor.EXECUTOR_ID.equals(executorId) && profile instanceof HxcppVshaxeRunConfiguration;
  }

  @Override
  protected RunContentDescriptor doExecute(@NotNull RunProfileState state, @NotNull ExecutionEnvironment environment)
    throws ExecutionException {
    // the state comes from HxcppVshaxeRunConfiguration.getState -> DapCommandLineRunningState
    ExecutionResult result = state.execute(environment.getExecutor(), this);
    return ExecutionUiService.getInstance().showRunContent(result, environment);
  }
}
