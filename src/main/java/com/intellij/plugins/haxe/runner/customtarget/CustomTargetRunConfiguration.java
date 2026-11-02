package com.intellij.plugins.haxe.runner.customtarget;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.runner.debugger.dap.ide.DapExecutableRunConfigurationBase;
import org.jetbrains.annotations.NotNull;

/**
 * Runs any program, for a target the plugin cannot launch itself: an
 * executable (absolute, module-relative or through {@code $PROJECT_DIR$} /
 * {@code $MODULE_DIR$}), a working directory and arguments. Plain Run only;
 * the build happens in a before-launch Haxe build step, which a custom action
 * that creates this configuration supplies.
 */
public class CustomTargetRunConfiguration extends DapExecutableRunConfigurationBase {

  public CustomTargetRunConfiguration(String name, Project project, ConfigurationFactory factory) {
    super(name, project, factory);
  }

  @Override
  public @NotNull SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new CustomTargetRunConfigurationEditor(getProject());
  }
}
