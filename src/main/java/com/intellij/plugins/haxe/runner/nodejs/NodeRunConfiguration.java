package com.intellij.plugins.haxe.runner.nodejs;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.runner.HaxeRuntimeRunConfiguration;
import com.intellij.plugins.haxe.runner.debugger.javascript.AdapterPin;
import com.intellij.plugins.haxe.v2.buildtools.HaxeToolPathResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A Node.js application run configuration: the node runtime with the
 * compiled {@code .js} script as its program argument. Run, and Debug
 * through {@link NodeDebugRunner}.
 */
// TODO: a profiler lane over the inspector protocol's CPU profiler
public class NodeRunConfiguration extends HaxeRuntimeRunConfiguration {

  public NodeRunConfiguration(String name, Project project, ConfigurationFactory factory) {
    super(name, project, factory);
  }

  @Override
  protected @NotNull String runtimeName() {
    return "node";
  }

  @Override
  protected @Nullable String locateRuntime() {
    return HaxeToolPathResolver.resolveNodeExecutable(getProject(), null);
  }

  @Override
  protected @NotNull String runtimeMissingMessage() {
    return HaxeDebuggerBundle.message("node.runner.runtime.missing");
  }

  @Override
  protected @NotNull String executableHint() {
    return HaxeDebuggerBundle.message("node.runner.editor.executable.hint");
  }

  @Override
  protected @NotNull AdapterPin debugAdapter() {
    return AdapterPin.JS_DEBUG;
  }
}
