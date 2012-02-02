package com.intellij.plugins.haxe.runner;

import com.intellij.execution.configurations.RunConfigurationModule;
import com.intellij.openapi.project.Project;

public class HaxeApplicationModuleBasedConfiguration extends RunConfigurationModule {
  public HaxeApplicationModuleBasedConfiguration(Project project) {
    super(project);
  }
}
