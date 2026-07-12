package com.intellij.plugins.haxe.hashlink;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.process.ColoredProcessHandler;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.GenericProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugProcessStarter;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

/**
 * Debug runner for the dedicated HashLink configuration (experimental):
 * spawns the debuggee itself ({@code hl --debug <port> --debug-wait <program>}),
 * then the bundled DAP adapter, which ATTACHES to the debuggee by pid via
 * {@link HashLinkDebugProcess}. Keys on {@link HashLinkRunConfiguration} only,
 * so the legacy Flash/hxcpp debugger is never involved.
 *
 * The debuggee must be spawned from Java, not from the adapter: the adapter is
 * itself a HashLink process, and HL's process.c spawns children with
 * STARTF_USESHOWWINDOW + SW_HIDE — Windows then overrides the child's first
 * ShowWindow call, leaving a GUI debuggee's window permanently invisible.
 * Java's spawn (same as plain Run) has no such flag, so windows show normally,
 * and the debuggee's stdio/lifetime belong to the IDE's process handler.
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

    // Spawn the debuggee suspended under the VM's debug server: --debug-wait
    // blocks it until the adapter connects, so no user code runs before
    // breakpoints are installed.
    Path workDir = workingDirectory != null ? workingDirectory : hlProgram.getParent();
    int debugPort = findFreePort();
    GeneralCommandLine commandLine = new GeneralCommandLine()
      .withExePath(hlExecutable.toString())
      .withParameters("--debug", Integer.toString(debugPort), "--debug-wait", hlProgram.toString())
      .withWorkDirectory(workDir != null ? workDir.toString() : null);
    ColoredProcessHandler debuggeeHandler =
      new ColoredProcessHandler(commandLine.createProcess(), commandLine.getCommandLineString());
    ProcessTerminatedListener.attach(debuggeeHandler, environment.getProject());
    long debuggeePid = debuggeeHandler.getProcess().pid();

    try {
      XDebugSession debugSession = XDebuggerManager.getInstance(environment.getProject()).startSession(
        environment,
        new XDebugProcessStarter() {
          @NotNull
          @Override
          public XDebugProcess start(@NotNull XDebugSession session) {
            // lightweight: the adapter is spawned asynchronously in sessionInitialized()
            return new HashLinkDebugProcess(session, module, hlExecutable, hlProgram,
                                            debuggeeHandler, debugPort, debuggeePid);
          }
        });
      return debugSession.getRunContentDescriptor();
    } catch (ExecutionException | RuntimeException e) {
      debuggeeHandler.destroyProcess();
      throw e;
    }
  }

  private static int findFreePort() throws ExecutionException {
    // unavoidable race between closing here and the VM binding it; in practice
    // hl binds immediately at startup and a collision just fails the session
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (IOException e) {
      throw new ExecutionException("Cannot allocate a debug port: " + e.getMessage(), e);
    }
  }
}
