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
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

/**
 * Debug runner for the dedicated HashLink configuration (experimental):
 * spawns the bundled DAP adapter and drives it through
 * {@link HashLinkDebugProcess}. Keys on {@link HashLinkRunConfiguration} only,
 * so the legacy Flash/hxcpp debugger is never involved.
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
    return DefaultDebugExecutor.EXECUTOR_ID.equals(executorId) && profile instanceof HashLinkRunConfiguration;
  }

  @Override
  protected RunContentDescriptor doExecute(@NotNull RunProfileState state, @NotNull ExecutionEnvironment environment)
    throws ExecutionException {
    HashLinkRunConfiguration configuration = (HashLinkRunConfiguration)environment.getRunProfile();
    Module module = configuration.requireModule();

    // fail fast, before any UI is built
    Path hlExecutable = HashLinkRunConfigurations.resolveHlExecutable(module);
    Path hlProgram = configuration.resolveProgram(module);
    Path workingDirectory = configuration.resolveWorkingDirectory(module);

    XDebugSession debugSession = XDebuggerManager.getInstance(environment.getProject()).startSession(
      environment,
      new XDebugProcessStarter() {
        @NotNull
        @Override
        public XDebugProcess start(@NotNull XDebugSession session) {
          // lightweight: the adapter is spawned asynchronously in sessionInitialized()
          return new HashLinkDebugProcess(session, module, hlExecutable, hlProgram, workingDirectory);
        }
      });
    return debugSession.getRunContentDescriptor();
  }
}
