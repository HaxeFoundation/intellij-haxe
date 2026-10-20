package com.intellij.plugins.haxe.runner.debugger.dap.ide;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil;
import com.intellij.execution.testframework.sm.runner.SMRunnerConsolePropertiesProvider;
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * SM test console for debugged test runs. A run profile that provides SM
 * console properties (a test run configuration) gets an SM test console
 * attached to the debuggee's process handler as the session console: the
 * TeamCity service messages on the debuggee's stdout drive the test tree
 * exactly as in a plain test run, while breakpoints work through the DAP
 * session. The platform attaches the session console BEFORE calling
 * {@code startNotify()} on the session's process handler, so the SM converter
 * sees the full lifecycle (startNotified, output, processTerminated).
 */
public final class DapTestConsoles {
  private static final Logger LOG = Logger.getInstance(DapTestConsoles.class);

  private DapTestConsoles() {
  }

  /** The SM console for a test-run profile, attached to {@code processHandler}; null for any other profile. */
  public static @Nullable ExecutionConsole createTestConsole(@Nullable RunProfile profile,
                                                             @NotNull Executor executor,
                                                             @NotNull ProcessHandler processHandler) {
    if (!(profile instanceof SMRunnerConsolePropertiesProvider provider)) {
      return null;
    }
    SMTRunnerConsoleProperties properties = provider.createTestConsoleProperties(executor);
    try {
      return SMTestRunnerConnectionUtil.createAndAttachConsole(
        properties.getTestFrameworkName(), processHandler, properties);
    } catch (ExecutionException e) {
      LOG.warn("SM test console could not be created; falling back to the plain console", e);
      // on success the console owns the properties; without one, nobody does
      Disposer.dispose(properties);
      return null;
    }
  }
}
