package com.intellij.plugins.haxe.runner.neko;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.configurations.RuntimeConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapExecutableRunConfigurationBase;
import com.intellij.plugins.haxe.v2.buildtools.HaxeToolPathResolver;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

/**
 * A Neko application run configuration: the shared executable configuration,
 * run-only (neko has no debugger). The executable is either a packaged
 * launcher (lime/nme wrap the bytecode in one) or the neko runtime with the
 * {@code .n} file as its program argument. An empty executable — or the bare
 * runtime name — means the neko RUNTIME, resolved at LAUNCH time through the
 * shared precedence (Build Tools setting, SDK, PATH): the same lookup every
 * neko launch in the plugin uses, and never a value baked at configuration
 * creation that a settings change could not reach.
 */
public class NekoRunConfiguration extends DapExecutableRunConfigurationBase {

  public NekoRunConfiguration(String name, Project project, ConfigurationFactory factory) {
    super(name, project, factory);
  }

  @Override
  public @NotNull SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new NekoRunConfigurationEditor(getProject());
  }

  private boolean usesResolvedRuntime() {
    String path = getExecutablePath();
    return path.isBlank() || path.equals("neko") || path.equals("neko.exe");
  }

  @Override
  public Path resolveExecutable() throws ExecutionException {
    if (!usesResolvedRuntime()) {
      return super.resolveExecutable();
    }
    Path runtime = Path.of(HaxeToolPathResolver.resolveNekoExecutable(getProject(), null));
    if (!Files.isRegularFile(runtime)) {
      throw new ExecutionException(HaxeDebuggerBundle.message("neko.runner.runtime.missing"));
    }
    return runtime;
  }

  @Override
  public void checkConfiguration() throws RuntimeConfigurationException {
    if (!usesResolvedRuntime()) {
      super.checkConfiguration();
      return;
    }
    // the runtime is resolved at launch; only the module needs to exist here
    if (getConfigurationModule().getModule() == null) {
      throw new RuntimeConfigurationError(HaxeDebuggerBundle.message("neko.runner.no.module"));
    }
  }
}
