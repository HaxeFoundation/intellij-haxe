package com.intellij.plugins.haxe.runner;

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
 * Plain Run for one run configuration class: the Run executor only, executing
 * the configuration's own state and showing it in the Run tool window. Keying
 * on a single class keeps every other runner out of that configuration's
 * launches and this one out of theirs.
 */
public abstract class HaxePlainRunner extends GenericProgramRunner<RunnerSettings> {

  private final String runnerId;
  private final Class<? extends RunProfile> profileClass;

  protected HaxePlainRunner(@NotNull String runnerId, @NotNull Class<? extends RunProfile> profileClass) {
    this.runnerId = runnerId;
    this.profileClass = profileClass;
  }

  @Override
  public @NotNull String getRunnerId() {
    return runnerId;
  }

  @Override
  public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
    return DefaultRunExecutor.EXECUTOR_ID.equals(executorId) && profileClass.isInstance(profile);
  }

  @Override
  protected RunContentDescriptor doExecute(@NotNull RunProfileState state, @NotNull ExecutionEnvironment environment)
    throws ExecutionException {
    ExecutionResult result = state.execute(environment.getExecutor(), this);
    return ExecutionUiService.getInstance().showRunContent(result, environment);
  }
}
