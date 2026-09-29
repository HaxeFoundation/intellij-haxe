package com.intellij.plugins.haxe.runner.debugger.hashlink;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.module.Module;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapDebugRunnerBase;
import com.intellij.plugins.haxe.v2.runconfig.HaxeActionBeforeRunTaskProvider;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

/**
 * Debug runner for the dedicated HashLink configuration (experimental).
 * Keys on {@link HashLinkRunConfiguration} only, so the legacy Flash/hxcpp
 * debugger is never involved.
 *
 * The runner spawns the debuggee itself
 * ({@code hl --debug <port> --debug-wait <program>}), suspended under the
 * VM's debug server until the adapter connects — so no user code runs before
 * breakpoints are installed. The debuggee must be spawned from Java, not from
 * the adapter: the adapter is itself a HashLink process, and HL's process.c
 * spawns children with STARTF_USESHOWWINDOW + SW_HIDE — Windows then
 * overrides the child's first ShowWindow call, leaving a GUI debuggee's
 * window permanently invisible. The {@link HashLinkBackend} launches the
 * external adapter later (at connect time) and attaches it by pid.
 */
public class HashLinkDebugRunner extends DapDebugRunnerBase<HashLinkRunConfiguration, HashLinkBackend> {
  public static final String RUNNER_ID = "HashLinkDebugRunner";

  @NotNull
  @Override
  public String getRunnerId() {
    return RUNNER_ID;
  }

  @Override
  protected Class<HashLinkRunConfiguration> configurationClass() {
    return HashLinkRunConfiguration.class;
  }

  @Override
  protected void validate(HashLinkRunConfiguration configuration) throws ExecutionException {
    Module module = configuration.requireModule();
    configuration.resolveHlExecutable(module);
    configuration.resolveProgram(module);
  }

  @Override
  protected HashLinkBackend createBackend(HashLinkRunConfiguration configuration) throws ExecutionException {
    Module module = configuration.requireModule();
    return new HashLinkBackend(configuration.resolveHlExecutable(module),
                               configuration.resolveProgram(module),
                               findFreePort(),
                               HaxeActionBeforeRunTaskProvider.buildStepSourceDirectories(configuration));
  }

  // TODO: profiling during debug - add --profile here (co-exists with --debug)
  //       and a "Get profiler snapshot" session action injecting
  //       hl.Profile.event(Dump) through the adapter's call-injection machinery.
  @Override
  protected GeneralCommandLine createCommandLine(HashLinkRunConfiguration configuration, HashLinkBackend backend)
    throws ExecutionException {
    Module module = configuration.requireModule();
    Path hlExecutable = configuration.resolveHlExecutable(module);
    Path hlProgram = configuration.resolveProgram(module);
    Path workingDirectory = configuration.resolveWorkingDirectory(module);
    Path workDir = workingDirectory != null ? workingDirectory : hlProgram.getParent();

    return new GeneralCommandLine()
      .withExePath(hlExecutable.toString())
      .withParameters("--debug", Integer.toString(backend.getDebugPort()), "--debug-wait", hlProgram.toString())
      .withWorkDirectory(workDir != null ? workDir.toString() : null);
  }

  public static int findFreePort() throws ExecutionException {
    // unavoidable race between closing here and the VM binding it; in practice
    // hl binds immediately at startup and a collision just fails the session
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (IOException e) {
      throw new ExecutionException("Cannot allocate a debug port: " + e.getMessage(), e);
    }
  }
}
