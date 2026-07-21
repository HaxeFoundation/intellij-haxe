package com.intellij.plugins.haxe.runner.debugger.interp;

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
 * The Haxe INTERPRETER flavour of the Haxe run configuration group
 * (experimental): runs a compilation on the compiler's built-in eval VM —
 * either interpreting a program ({@code --interp}) or executing a build whose
 * MACROS are the debug target. Debugging needs no library or runtime: the
 * debug server lives inside haxe itself (haxe 4.0+).
 */
public class InterpConfigurationFactory extends ConfigurationFactory {

  public InterpConfigurationFactory(ConfigurationType type) {
    super(type);
  }

  @Override
  @NotNull
  public String getName() {
    return HaxeDebuggerBundle.message("interp.runner.configuration.name");
  }

  @Override
  public Icon getIcon() {
    return HaxeIcons.HAXE_LOGO;
  }

  @Override
  public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
    return new InterpRunConfiguration(HaxeDebuggerBundle.message("interp.runner.configuration.name"), project, this);
  }

  @Override
  public @NotNull @NonNls String getId() {
    // must not come from a localized bundle
    return "Haxe Interpreter";
  }
}
