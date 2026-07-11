package com.intellij.plugins.haxe.hashlink;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.GenericProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.runner.HaxeApplicationConfiguration;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

/**
 * Debug runner for HashLink targets (experimental): spawns the bundled DAP
 * adapter and drives it through {@link HashLinkDebugProcess}. Registered ahead
 * of the generic HaxeDebugRunner and claims ONLY HashLink configurations, so
 * the existing Flash/hxcpp debugger paths are untouched.
 */
public class HashLinkDebugRunner extends GenericProgramRunner<RunnerSettings> {
  public static final String RUNNER_ID = "HashLinkDebugRunner";

  @NotNull
  @Override
  public String getRunnerId() {
    return RUNNER_ID;
  }

  @Override
  public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
    return DefaultDebugExecutor.EXECUTOR_ID.equals(executorId)
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

    // fail fast, before any UI is built
    Path hlExecutable = HashLinkRunConfigurations.resolveHlExecutable(module);
    Path hlProgram = HashLinkRunConfigurations.resolveHlOutput(configuration, module);

    XDebugSession debugSession = XDebuggerManager.getInstance(environment.getProject()).startSession(
      environment,
      new XDebugProcessStarter() {
        @NotNull
        @Override
        public XDebugProcess start(@NotNull XDebugSession session) {
          // lightweight: the adapter is spawned asynchronously in sessionInitialized()
          return new HashLinkDebugProcess(session, module, hlExecutable, hlProgram);
        }
      });
    return debugSession.getRunContentDescriptor();
  }
}
