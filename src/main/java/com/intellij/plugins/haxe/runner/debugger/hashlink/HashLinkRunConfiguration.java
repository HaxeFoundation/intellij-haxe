package com.intellij.plugins.haxe.runner.debugger.hashlink;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.configurations.RuntimeConfigurationWarning;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.project.DumbService;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.profiler.HaxeProfilableRunConfiguration;
import com.intellij.plugins.haxe.profiler.HaxeProfilerExecutorSupport;
import com.intellij.plugins.haxe.profiler.HaxeProfilerProcessUi;
import com.intellij.plugins.haxe.profiler.HaxeProfilingNotifier;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapCommandLineRunningState;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapRunConfigurationBase;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// A HashLink run/debug configuration (experimental): a module (for the Haxe
/// SDK and source lookup) plus the compiled `.hl` to execute. When the
/// .hl path is left empty it is auto-detected from the module's build
/// (`-hl <out>.hl` in the hxml or compiler arguments).
public class HashLinkRunConfiguration extends DapRunConfigurationBase implements HaxeProfilableRunConfiguration {
  private static final String HL_FILE = "hlFile";
  private static final String WORKING_DIRECTORY = "workingDirectory";
  private static final String USE_CUSTOM_HL_BINARY = "useCustomHlBinary";
  private static final String CUSTOM_HL_BINARY = "customHlBinary";

  /** The dump file name hashlink's profiler writes into the process working directory. */
  public static final String PROFILER_DUMP_FILE_NAME = "hlprofile.dump";

  private String hlFilePath = "";
  private String workingDirectory = "";
  // when enabled, this HashLink executable runs the program instead of the
  // SDK/environment-resolved one (e.g. a Lime/OpenFL game's renamed hl.exe)
  private boolean useCustomHlBinary = false;
  private String customHlBinaryPath = "";

  public HashLinkRunConfiguration(String name, Project project, ConfigurationFactory factory) {
    super(name, project, factory);
  }

  // --- settings ---

  public String getHlFilePath() {
    return hlFilePath;
  }

  public void setHlFilePath(@Nullable String path) {
    hlFilePath = path == null ? "" : path;
  }

  public String getWorkingDirectory() {
    return workingDirectory;
  }

  public void setWorkingDirectory(@Nullable String directory) {
    workingDirectory = directory == null ? "" : directory;
  }

  public boolean isUseCustomHlBinary() {
    return useCustomHlBinary;
  }

  public void setUseCustomHlBinary(boolean use) {
    useCustomHlBinary = use;
  }

  public String getCustomHlBinaryPath() {
    return customHlBinaryPath;
  }

  public void setCustomHlBinaryPath(@Nullable String path) {
    customHlBinaryPath = path == null ? "" : path;
  }

  /**
   * This configuration always launches HL output, so profiling is ready
   * whenever the configuration could run at all — the gate only mirrors
   * Run/Debug's graying while the project is indexing. Which configuration
   * the buttons act on is the run widget's SELECTION; the tool window's
   * target dropdown is not consulted.
   */
  @Override
  public @NotNull Lane profilingLane() {
    return Lane.HASHLINK;
  }

  @Override
  public boolean isProfilingReady() {
    return !DumbService.isDumb(getProject());
  }

  // --- ModuleBasedConfiguration ---

  @Override
  public @NotNull SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new HashLinkRunConfigurationEditor(getProject());
  }

  @Override
  public void onNewConfigurationCreated() {
    super.onNewConfigurationCreated();
    // convenience: prefill the .hl from the module's build when detectable
    Module module = getConfigurationModule().getModule();
    if (module != null && hlFilePath.isBlank()) {
      HashLinkRunConfigurations.detectedOutput(module).ifPresent(path -> hlFilePath = path.toString());
    }
  }

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {
    Module module = getConfigurationModule().getModule();
    if (module == null) {
      throw new RuntimeConfigurationError(HaxeDebuggerBundle.message("hashlink.runner.no.module"));
    }
    Path program = resolveProgramOrNull(module);
    if (program == null) {
      throw new RuntimeConfigurationError(HaxeDebuggerBundle.message("hashlink.runner.no.hl.file"));
    }
    if (!Files.isRegularFile(program)) {
      throw new RuntimeConfigurationWarning(HaxeDebuggerBundle.message("haxe.run.hl.output.missing", program.toString()));
    }
    if (useCustomHlBinary) {
      Path custom = HashLinkRunConfigurations.resolveAgainstModule(module, customHlBinaryPath);
      if (customHlBinaryPath.isBlank() || custom == null) {
        throw new RuntimeConfigurationError(HaxeDebuggerBundle.message("hashlink.runner.custom.hl.not.set"));
      }
      if (!Files.isRegularFile(custom)) {
        throw new RuntimeConfigurationWarning(HaxeDebuggerBundle.message("haxe.run.custom.hl.missing", custom.toString()));
      }
    } else if (HlExecutableResolver.resolve(module).isEmpty()) {
      throw new RuntimeConfigurationWarning(HaxeDebuggerBundle.message("haxe.run.bad.hl.bin.path"));
    }
  }

  // Plain Run: hl <program.hl>, output in the console. Default working
  // directory is the program's directory so relative resource loading behaves
  // like a manual launch. Resolution stays inside the supplier: getState runs
  // before before-launch tasks, so the .hl a compile step produces may not
  // exist yet.
  @Override
  public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) {
    // non-null exactly when the IU "Run with Profiler" executor launched us
    // with the HashLink profiler configuration selected
    Integer profilerSamples = HaxeProfilerExecutorSupport.hashlinkSamplesFor(executor);
    return new DapCommandLineRunningState(env, getProject(), () -> createRunCommandLine(profilerSamples)) {
      @Override
      protected @NotNull ProcessHandler startProcess() throws ExecutionException {
        ProcessHandler handler = super.startProcess();
        if (profilerSamples != null) {
          // the sampler is in-process and live from launch
          Path dumpPath = expectedDumpPath();
          HaxeProfilerProcessUi.Session session = HaxeProfilerProcessUi.notifyAttached(getProject(), getName(), dumpPath);
          HaxeProfilingNotifier.watch(getProject(), handler, dumpPath, "haxe.profiler.dump.missing", session);
        }
        return handler;
      }
    };
  }

  private GeneralCommandLine createRunCommandLine(@Nullable Integer profilerSamples) throws ExecutionException {
    Module module = requireModule();
    Path hlExecutable = resolveHlExecutable(module);
    Path hlProgram = resolveProgram(module);
    Path workingDir = resolveWorkingDirectory(module);
    Path workDir = workingDir != null ? workingDir : hlProgram.getParent();

    GeneralCommandLine commandLine = new GeneralCommandLine().withExePath(hlExecutable.toString());
    if (profilerSamples != null) {
      // hidden VM flag; must come BEFORE the program path. The dump is only
      // written on clean exit - see HaxeProfilingNotifier.
      commandLine.withParameters("--profile", String.valueOf(profilerSamples));
    }
    return commandLine
      .withParameters(hlProgram.toString())
      .withWorkDirectory(workDir != null ? workDir.toString() : null);
  }

  /** Where this run's dump will appear: hashlink writes it into the process working directory. */
  private Path expectedDumpPath() throws ExecutionException {
    Module module = requireModule();
    Path workingDir = resolveWorkingDirectory(module);
    Path base = workingDir != null ? workingDir : resolveProgram(module).getParent();
    return base.resolve(PROFILER_DUMP_FILE_NAME);
  }

  /**
   * The HashLink executable to run the program with: the custom override when
   * enabled (it must exist — a broken override fails loudly instead of falling
   * back to an unexpected binary), else the SDK/environment-resolved one.
   */
  Path resolveHlExecutable(Module module) throws ExecutionException {
    if (!useCustomHlBinary) {
      return HashLinkRunConfigurations.resolveHlExecutable(module);
    }
    Path custom = HashLinkRunConfigurations.resolveAgainstModule(module, customHlBinaryPath);
    if (custom == null || !Files.isRegularFile(custom)) {
      throw new ExecutionException(
        HaxeDebuggerBundle.message("haxe.run.custom.hl.missing", custom != null ? custom.toString() : "<not set>"));
    }
    return custom;
  }

  // --- program resolution ---

  /** The .hl to execute: the explicit setting, else the build-detected output. */
  Path resolveProgram(Module module) throws ExecutionException {
    Path program = resolveProgramOrNull(module);
    if (program == null || !Files.isRegularFile(program)) {
      throw new ExecutionException(
        HaxeDebuggerBundle.message("haxe.run.hl.output.missing", program != null ? program.toString() : "<not set>"));
    }
    return program;
  }

  private @Nullable Path resolveProgramOrNull(Module module) {
    if (!hlFilePath.isBlank()) {
      return HashLinkRunConfigurations.resolveAgainstModule(module, hlFilePath);
    }
    return HashLinkRunConfigurations.detectedOutput(module).orElse(null);
  }

  /** The working directory: the explicit setting, else the program's directory. */
  @Nullable Path resolveWorkingDirectory(Module module) {
    if (!workingDirectory.isBlank()) {
      return HashLinkRunConfigurations.resolveAgainstModule(module, workingDirectory);
    }
    Path program = resolveProgramOrNull(module);
    return program != null ? program.getParent() : null;
  }

  // --- persistence ---

  @Override
  public void readExternal(@NotNull Element element) throws InvalidDataException {
    super.readExternal(element);
    readModule(element);
    hlFilePath = orEmpty(JDOMExternalizerUtil.readField(element, HL_FILE));
    workingDirectory = orEmpty(JDOMExternalizerUtil.readField(element, WORKING_DIRECTORY));
    useCustomHlBinary = Boolean.parseBoolean(JDOMExternalizerUtil.readField(element, USE_CUSTOM_HL_BINARY));
    customHlBinaryPath = orEmpty(JDOMExternalizerUtil.readField(element, CUSTOM_HL_BINARY));
  }

  @Override
  public void writeExternal(@NotNull Element element) throws WriteExternalException {
    super.writeExternal(element); // also serializes the module
    JDOMExternalizerUtil.writeField(element, HL_FILE, hlFilePath);
    JDOMExternalizerUtil.writeField(element, WORKING_DIRECTORY, workingDirectory);
    JDOMExternalizerUtil.writeField(element, USE_CUSTOM_HL_BINARY, String.valueOf(useCustomHlBinary));
    JDOMExternalizerUtil.writeField(element, CUSTOM_HL_BINARY, customHlBinaryPath);
  }
}
