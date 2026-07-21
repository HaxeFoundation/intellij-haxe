package com.intellij.plugins.haxe.runner.debugger.dap.ide;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.process.ColoredProcessHandler;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.GenericProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.XSessionStartedResult;
import java.io.IOException;
import org.jetbrains.annotations.NotNull;

/**
 * The debug-session start dance shared by every DAP debugger, as a template:
 * validate the configuration (fail fast, before any UI is built), create the
 * {@link DapBackend} FIRST — it must be listening when the debuggee starts
 * connecting — then spawn the debuggee and hand both to a
 * {@link DapDebugProcess} through the session builder. Cleanup on any
 * failure: the spawned process is destroyed and the backend closed.
 *
 * A concrete runner supplies its configuration class, the backend and the
 * command line; everything else lives here.
 */
public abstract class DapDebugRunnerBase<C extends RunConfiguration, B extends DapBackend>
  extends GenericProgramRunner<RunnerSettings> {

  /** The configuration this runner keys on ({@code canRun} matches nothing else). */
  protected abstract Class<C> configurationClass();

  /** Fail-fast validation before anything is built (e.g. {@code requireModule()}). */
  protected abstract void validate(C configuration) throws ExecutionException;

  /** Builds the backend; it starts listening in its constructor. */
  protected abstract B createBackend(C configuration) throws ExecutionException;

  /** The debuggee invocation, including anything the backend contributes (env vars, defines). */
  protected abstract GeneralCommandLine createCommandLine(C configuration, B backend) throws ExecutionException;

  @Override
  public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
    return DefaultDebugExecutor.EXECUTOR_ID.equals(executorId) && configurationClass().isInstance(profile);
  }

  @Override
  protected RunContentDescriptor doExecute(@NotNull RunProfileState state, @NotNull ExecutionEnvironment environment)
    throws ExecutionException {
    C configuration = configurationClass().cast(environment.getRunProfile());
    validate(configuration);

    B backend = createBackend(configuration);

    ColoredProcessHandler debuggeeHandler;
    try {
      GeneralCommandLine commandLine = createCommandLine(configuration, backend);
      debuggeeHandler = new ColoredProcessHandler(commandLine.createProcess(), commandLine.getCommandLineString());
    } catch (ExecutionException | RuntimeException e) {
      closeQuietly(backend);
      throw e;
    }
    ProcessTerminatedListener.attach(debuggeeHandler, environment.getProject());
    backend.debuggeeSpawned(debuggeeHandler);

    try {
      // the session builder is the split-debugger-safe way to hand the
      // descriptor back to the execution manager (XDebugSession's own
      // getRunContentDescriptor is deprecated and logs an error)
      XSessionStartedResult started = XDebuggerManager.getInstance(environment.getProject())
        .newSessionBuilder(new XDebugProcessStarter() {
          @NotNull
          @Override
          public XDebugProcess start(@NotNull XDebugSession session) {
            // lightweight: the DAP conversation starts asynchronously in sessionInitialized()
            return new DapDebugProcess(session, backend, debuggeeHandler);
          }
        })
        .environment(environment)
        .startSession();
      return started.getRunContentDescriptor();
    } catch (ExecutionException | RuntimeException e) {
      debuggeeHandler.destroyProcess();
      closeQuietly(backend);
      throw e;
    }
  }

  private static void closeQuietly(DapBackend backend) {
    try {
      backend.close();
    } catch (IOException ignored) {
      // teardown on a failed start; the original failure matters more
    }
  }
}
