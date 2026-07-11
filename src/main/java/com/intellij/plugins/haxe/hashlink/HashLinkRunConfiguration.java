package com.intellij.plugins.haxe.hashlink;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunConfigurationModule;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.execution.configurations.RuntimeConfigurationWarning;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizerUtil;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.plugins.haxe.HaxeBundle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A HashLink run/debug configuration (experimental): a module (for the Haxe
 * SDK and source lookup) plus the compiled {@code .hl} to execute. When the
 * .hl path is left empty it is auto-detected from the module's build
 * ({@code -hl <out>.hl} in the hxml or compiler arguments).
 */
public class HashLinkRunConfiguration extends ModuleBasedConfiguration<RunConfigurationModule, Element> {
  private static final String HL_FILE = "hlFile";
  private static final String WORKING_DIRECTORY = "workingDirectory";

  private String hlFilePath = "";
  private String workingDirectory = "";

  public HashLinkRunConfiguration(String name, Project project, ConfigurationFactory factory) {
    super(name, new RunConfigurationModule(project), factory);
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

  // --- ModuleBasedConfiguration ---

  @Override
  public Collection<Module> getValidModules() {
    return Arrays.asList(ModuleManager.getInstance(getProject()).getModules());
  }

  @Override
  public @NotNull SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new HashLinkRunConfigurationEditor(getProject());
  }

  @Override
  public void onNewConfigurationCreated() {
    super.onNewConfigurationCreated();
    if (getConfigurationModule().getModule() == null) {
      Module[] modules = ModuleManager.getInstance(getProject()).getModules();
      if (modules.length > 0) {
        setModule(modules[0]);
      }
    }
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
      throw new RuntimeConfigurationError(HaxeBundle.message("hashlink.runner.no.module"));
    }
    Path program = resolveProgramOrNull(module);
    if (program == null) {
      throw new RuntimeConfigurationError(HaxeBundle.message("hashlink.runner.no.hl.file"));
    }
    if (!Files.isRegularFile(program)) {
      throw new RuntimeConfigurationWarning(HaxeBundle.message("haxe.run.hl.output.missing", program.toString()));
    }
    if (HlExecutableResolver.resolve(module).isEmpty()) {
      throw new RuntimeConfigurationWarning(HaxeBundle.message("haxe.run.bad.hl.bin.path"));
    }
  }

  @Override
  public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) throws ExecutionException {
    Module module = requireModule();
    return new HashLinkRunningState(env, module,
                                    HashLinkRunConfigurations.resolveHlExecutable(module),
                                    resolveProgram(module),
                                    resolveWorkingDirectory(module));
  }

  // --- program resolution ---

  Module requireModule() throws ExecutionException {
    Module module = getConfigurationModule().getModule();
    if (module == null) {
      throw new ExecutionException(HaxeBundle.message("no.module.for.run.configuration", getName()));
    }
    return module;
  }

  /** The .hl to execute: the explicit setting, else the build-detected output. */
  Path resolveProgram(Module module) throws ExecutionException {
    Path program = resolveProgramOrNull(module);
    if (program == null || !Files.isRegularFile(program)) {
      throw new ExecutionException(
        HaxeBundle.message("haxe.run.hl.output.missing", program != null ? program.toString() : "<not set>"));
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
  }

  @Override
  public void writeExternal(@NotNull Element element) throws WriteExternalException {
    super.writeExternal(element);
    writeModule(element);
    JDOMExternalizerUtil.writeField(element, HL_FILE, hlFilePath);
    JDOMExternalizerUtil.writeField(element, WORKING_DIRECTORY, workingDirectory);
  }

  private static String orEmpty(@Nullable String value) {
    return value == null ? "" : value;
  }
}
