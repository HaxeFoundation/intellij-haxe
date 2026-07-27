package com.intellij.plugins.haxe.runner.debugger.hxcpp.intellij;

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
 * The HXCPP (IntelliJ debug server) flavour of the Haxe run configuration
 * group: debugs executables compiled with {@code -lib
 * intellij-hxcpp-debug-server}, whose embedded server speaks DAP natively.
 * The runners key on {@link HxcppIntellijRunConfiguration}, so neither the
 * legacy runners nor the vshaxe-adapter runners ever see these runs.
 */
public class HxcppIntellijConfigurationFactory extends ConfigurationFactory {

  public HxcppIntellijConfigurationFactory(ConfigurationType type) {
    super(type);
  }

  @Override
  @NotNull
  public String getName() {
    return HaxeDebuggerBundle.message("hxcpp.intellij.runner.configuration.name");
  }

  @Override
  public Icon getIcon() {
    return HaxeIcons.DEBUGGER_HXCPP;
  }

  @Override
  public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
    return new HxcppIntellijRunConfiguration(
      HaxeDebuggerBundle.message("hxcpp.intellij.runner.configuration.name"), project, this);
  }

  @Override
  public @NotNull @NonNls String getId() {
    // must not come from a localized bundle
    return "HXCPP Application (IntelliJ)";
  }
}
