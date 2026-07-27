package com.intellij.plugins.haxe.runner.debugger.hxcpp.intellij;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapDebugRunnerBase;
import java.io.IOException;
import org.jetbrains.annotations.NotNull;

/**
 * Debug runner for the HXCPP (IntelliJ debug server) configuration. Keys on
 * {@link HxcppIntellijRunConfiguration} only. The base class binds the
 * backend's ephemeral loopback listener BEFORE spawning the debuggee with
 * that listener's address in the HXCPP_DEBUG_HOST/PORT env vars — the
 * executable's embedded debug server connects out during startup and holds
 * the program before {@code main} until the IDE finishes configuring. An
 * ephemeral port per session means no port setting, no collisions between
 * concurrent sessions, and no leftover-instance poisoning.
 */
public class HxcppIntellijDebugRunner extends DapDebugRunnerBase<HxcppIntellijRunConfiguration, HxcppIntellijBackend> {
  public static final String RUNNER_ID = "HxcppIntellijDebugRunner";

  /** Generous: the debuggee connects during process startup, typically instantly. */
  private static final int DEBUGGEE_CONNECT_TIMEOUT_MILLIS = 30_000;

  @NotNull
  @Override
  public String getRunnerId() {
    return RUNNER_ID;
  }

  @Override
  protected Class<HxcppIntellijRunConfiguration> configurationClass() {
    return HxcppIntellijRunConfiguration.class;
  }

  @Override
  protected void validate(HxcppIntellijRunConfiguration configuration) throws ExecutionException {
    configuration.requireModule();
    configuration.resolveExecutable();
  }

  @Override
  protected HxcppIntellijBackend createBackend(HxcppIntellijRunConfiguration configuration) throws ExecutionException {
    try {
      return new HxcppIntellijBackend(DEBUGGEE_CONNECT_TIMEOUT_MILLIS);
    } catch (IOException e) {
      throw new ExecutionException(HaxeDebuggerBundle.message("hxcpp.intellij.runner.listen.failed", e.getMessage()));
    }
  }

  @Override
  protected GeneralCommandLine createCommandLine(HxcppIntellijRunConfiguration configuration, HxcppIntellijBackend backend)
    throws ExecutionException {
    return configuration.createCommandLine()
      .withEnvironment(HxcppIntellijBackend.ENV_DEBUG_HOST, backend.getHost())
      .withEnvironment(HxcppIntellijBackend.ENV_DEBUG_PORT, Integer.toString(backend.getPort()));
  }
}
