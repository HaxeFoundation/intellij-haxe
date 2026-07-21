package com.intellij.plugins.haxe.runner.debugger.interp;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapDebugRunnerBase;
import java.io.IOException;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Debug runner for the Haxe interpreter configuration (experimental). Keys on
 * {@link InterpRunConfiguration} only. The base class binds the adapter's
 * listener BEFORE spawning haxe with {@code -D eval-debugger} pointing at
 * it — the eval VM connects out during compiler startup and holds execution
 * (the interpreted main, or the build's first macro) until the adapter
 * continues it on configurationDone, so breakpoints are always installed
 * before any user code runs.
 */
public class InterpDebugRunner extends DapDebugRunnerBase<InterpRunConfiguration, InterpDapBackend> {
  public static final String RUNNER_ID = "HaxeInterpDebugRunner";

  /** Generous: the eval VM connects during compiler startup, typically instantly. */
  private static final long VM_CONNECT_TIMEOUT_MILLIS = 30_000;

  @NotNull
  @Override
  public String getRunnerId() {
    return RUNNER_ID;
  }

  @Override
  protected Class<InterpRunConfiguration> configurationClass() {
    return InterpRunConfiguration.class;
  }

  @Override
  protected void validate(InterpRunConfiguration configuration) throws ExecutionException {
    configuration.requireModule();
  }

  @Override
  protected InterpDapBackend createBackend(InterpRunConfiguration configuration) throws ExecutionException {
    try {
      return new InterpDapBackend(VM_CONNECT_TIMEOUT_MILLIS);
    } catch (IOException e) {
      throw new ExecutionException(HaxeDebuggerBundle.message("interp.runner.listener.failed", e.getMessage()));
    }
  }

  @Override
  protected GeneralCommandLine createCommandLine(InterpRunConfiguration configuration, InterpDapBackend backend)
    throws ExecutionException {
    return configuration.createCommandLine(List.of("-D", "eval-debugger=127.0.0.1:" + backend.getVmPort()));
  }
}
