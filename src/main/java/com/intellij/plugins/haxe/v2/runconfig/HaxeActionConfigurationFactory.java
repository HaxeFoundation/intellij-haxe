package com.intellij.plugins.haxe.v2.runconfig;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import icons.HaxeIcons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * The build file action flavour of the Haxe run configuration group (v2): runs
 * one action of a build file, resolved live from the tool window configuration.
 */
public class HaxeActionConfigurationFactory extends ConfigurationFactory {

  public HaxeActionConfigurationFactory(ConfigurationType type) {
    super(type);
  }

  @Override
  @NotNull
  public String getName() {
    return HaxeBundle.message("haxe.action.configuration.name");
  }

  @Override
  public Icon getIcon() {
    return HaxeIcons.HAXE_LOGO;
  }

  @Override
  public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
    return new HaxeActionRunConfiguration(project, this, HaxeBundle.message("haxe.action.configuration.name"));
  }

  @Override
  public @NotNull @NonNls String getId() {
    // must not come from a localized bundle
    return "Haxe Action";
  }
}
