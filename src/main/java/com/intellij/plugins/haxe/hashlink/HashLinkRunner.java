package com.intellij.plugins.haxe.hashlink;

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
import com.intellij.openapi.module.Module;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.runner.HaxeApplicationConfiguration;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

/**
 * Plain Run for HashLink targets (experimental): launches the compiled
 * {@code .hl} on the HashLink VM. Registered ahead of the generic HaxeRunner
 * and claims ONLY HashLink configurations (see HashLinkRunConfigurations), so
 * existing run behaviour for every other target is untouched.
 */
public class HashLinkRunner extends GenericProgramRunner<RunnerSettings> {
  public static final String RUNNER_ID = "HashLinkRunner";

  @NotNull
  @Override
  public String getRunnerId() {
    return RUNNER_ID;
  }

  @Override
  public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
    return DefaultRunExecutor.EXECUTOR_ID.equals(executorId)
           && HashLinkRunConfigurations.isHashLinkConfiguration(profile);
  }

  @Override
  protected RunContentDescriptor doExecute(@NotNull RunProfileState state, @NotNull ExecutionEnvironment environment)
    throws ExecutionException {
    HaxeApplicationConfiguration configuration = (HaxeApplicationConfiguration)environment.getRunProfile();
    Module module = configuration.getConfigurationModule().getModule();
    if (module == null) {
      throw new ExecutionException(HaxeBundle.message("no.module.for.run.configuration", configuration.getName()));
    }

    Path hlExecutable = HashLinkRunConfigurations.resolveHlExecutable(module);
    Path hlProgram = HashLinkRunConfigurations.resolveHlOutput(configuration, module);

    HashLinkRunningState runningState = new HashLinkRunningState(environment, module, hlExecutable, hlProgram);
    ExecutionResult result = runningState.execute(environment.getExecutor(), this);
    return ExecutionUiService.getInstance().showRunContent(result, environment);
  }
}
