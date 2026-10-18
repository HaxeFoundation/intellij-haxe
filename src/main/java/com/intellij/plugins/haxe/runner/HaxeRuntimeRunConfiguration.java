package com.intellij.plugins.haxe.runner;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapExecutableRunConfigurationBase;
import com.intellij.plugins.haxe.runner.debugger.javascript.AdapterPin;
import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * A run-only configuration that starts a runtime with the build output as its
 * program argument (neko on a {@code .n}, node on a {@code .js}), or a
 * packaged launcher directly. An empty executable - or the runtime's bare
 * name - means the RUNTIME, resolved at LAUNCH time through the shared
 * precedence (Build Tools setting, SDK, PATH), never a path baked at
 * creation that a settings change could not reach.
 */
public abstract class HaxeRuntimeRunConfiguration extends DapExecutableRunConfigurationBase {

  protected HaxeRuntimeRunConfiguration(String name, Project project, ConfigurationFactory factory) {
    super(name, project, factory);
  }

  /** The runtime's bare executable name: {@code neko}, {@code node}. */
  @NotNull
  protected abstract String runtimeName();

  /** The runtime as the shared precedence finds it; null, or a path to nothing, when there is none. */
  @Nullable
  protected abstract String locateRuntime();

  /** Why a launch cannot start without the runtime, and where to configure it. */
  @NotNull
  protected abstract String runtimeMissingMessage();

  /** The editor's hint under the executable field. */
  @NotNull
  protected abstract String executableHint();

  /** The DAP adapter the Debug executor needs, offered for download in the editor; null for a runtime debugged without one. */
  @Nullable
  protected AdapterPin debugAdapter() {
    return null;
  }

  @Override
  public @NotNull SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new HaxeRuntimeRunConfigurationEditor(getProject(), executableHint(), debugAdapter());
  }

  private boolean usesResolvedRuntime() {
    String path = getExecutablePath();
    return path.isBlank() || path.equals(runtimeName()) || path.equals(runtimeName() + ".exe");
  }

  @Override
  public Path resolveExecutable() throws ExecutionException {
    if (!usesResolvedRuntime()) {
      return super.resolveExecutable();
    }
    String runtime = locateRuntime();
    if (runtime == null || !Files.isRegularFile(Path.of(runtime))) {
      throw new ExecutionException(runtimeMissingMessage());
    }
    return Path.of(runtime);
  }

  /** With the runtime as the executable, a blank working directory is the script's folder, not the runtime's. */
  @Override
  public @Nullable Path resolveWorkingDirectory() {
    if (!getWorkingDirectory().isBlank() || !usesResolvedRuntime()) {
      return super.resolveWorkingDirectory();
    }
    List<String> arguments = ParametersListUtil.parse(getProgramArguments());
    Path script = arguments.isEmpty() ? null : resolveAgainstModule(arguments.getFirst());
    return script != null ? script.getParent() : super.resolveWorkingDirectory();
  }

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {
    if (!usesResolvedRuntime()) {
      super.checkConfiguration();
      return;
    }
    // the runtime is resolved at launch; only the module needs to exist here
    if (getConfigurationModule().getModule() == null) {
      throw new RuntimeConfigurationError(HaxeDebuggerBundle.message("hxcpp.runner.no.module"));
    }
  }
}
