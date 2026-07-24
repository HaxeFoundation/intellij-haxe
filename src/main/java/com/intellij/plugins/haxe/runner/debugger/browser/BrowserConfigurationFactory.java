package com.intellij.plugins.haxe.runner.debugger.browser;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import javax.swing.Icon;

import icons.HaxeIcons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * The browser (haxe -js) flavour of the Haxe run configuration group
 * (experimental). The debug runner keys on {@link BrowserRunConfiguration},
 * so no other runner ever sees a browser run and vice versa.
 */
public class BrowserConfigurationFactory extends ConfigurationFactory {

  public BrowserConfigurationFactory(ConfigurationType type) {
    super(type);
  }

  @Override
  @NotNull
  public String getName() {
    return HaxeDebuggerBundle.message("browser.runner.configuration.name");
  }

  @Override
  public Icon getIcon() {
    return HaxeIcons.DEBUGGER_WEB;
  }

  @Override
  public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
    return new BrowserRunConfiguration(HaxeDebuggerBundle.message("browser.runner.configuration.name"), project, this);
  }

  @Override
  public @NotNull @NonNls String getId() {
    // must not come from a localized bundle
    return "Haxe Web Browser";
  }
}
