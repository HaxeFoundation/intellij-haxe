package com.intellij.plugins.haxe.runner.debugger.flash;

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
 * The AIR flavour of the Haxe run configuration group. The runners key on
 * {@link AirRunConfiguration}, so the other flavours never see an AIR run
 * and vice versa.
 */
public class AirConfigurationFactory extends ConfigurationFactory {

  public AirConfigurationFactory(ConfigurationType type) {
    super(type);
  }

  @Override
  @NotNull
  public String getName() {
    return HaxeDebuggerBundle.message("air.runner.configuration.name");
  }

  @Override
  public Icon getIcon() {
    return HaxeIcons.HAXE_LOGO;
  }

  @Override
  public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
    return new AirRunConfiguration(HaxeDebuggerBundle.message("air.runner.configuration.name"), project, this);
  }

  @Override
  public @NotNull @NonNls String getId() {
    // must not come from a localized bundle
    return "AIR Application";
  }
}
