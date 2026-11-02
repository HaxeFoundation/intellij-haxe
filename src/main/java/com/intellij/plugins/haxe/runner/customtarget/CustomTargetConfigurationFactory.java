package com.intellij.plugins.haxe.runner.customtarget;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import icons.HaxeIcons;
import javax.swing.Icon;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/** The Custom Target flavour of the Haxe run configuration group; {@link CustomTargetRunner} keys on its configurations. */
public class CustomTargetConfigurationFactory extends ConfigurationFactory {

  public CustomTargetConfigurationFactory(ConfigurationType type) {
    super(type);
  }

  @Override
  @NotNull
  public String getName() {
    return HaxeDebuggerBundle.message("custom.target.runner.configuration.name");
  }

  @Override
  public Icon getIcon() {
    return HaxeIcons.HAXE_LOGO;
  }

  @Override
  public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
    return new CustomTargetRunConfiguration(HaxeDebuggerBundle.message("custom.target.runner.configuration.name"), project, this);
  }

  @Override
  public @NotNull @NonNls String getId() {
    // must not come from a localized bundle
    return "Custom Target Application";
  }
}
