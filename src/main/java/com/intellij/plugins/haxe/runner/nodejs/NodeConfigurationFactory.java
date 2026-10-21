package com.intellij.plugins.haxe.runner.nodejs;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import icons.HaxeIcons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

/** The Node.js flavour of the Haxe run configuration group; {@link NodeRunner} keys on its configurations. */
public class NodeConfigurationFactory extends ConfigurationFactory {

  public NodeConfigurationFactory(ConfigurationType type) {
    super(type);
  }

  @Override
  @NotNull
  public String getName() {
    return HaxeDebuggerBundle.message("node.runner.configuration.name");
  }

  @Override
  public Icon getIcon() {
    return HaxeIcons.HAXE_LOGO;
  }

  @Override
  public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
    return new NodeRunConfiguration(HaxeDebuggerBundle.message("node.runner.configuration.name"), project, this);
  }

  @Override
  public @NotNull @NonNls String getId() {
    // must not come from a localized bundle
    return "Node.js Application";
  }
}
