package com.intellij.plugins.haxe.profiler.bridge;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.GenericProgramRunner;
import com.intellij.execution.ui.ExecutionUiService;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.plugins.haxe.profiler.HaxeProfilableRunConfiguration;
import com.intellij.profiler.api.configurations.ProfilerConfigurationState;
import org.jetbrains.annotations.NotNull;

/**
 * Runs a Haxe profilable configuration under the IU "Run with Profiler"
 * executor when the chosen profiler configuration matches its lane. The
 * profiled behaviour itself lives with the configurations — HashLink's
 * getState injects {@code --profile}, an hxcpp run gets its bootstrap through
 * the before-launch compile — both asking {@code HaxeProfilerExecutorSupport}.
 */
public class HaxeProfilerRunner extends GenericProgramRunner<RunnerSettings> {

  public static final String RUNNER_ID = "HaxeHlProfilerRunner";

  @NotNull
  @Override
  public String getRunnerId() {
    return RUNNER_ID;
  }

  @Override
  public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
    if (!(profile instanceof HaxeProfilableRunConfiguration configuration) || !configuration.isProfilingReady()) {
      return false;
    }
    ProfilerConfigurationState state = HaxeProfilerConfigurations.stateForExecutor(executorId);
    return state != null
           && HaxeProfilerConfigurations.typeIdsFor(configuration.profilingLane()).contains(state.getConfigurationTypeId());
  }

  @Override
  protected RunContentDescriptor doExecute(@NotNull RunProfileState state, @NotNull ExecutionEnvironment environment)
    throws ExecutionException {
    // the state is already the profiling one - the configuration's getState
    // saw the profiler executor through HaxeProfilerExecutorSupport
    ExecutionResult result = state.execute(environment.getExecutor(), this);
    return ExecutionUiService.getInstance().showRunContent(result, environment);
  }
}
