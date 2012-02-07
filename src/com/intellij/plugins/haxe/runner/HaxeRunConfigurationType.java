package com.intellij.plugins.haxe.runner;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeIcons;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class HaxeRunConfigurationType implements ConfigurationType {
  private final HaxeFactory configurationFactory;

  public HaxeRunConfigurationType() {
    configurationFactory = new HaxeFactory(this);
  }

  public static HaxeRunConfigurationType getInstance() {
    return ContainerUtil.findInstance(Extensions.getExtensions(CONFIGURATION_TYPE_EP), HaxeRunConfigurationType.class);
  }

  public String getDisplayName() {
    return HaxeBundle.message("runner.configuration.name");
  }

  public String getConfigurationTypeDescription() {
    return HaxeBundle.message("runner.configuration.name");
  }

  public Icon getIcon() {
    return HaxeIcons.HAXE_ICON_16x16;
  }

  @NotNull
  public String getId() {
    return "HaxeApplicationRunConfiguration";
  }

  public ConfigurationFactory[] getConfigurationFactories() {
    return new ConfigurationFactory[]{configurationFactory};
  }

  public static class HaxeFactory extends ConfigurationFactory {

    public HaxeFactory(ConfigurationType type) {
      super(type);
    }

    public RunConfiguration createTemplateConfiguration(Project project) {
      final String name = HaxeBundle.message("runner.configuration.name");
      return new HaxeApplicationConfiguration(name, project, getInstance());
    }
  }
}
