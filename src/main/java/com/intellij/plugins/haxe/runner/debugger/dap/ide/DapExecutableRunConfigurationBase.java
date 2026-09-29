package com.intellij.plugins.haxe.runner.debugger.dap.ide;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.configurations.RuntimeConfigurationWarning;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.util.execution.ParametersListUtil;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import lombok.Getter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base for DAP-debugger configurations that run a compiled native executable:
 * the executable path (absolute or project-relative), an optional working
 * directory (default: the executable's directory, so relative resource
 * loading behaves like a manual launch) and program arguments — with their
 * resolution, validation, persistence and the plain-Run state. Subclasses add
 * connection-specific fields (or nothing at all).
 *
 * The validation message keys are the shared hxcpp.runner.* entries; a future
 * configuration wanting different wording overrides {@link #checkConfiguration}.
 */
public abstract class DapExecutableRunConfigurationBase extends DapRunConfigurationBase {
  private static final String EXECUTABLE = "executable";
  private static final String WORKING_DIRECTORY = "workingDirectory";
  private static final String PROGRAM_ARGUMENTS = "programArguments";

  @Getter private String executablePath = "";
  @Getter private String workingDirectory = "";
  @Getter private String programArguments = "";

  protected DapExecutableRunConfigurationBase(String name, Project project, ConfigurationFactory factory) {
    super(name, project, factory);
  }

  // --- settings (setters normalize their input, so they stay hand-written) ---

  public void setExecutablePath(@Nullable String path) {
    executablePath = path == null ? "" : path;
  }

  public void setWorkingDirectory(@Nullable String directory) {
    workingDirectory = directory == null ? "" : directory;
  }

  public void setProgramArguments(@Nullable String arguments) {
    programArguments = arguments == null ? "" : arguments;
  }

  // --- validation ---

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {
    if (getConfigurationModule().getModule() == null) {
      throw new RuntimeConfigurationError(HaxeDebuggerBundle.message("hxcpp.runner.no.module"));
    }
    if (executablePath.isBlank()) {
      throw new RuntimeConfigurationError(HaxeDebuggerBundle.message("hxcpp.runner.no.executable"));
    }
    Path executable = resolveExecutableOrNull();
    if (executable == null || !Files.isRegularFile(executable)) {
      throw new RuntimeConfigurationWarning(
        HaxeDebuggerBundle.message("hxcpp.runner.executable.missing", executablePath));
    }
  }

  // Resolution stays inside the supplier: getState runs before before-launch
  // tasks, so an executable a build step produces may not exist yet.
  @Override
  public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) throws ExecutionException {
    requireModule();
    return new DapCommandLineRunningState(env, getProject(), this::createCommandLine);
  }

  // --- resolution ---

  public Path resolveExecutable() throws ExecutionException {
    Path executable = resolveExecutableOrNull();
    if (executable == null || !Files.isRegularFile(executable)) {
      throw new ExecutionException(
        HaxeDebuggerBundle.message("hxcpp.runner.executable.missing", executablePath.isBlank() ? "<not set>" : executablePath));
    }
    return executable;
  }

  private @Nullable Path resolveExecutableOrNull() {
    return executablePath.isBlank() ? null : resolveAgainstProject(executablePath);
  }

  /** The working directory: the explicit setting, else the executable's directory. */
  public @Nullable Path resolveWorkingDirectory() {
    if (!workingDirectory.isBlank()) {
      try {
        return Path.of(workingDirectory);
      } catch (InvalidPathException e) {
        return null;
      }
    }
    Path executable = resolveExecutableOrNull();
    return executable != null ? executable.getParent() : null;
  }

  /** The executable invocation for this configuration (debug runners add env vars on top). */
  public GeneralCommandLine createCommandLine() throws ExecutionException {
    Path executable = resolveExecutable();
    Path workDir = resolveWorkingDirectory();
    GeneralCommandLine commandLine = new GeneralCommandLine()
      .withExePath(executable.toString())
      .withWorkDirectory(workDir != null ? workDir.toString() : null);
    if (!programArguments.isBlank()) {
      commandLine.addParameters(ParametersListUtil.parse(programArguments));
    }
    return commandLine;
  }

  // --- persistence ---

  @Override
  public void readExternal(@NotNull Element element) throws InvalidDataException {
    super.readExternal(element);
    readModule(element);
    executablePath = orEmpty(JDOMExternalizerUtil.readField(element, EXECUTABLE));
    workingDirectory = orEmpty(JDOMExternalizerUtil.readField(element, WORKING_DIRECTORY));
    programArguments = orEmpty(JDOMExternalizerUtil.readField(element, PROGRAM_ARGUMENTS));
  }

  @Override
  public void writeExternal(@NotNull Element element) throws WriteExternalException {
    super.writeExternal(element); // also serializes the module
    JDOMExternalizerUtil.writeField(element, EXECUTABLE, executablePath);
    JDOMExternalizerUtil.writeField(element, WORKING_DIRECTORY, workingDirectory);
    JDOMExternalizerUtil.writeField(element, PROGRAM_ARGUMENTS, programArguments);
  }
}
