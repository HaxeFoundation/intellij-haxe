package com.intellij.plugins.haxe.runner.debugger.hxcpp.legacy;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.GenericProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.XSessionStartedResult;
import java.io.IOException;
import org.jetbrains.annotations.NotNull;

/**
 * Debug runner for the legacy-HXCPP configuration: opens the listening socket
 * FIRST, then (unless remote) launches the debuggee with the
 * {@code -start_debugger} flags its {@code getState} appended, and accepts the
 * connection. In remote mode nothing is launched — the user starts the
 * debuggee themselves and a notification shows the port being listened on.
 */
public class LegacyHxcppDebugRunner extends GenericProgramRunner<RunnerSettings> {
  public static final String RUNNER_ID = "HaxeLegacyHxcppDebugRunner";

  @NotNull
  @Override
  public String getRunnerId() {
    return RUNNER_ID;
  }

  @Override
  public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
    return DefaultDebugExecutor.EXECUTOR_ID.equals(executorId) && profile instanceof LegacyHxcppRunConfiguration;
  }

  @Override
  protected RunContentDescriptor doExecute(@NotNull RunProfileState state, @NotNull ExecutionEnvironment environment)
    throws ExecutionException {
    LegacyHxcppRunConfiguration configuration = (LegacyHxcppRunConfiguration)environment.getRunProfile();
    Module module = configuration.requireModule();

    XDebugProcessStarter starter = new XDebugProcessStarter() {
      @NotNull
      @Override
      public XDebugProcess start(@NotNull XDebugSession session) throws ExecutionException {
        try {
          LegacyHxcppDebugProcess debugProcess = new LegacyHxcppDebugProcess(session, module, configuration.getPort());
          if (configuration.isRemoteDebugging()) {
            debugProcess.info(HaxeDebuggerBundle.message("legacy.hxcpp.runner.listening", configuration.getPort()));
          }
          else {
            debugProcess.setExecutionResult(state.execute(environment.getExecutor(), LegacyHxcppDebugRunner.this));
          }
          debugProcess.start();
          return debugProcess;
        }
        catch (IOException e) {
          throw new ExecutionException(e.getMessage(), e);
        }
      }
    };
    // the session builder hands the descriptor back split-debugger-safely
    // (XDebugSession's own getRunContentDescriptor is deprecated)
    XSessionStartedResult started = XDebuggerManager.getInstance(environment.getProject())
      .newSessionBuilder(starter)
      .environment(environment)
      .startSession();
    return started.getRunContentDescriptor();
  }
}
