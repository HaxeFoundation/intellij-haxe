package com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapDebugRunnerBase;
import java.io.IOException;
import org.jetbrains.annotations.NotNull;

/**
 * Debug runner for the HXCPP (vshaxe debug server) configuration. Keys on
 * {@link HxcppVshaxeRunConfiguration} only, so the legacy Flash/hxcpp debugger is
 * never involved. The base class binds the adapter's listener BEFORE spawning
 * the debuggee — the executable's embedded debug server connects out during
 * startup and holds the program before {@code main} until configuration is
 * done, so breakpoints are always installed before user code runs.
 */
public class HxcppVshaxeDebugRunner extends DapDebugRunnerBase<HxcppVshaxeRunConfiguration, HxcppVshaxeBackend> {
  public static final String RUNNER_ID = "HxcppVshaxeDebugRunner";

  /** Generous: the debuggee connects during process startup, typically instantly. */
  private static final long DEBUGGEE_CONNECT_TIMEOUT_MILLIS = 30_000;

  @NotNull
  @Override
  public String getRunnerId() {
    return RUNNER_ID;
  }

  @Override
  protected Class<HxcppVshaxeRunConfiguration> configurationClass() {
    return HxcppVshaxeRunConfiguration.class;
  }

  @Override
  protected void validate(HxcppVshaxeRunConfiguration configuration) throws ExecutionException {
    configuration.requireModule();
    configuration.resolveExecutable();
    configuration.resolveDebugPort();
  }

  @Override
  protected HxcppVshaxeBackend createBackend(HxcppVshaxeRunConfiguration configuration) throws ExecutionException {
    String debugHost = configuration.getDebugHost();
    int debugPort = configuration.resolveDebugPort();
    try {
      // one debug session per port: the port is baked into the executable at
      // compile time, so a second concurrent session cannot get its own
      return new HxcppVshaxeBackend(debugHost, debugPort, DEBUGGEE_CONNECT_TIMEOUT_MILLIS);
    } catch (IOException e) {
      throw new ExecutionException(
        HaxeDebuggerBundle.message("hxcpp.runner.port.busy", debugHost, debugPort, e.getMessage()));
    }
  }

  @Override
  protected GeneralCommandLine createCommandLine(HxcppVshaxeRunConfiguration configuration, HxcppVshaxeBackend backend)
    throws ExecutionException {
    return configuration.createCommandLine();
  }
}
