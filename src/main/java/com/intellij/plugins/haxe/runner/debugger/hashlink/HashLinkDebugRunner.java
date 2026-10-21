package com.intellij.plugins.haxe.runner.debugger.hashlink;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.module.Module;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapDebugRunnerBase;
import com.intellij.plugins.haxe.v2.runconfig.HaxeActionBeforeRunTaskProvider;
import org.jetbrains.annotations.NotNull;

/**
 * Debug runner for the dedicated HashLink configuration (experimental).
 * Keys on {@link HashLinkRunConfiguration} only, so no other debug runner is
 * involved.
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
    return backend.debuggeeCommandLine(configuration.effectiveWorkingDirectory(module).toString());
  }
}
