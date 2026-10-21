package com.intellij.plugins.haxe.v2.testing.run;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/** Factory of the single Haxe unit-test configuration flavour. */
public class HaxeTestConfigurationFactory extends ConfigurationFactory {

  public HaxeTestConfigurationFactory(@NotNull ConfigurationType type) {
    super(type);
  }

  @Override
  @NotNull
  public String getName() {
    return HaxeBundle.message("haxe.test.configuration.name");
  }

  @Override
  public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
    return new HaxeTestRunConfiguration(project, this, getName());
  }

  @Override
  public @NotNull @NonNls String getId() {
    // must not come from a localized bundle
    return "Haxe Unit Tests";
  }
}
