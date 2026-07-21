package com.intellij.plugins.haxe.runner.debugger.hxcpp.intellij;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapExecutableRunConfigurationBase;
import org.jetbrains.annotations.NotNull;

/**
 * An HXCPP (IntelliJ debug server) run/debug configuration: exactly the
 * shared executable configuration. For debugging, the executable must have
 * been compiled with {@code -debug} and {@code -lib
 * intellij-hxcpp-debug-server}. No host/port settings: the IDE listens on an
 * ephemeral loopback port per session and hands it to the debuggee through
 * env vars, so nothing is baked into the build and concurrent sessions never
 * collide. A build with the library runs normally outside the debugger (the
 * server makes one quick connect attempt and stays out of the way).
 */
public class HxcppIntellijRunConfiguration extends DapExecutableRunConfigurationBase {

  public HxcppIntellijRunConfiguration(String name, Project project, ConfigurationFactory factory) {
    super(name, project, factory);
  }

  @Override
  public @NotNull SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new HxcppIntellijRunConfigurationEditor(getProject());
  }
}
