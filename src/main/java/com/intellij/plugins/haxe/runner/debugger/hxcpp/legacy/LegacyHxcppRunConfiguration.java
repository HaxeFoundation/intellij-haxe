package com.intellij.plugins.haxe.runner.debugger.hxcpp.legacy;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.configurations.RuntimeConfigurationWarning;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapCommandLineRunningState;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapRunConfigurationBase;
import com.intellij.util.execution.ParametersListUtil;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// The legacy HXCPP debugger configuration: launches a hxcpp-compiled
/// executable whose program was built with the old `debugger` haxelib
/// (the `hxcpp.DebugSocket` wire protocol). On Debug the IDE listens on
/// the configured port and appends
/// `-start_debugger -debugger_host=localhost:<port>` to the program
/// arguments so the debuggee connects back; a lime/openfl launch must end its
/// parameters with `-args` so those flags reach the program. Remote mode
/// only listens — the user starts the debuggee themselves.
public class LegacyHxcppRunConfiguration extends DapRunConfigurationBase {
  public static final int DEFAULT_PORT = 6972;

  private static final String EXECUTABLE = "executable";
  private static final String PROGRAM_PARAMETERS = "programParameters";
  private static final String WORKING_DIRECTORY = "workingDirectory";
  private static final String PORT = "debugPort";
  private static final String REMOTE = "remoteDebugging";

  private String executablePath = "";
  private String programParameters = "";
  private String workingDirectory = "";
  private int port = DEFAULT_PORT;
  private boolean remoteDebugging = false;

  public LegacyHxcppRunConfiguration(String name, Project project, ConfigurationFactory factory) {
    super(name, project, factory);
  }

  // --- settings ---

  public String getExecutablePath() {
    return executablePath;
  }

  public void setExecutablePath(@Nullable String path) {
    executablePath = orEmpty(path);
  }

  public String getProgramParameters() {
    return programParameters;
  }

  public void setProgramParameters(@Nullable String parameters) {
    programParameters = orEmpty(parameters);
  }

  public String getWorkingDirectory() {
    return workingDirectory;
  }

  public void setWorkingDirectory(@Nullable String directory) {
    workingDirectory = orEmpty(directory);
  }

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

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {
    if (getConfigurationModule().getModule() == null) {
      throw new RuntimeConfigurationError(HaxeDebuggerBundle.message("legacy.hxcpp.runner.no.module"));
    }
    if (port < 1 || port > 65535) {
      throw new RuntimeConfigurationError(HaxeDebuggerBundle.message("legacy.hxcpp.runner.bad.port"));
    }
    if (!remoteDebugging && executablePath.isBlank()) {
      throw new RuntimeConfigurationError(HaxeDebuggerBundle.message("legacy.hxcpp.runner.no.executable"));
    }
    Path executable = resolveExecutableOrNull();
    if (executable != null && !Files.isRegularFile(executable)) {
      throw new RuntimeConfigurationWarning(
        HaxeDebuggerBundle.message("legacy.hxcpp.runner.executable.missing", executablePath));
    }
  }

  // Resolution stays inside the supplier - getState runs before before-launch
  // tasks, so the executable a build step produces may not exist yet. In
  // remote debug the returned state is never executed (the debug runner only
  // listens).
  @Override
  public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) throws ExecutionException {
    requireModule();
    boolean debug = DefaultDebugExecutor.EXECUTOR_ID.equals(executor.getId());
    return new DapCommandLineRunningState(env, getProject(), () -> createCommandLine(debug));
  }

  private GeneralCommandLine createCommandLine(boolean debug) throws ExecutionException {
    Path executable = resolveExecutableOrNull();
    if (executable == null || !Files.isRegularFile(executable)) {
      throw new ExecutionException(HaxeDebuggerBundle.message(
        "legacy.hxcpp.runner.executable.missing", executablePath.isBlank() ? "<not set>" : executablePath));
    }
    GeneralCommandLine commandLine = new GeneralCommandLine().withExePath(executable.toString());
    if (!programParameters.isBlank()) {
      commandLine.withParameters(ParametersListUtil.parse(programParameters));
    }
    if (debug) {
      commandLine.withParameters("-start_debugger", "-debugger_host=localhost:" + port);
    }
    Path workDir = !workingDirectory.isBlank() ? resolveAgainstProject(workingDirectory) : executable.getParent();
    if (workDir != null) {
      commandLine.withWorkDirectory(workDir.toString());
    }
    return commandLine;
  }

  // --- resolution ---

  @Nullable
  private Path resolveExecutableOrNull() {
    return executablePath.isBlank() ? null : resolveAgainstProject(executablePath);
  }

  // --- persistence ---

  @Override
  public void readExternal(@NotNull Element element) throws InvalidDataException {
    super.readExternal(element);
    readModule(element);
    executablePath = orEmpty(JDOMExternalizerUtil.readField(element, EXECUTABLE));
    programParameters = orEmpty(JDOMExternalizerUtil.readField(element, PROGRAM_PARAMETERS));
    workingDirectory = orEmpty(JDOMExternalizerUtil.readField(element, WORKING_DIRECTORY));
    try {
      port = Integer.parseInt(JDOMExternalizerUtil.readField(element, PORT, String.valueOf(DEFAULT_PORT)));
    } catch (NumberFormatException e) {
      port = DEFAULT_PORT;
    }
    remoteDebugging = Boolean.parseBoolean(JDOMExternalizerUtil.readField(element, REMOTE, "false"));
  }

  @Override
  public void writeExternal(@NotNull Element element) throws WriteExternalException {
    super.writeExternal(element); // also serializes the module
    JDOMExternalizerUtil.writeField(element, EXECUTABLE, executablePath);
    JDOMExternalizerUtil.writeField(element, PROGRAM_PARAMETERS, programParameters);
    JDOMExternalizerUtil.writeField(element, WORKING_DIRECTORY, workingDirectory);
    JDOMExternalizerUtil.writeField(element, PORT, String.valueOf(port));
    JDOMExternalizerUtil.writeField(element, REMOTE, String.valueOf(remoteDebugging));
  }
}
