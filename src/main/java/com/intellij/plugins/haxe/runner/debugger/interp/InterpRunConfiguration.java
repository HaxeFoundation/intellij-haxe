package com.intellij.plugins.haxe.runner.debugger.interp;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapCommandLineRunningState;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapRunConfigurationBase;
import com.intellij.plugins.haxe.v2.buildsystem.HxmlFileParser;
import com.intellij.plugins.haxe.v2.buildtools.HaxeToolPathResolver;
import com.intellij.util.execution.ParametersListUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import lombok.Getter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A Haxe interpreter run/debug configuration (experimental): a module (for
 * source lookup and the compiler to run) plus the compiler arguments of the
 * compilation to run on the eval VM — an hxml file ({@code build.hxml}) or
 * plain arguments ({@code -cp src -main Main}).
 *
 * Two modes, per the eval design (docs in debuggers/eval-debugger):
 * with "run as interpreter" set (the default), {@code --interp} is appended
 * unless the arguments already select it, and the program's own code is
 * debugged; without it the arguments are a regular build, and what is
 * debugged are the macros that run during that compilation. Debugging needs
 * nothing compiled in — the debug server is part of haxe itself (4.0+).
 */
public class InterpRunConfiguration extends DapRunConfigurationBase {
  private static final String COMPILER_ARGUMENTS = "compilerArguments";
  private static final String WORKING_DIRECTORY = "workingDirectory";
  private static final String RUN_AS_INTERPRETER = "runAsInterpreter";
  private static final String HXML_EXTENSION = ".hxml";

  @Getter private String compilerArguments = "";
  @Getter private String workingDirectory = "";
  @Getter private boolean runAsInterpreter = true;

  public InterpRunConfiguration(String name, Project project, ConfigurationFactory factory) {
    super(name, project, factory);
  }

  public void setCompilerArguments(@Nullable String arguments) {
    compilerArguments = arguments == null ? "" : arguments;
  }

  public void setWorkingDirectory(@Nullable String directory) {
    workingDirectory = directory == null ? "" : directory;
  }

  public void setRunAsInterpreter(boolean interpret) {
    runAsInterpreter = interpret;
  }

  @Override
  public @NotNull SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new InterpRunConfigurationEditor(getProject());
  }

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {
    if (getConfigurationModule().getModule() == null) {
      throw new RuntimeConfigurationError(HaxeDebuggerBundle.message("interp.runner.no.module"));
    }
    if (compilerArguments.isBlank()) {
      throw new RuntimeConfigurationError(HaxeDebuggerBundle.message("interp.runner.no.arguments"));
    }
  }

  @Override
  public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) throws ExecutionException {
    requireModule();
    return new DapCommandLineRunningState(env, getProject(), () -> createCommandLine(List.of()));
  }

  // --- resolution ---

  /**
   * The haxe invocation for this configuration, with {@code extraArguments}
   * (e.g. the debugger define) inserted BEFORE the trailing {@code --interp}
   * so they always belong to the compilation itself.
   */
  GeneralCommandLine createCommandLine(List<String> extraArguments) throws ExecutionException {
    if (compilerArguments.isBlank()) {
      throw new ExecutionException(HaxeDebuggerBundle.message("interp.runner.no.arguments"));
    }
    Module module = requireModule();
    List<String> arguments = ParametersListUtil.parse(compilerArguments);
    Path workDirectory = resolveWorkingDirectory();
    GeneralCommandLine commandLine = new GeneralCommandLine()
      .withExePath(resolveCompiler(module))
      .withWorkDirectory(workDirectory != null ? workDirectory.toString() : null);
    commandLine.addParameters(arguments);
    commandLine.addParameters(extraArguments);
    if (runAsInterpreter && !declaresInterp(arguments, workDirectory)) {
      commandLine.addParameter("--interp");
    }
    return commandLine;
  }

  /**
   * The haxe of the module's SDK (its Environment SDK, else the Build Tools
   * one, else any registered Haxe SDK). Without one the bare name runs from
   * the PATH; it is looked up here so that a missing compiler fails with a
   * message instead of at process start.
   */
  private String resolveCompiler(Module module) throws ExecutionException {
    String sdkName = HaxeToolPathResolver.effectiveSdkName(getProject(), module.getName());
    String compiler = HaxeToolPathResolver.resolveHaxeExecutable(getProject(), sdkName);
    boolean onPathOnly = !Path.of(compiler).isAbsolute();
    if (onPathOnly && HaxeToolPathResolver.pathDetectedExecutable("haxe") == null) {
      throw new ExecutionException(HaxeDebuggerBundle.message("interp.runner.compiler.missing", compiler));
    }
    return compiler;
  }

  /** The working directory: the explicit setting (a relative one resolved against the module folder), else the module folder. */
  public @Nullable Path resolveWorkingDirectory() {
    return workingDirectory.isBlank() ? baseDirectory() : resolveAgainstModule(workingDirectory);
  }

  /**
   * Whether the arguments already select the interpreter — as a flag, or
   * inside an hxml they name — so that appending {@code --interp} would make
   * the compiler reject the build with "Multiple targets".
   */
  static boolean declaresInterp(List<String> arguments, @Nullable Path workDirectory) {
    boolean asFlag = arguments.stream().anyMatch(InterpRunConfiguration::isInterpFlag);
    return asFlag || arguments.stream().anyMatch(argument -> hxmlDeclaresInterp(argument, workDirectory));
  }

  private static boolean isInterpFlag(String token) {
    return HxmlFileParser.targetForFlag(token) == HaxeTarget.INTERP;
  }

  /**
   * Whether the argument names an existing hxml (resolved against the working
   * directory, like the compiler does) whose effective content selects the
   * interpreter in any section. Nested hxml references resolve against the
   * working directory too — haxe reads them relative to its cwd, not to the
   * referencing file.
   */
  static boolean hxmlDeclaresInterp(String argument, @Nullable Path workDirectory) {
    if (!argument.endsWith(HXML_EXTENSION)) return false;
    String content = readOrNull(resolveOrNull(workDirectory, argument));
    if (content == null) return false;
    String effective = HxmlFileParser.flatten(content, reference -> readOrNull(resolveOrNull(workDirectory, reference)));
    return HxmlFileParser.declaresTarget(effective, HaxeTarget.INTERP);
  }

  private static @Nullable Path resolveOrNull(@Nullable Path workDirectory, String path) {
    try {
      return workDirectory != null ? workDirectory.resolve(path) : Path.of(path);
    } catch (InvalidPathException e) {
      return null;
    }
  }

  private static @Nullable String readOrNull(@Nullable Path file) {
    if (file == null || !Files.isRegularFile(file)) return null;
    try {
      return Files.readString(file);
    } catch (IOException e) {
      return null;
    }
  }

  // --- persistence ---

  @Override
  public void readExternal(@NotNull Element element) throws InvalidDataException {
    super.readExternal(element);
    readModule(element);
    compilerArguments = orEmpty(JDOMExternalizerUtil.readField(element, COMPILER_ARGUMENTS));
    workingDirectory = orEmpty(JDOMExternalizerUtil.readField(element, WORKING_DIRECTORY));
    runAsInterpreter = !"false".equals(JDOMExternalizerUtil.readField(element, RUN_AS_INTERPRETER));
  }

  @Override
  public void writeExternal(@NotNull Element element) throws WriteExternalException {
    super.writeExternal(element); // also serializes the module
    JDOMExternalizerUtil.writeField(element, COMPILER_ARGUMENTS, compilerArguments);
    JDOMExternalizerUtil.writeField(element, WORKING_DIRECTORY, workingDirectory);
    JDOMExternalizerUtil.writeField(element, RUN_AS_INTERPRETER, Boolean.toString(runAsInterpreter));
  }

}
