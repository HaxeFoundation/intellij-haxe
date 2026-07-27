package com.intellij.plugins.haxe.runner.debugger.hashlink;

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
 * The HashLink flavour of the Haxe run configuration group (experimental).
 * The runners key on {@link HashLinkRunConfiguration}, so the legacy runners
 * never see a HashLink run and vice versa.
 */
public class HashLinkConfigurationFactory extends ConfigurationFactory {

  public HashLinkConfigurationFactory(ConfigurationType type) {
    super(type);
  }

  @Override
  @NotNull
  public String getName() {
    return HaxeDebuggerBundle.message("hashlink.runner.configuration.name");
  }

  @Override
  public Icon getIcon() {
    return HaxeIcons.DEBUGGER_HASHLINK;
  }

  @Override
  public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
    return new HashLinkRunConfiguration(HaxeDebuggerBundle.message("hashlink.runner.configuration.name"), project, this);
  }

  @Override
  public @NotNull @NonNls String getId() {
    // must not come from a localized bundle
    return "HashLink Application";
  }
}
