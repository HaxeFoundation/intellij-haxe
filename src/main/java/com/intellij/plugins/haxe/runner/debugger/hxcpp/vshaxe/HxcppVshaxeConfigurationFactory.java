package com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe;

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
 * The HXCPP (vshaxe debug server) flavour of the Haxe run configuration
 * group (experimental). The runners key on {@link HxcppVshaxeRunConfiguration}, so
 * the legacy runners (which also carry Flash/Flex debugging) never see an
 * HXCPP run and vice versa.
 */
public class HxcppVshaxeConfigurationFactory extends ConfigurationFactory {

  public HxcppVshaxeConfigurationFactory(ConfigurationType type) {
    super(type);
  }

  @Override
  @NotNull
  public String getName() {
    return HaxeDebuggerBundle.message("hxcpp.runner.configuration.name");
  }

  @Override
  public Icon getIcon() {
    return HaxeIcons.VSHAXE;
  }

  @Override
  public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
    return new HxcppVshaxeRunConfiguration(HaxeDebuggerBundle.message("hxcpp.runner.configuration.name"), project, this);
  }

  @Override
  public @NotNull @NonNls String getId() {
    // must not come from a localized bundle
    return "HXCPP Application";
  }
}
