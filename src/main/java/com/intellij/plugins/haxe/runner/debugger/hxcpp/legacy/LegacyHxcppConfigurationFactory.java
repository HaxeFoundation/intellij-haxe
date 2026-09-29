package com.intellij.plugins.haxe.runner.debugger.hxcpp.legacy;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import icons.HaxeIcons;
import javax.swing.Icon;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * The legacy-HXCPP flavour of the Haxe run configuration group. Keeps the
 * historical factory id {@code Haxe Application}, so run configurations saved
 * by earlier plugin versions still resolve to this flavour (the platform
 * matches saved configurations to factories by id).
 */
public class LegacyHxcppConfigurationFactory extends ConfigurationFactory {

  public LegacyHxcppConfigurationFactory(ConfigurationType type) {
    super(type);
  }

  @Override
  @NotNull
  public String getName() {
    return HaxeDebuggerBundle.message("legacy.hxcpp.runner.configuration.name");
  }

  @Override
  public Icon getIcon() {
    return HaxeIcons.HAXE_LOGO;
  }

  @Override
  public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
    return new LegacyHxcppRunConfiguration(
      HaxeDebuggerBundle.message("legacy.hxcpp.runner.configuration.name"), project, this);
  }

  @Override
  public @NotNull @NonNls String getId() {
    // must not come from a localized bundle - and must stay the historical
    // value so previously saved configurations keep resolving
    return "Haxe Application";
  }
}
