package com.intellij.plugins.haxe.runner.neko;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.runner.HaxeRuntimeRunConfiguration;
import com.intellij.plugins.haxe.v2.buildtools.HaxeToolPathResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A Neko application run configuration, run-only (neko has no debugger): a
 * packaged launcher (lime/nme wrap the bytecode in one), or the neko runtime
 * with the {@code .n} file as its program argument.
 */
public class NekoRunConfiguration extends HaxeRuntimeRunConfiguration {

  public NekoRunConfiguration(String name, Project project, ConfigurationFactory factory) {
    super(name, project, factory);
  }

  @Override
  protected @NotNull String runtimeName() {
    return "neko";
  }

  @Override
  protected @Nullable String locateRuntime() {
    return HaxeToolPathResolver.resolveNekoExecutable(getProject(), null);
  }

  @Override
  protected @NotNull String runtimeMissingMessage() {
    return HaxeDebuggerBundle.message("neko.runner.runtime.missing");
  }

  @Override
  protected @NotNull String executableHint() {
    return HaxeDebuggerBundle.message("neko.runner.editor.executable.hint");
  }
}
