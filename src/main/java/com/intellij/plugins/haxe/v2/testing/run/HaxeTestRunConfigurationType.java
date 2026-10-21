package com.intellij.plugins.haxe.v2.testing.run;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.icons.AllIcons;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * The Haxe unit-test run configuration type - separate from the Haxe application
 * group so the Run dialog shows it with the platform's test icon, the way other
 * test frameworks appear.
 */
public class HaxeTestRunConfigurationType implements ConfigurationType {

  private final HaxeTestConfigurationFactory factory = new HaxeTestConfigurationFactory(this);

  public static HaxeTestRunConfigurationType getInstance() {
    return ContainerUtil.findInstance(CONFIGURATION_TYPE_EP.getExtensionList(), HaxeTestRunConfigurationType.class);
  }

  @NotNull
  public HaxeTestConfigurationFactory getFactory() {
    return factory;
  }

  @Override
  public String getDisplayName() {
    return HaxeBundle.message("haxe.test.configuration.name");
  }

  @Override
  public String getConfigurationTypeDescription() {
    return HaxeBundle.message("haxe.test.configuration.description");
  }

  @Override
  public Icon getIcon() {
    return AllIcons.RunConfigurations.Junit;
  }

  @Override
  public @NotNull String getId() {
    return "HaxeTestRunConfiguration";
  }

  @Override
  public ConfigurationFactory[] getConfigurationFactories() {
    return new ConfigurationFactory[]{factory};
  }
}
