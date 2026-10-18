package com.intellij.plugins.haxe.runner.debugger.hxcpp.legacy;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapCommandLineRunningState;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapExecutableRunConfigurationBase;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

/// The legacy HXCPP debugger configuration: launches a hxcpp-compiled
/// executable whose program was built with the old `debugger` haxelib
/// (the `hxcpp.DebugSocket` wire protocol). On Debug the IDE listens on
/// the configured port and appends
/// `-start_debugger -debugger_host=localhost:<port>` to the program
/// arguments so the debuggee connects back; a lime/openfl launch must end its
/// parameters with `-args` so those flags reach the program. Remote mode
/// only listens — the user starts the debuggee themselves.
public class LegacyHxcppRunConfiguration extends DapExecutableRunConfigurationBase {
  public static final int DEFAULT_PORT = 6972;

  private static final String PORT = "debugPort";
  private static final String REMOTE = "remoteDebugging";

  private int port = DEFAULT_PORT;
  private boolean remoteDebugging = false;

  public LegacyHxcppRunConfiguration(String name, Project project, ConfigurationFactory factory) {
    super(name, project, factory);
  }

  // --- settings ---

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public boolean isRemoteDebugging() {
    return remoteDebugging;
  }

  public void setRemoteDebugging(boolean remote) {
    remoteDebugging = remote;
  }

  @Override
  public @NotNull SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new LegacyHxcppRunConfigurationEditor(getProject());
  }

  /** The port, then the base's executable checks; remote mode only listens, so there a blank executable passes as long as a module is set. */
  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {
    if (port < 1 || port > 65535) {
      throw new RuntimeConfigurationError(HaxeDebuggerBundle.message("legacy.hxcpp.runner.bad.port"));
    }
    if (remoteDebugging && getExecutablePath().isBlank()) {
      if (getConfigurationModule().getModule() == null) {
        throw new RuntimeConfigurationError(HaxeDebuggerBundle.message("hxcpp.runner.no.module"));
      }
      return;
    }
    super.checkConfiguration();
  }

  // Resolution stays inside the supplier - getState runs before before-launch
  // tasks, so the executable a build step produces may not exist yet. In
  // remote debug the returned state is never executed (the debug runner only
  // listens).
  @Override
  public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) throws ExecutionException {
    requireModule();
    boolean debug = DefaultDebugExecutor.EXECUTOR_ID.equals(executor.getId());
    return new DapCommandLineRunningState(env, () -> createCommandLine(debug));
  }

  /** The executable invocation plus, on Debug, the flags that make the debuggee dial the IDE's listener. */
  private GeneralCommandLine createCommandLine(boolean debug) throws ExecutionException {
    GeneralCommandLine commandLine = createCommandLine();
    if (debug) {
      commandLine.withParameters("-start_debugger", "-debugger_host=localhost:" + port);
    }
    return commandLine;
  }

  // --- persistence ---

  @Override
  public void readExternal(@NotNull Element element) throws InvalidDataException {
    super.readExternal(element);
    try {
      port = Integer.parseInt(JDOMExternalizerUtil.readField(element, PORT, String.valueOf(DEFAULT_PORT)));
    } catch (NumberFormatException e) {
      port = DEFAULT_PORT;
    }
    remoteDebugging = Boolean.parseBoolean(JDOMExternalizerUtil.readField(element, REMOTE, "false"));
  }

  @Override
  public void writeExternal(@NotNull Element element) throws WriteExternalException {
    super.writeExternal(element);
    JDOMExternalizerUtil.writeField(element, PORT, String.valueOf(port));
    JDOMExternalizerUtil.writeField(element, REMOTE, String.valueOf(remoteDebugging));
  }
}
