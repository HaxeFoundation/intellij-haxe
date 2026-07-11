package com.intellij.plugins.haxe.hashlink;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.util.containers.ContainerUtil;
import javax.swing.Icon;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * Dedicated run/debug configuration type for HashLink (experimental).
 *
 * Deliberately its own type rather than a mode of the generic Haxe Application
 * configuration: the HashLink runners key on {@link HashLinkRunConfiguration},
 * so the legacy runners can never claim a HashLink run and vice versa — no
 * target-detection heuristics involved.
 */
public class HashLinkRunConfigurationType implements ConfigurationType {
  private final HashLinkFactory factory = new HashLinkFactory(this);

  public static HashLinkRunConfigurationType getInstance() {
    return ContainerUtil.findInstance(CONFIGURATION_TYPE_EP.getExtensionList(), HashLinkRunConfigurationType.class);
  }

  @Override
  public @NotNull String getDisplayName() {
    return HaxeBundle.message("hashlink.runner.configuration.name");
  }

  @Override
  public String getConfigurationTypeDescription() {
    return HaxeBundle.message("hashlink.runner.configuration.description");
  }

  @Override
  public Icon getIcon() {
    return icons.HaxeIcons.HAXE_LOGO;
  }

  @Override
  public @NotNull String getId() {
    return "HashLinkRunConfiguration";
  }

  @Override
  public ConfigurationFactory[] getConfigurationFactories() {
    return new ConfigurationFactory[]{factory};
  }

  public static class HashLinkFactory extends ConfigurationFactory {
    public HashLinkFactory(ConfigurationType type) {
      super(type);
    }

    @Override
    public @NotNull RunConfiguration createTemplateConfiguration(@NotNull Project project) {
      return new HashLinkRunConfiguration(HaxeBundle.message("hashlink.runner.configuration.name"), project, this);
    }

    @Override
    public @NotNull @NonNls String getId() {
      // must not come from a localized bundle
      return "HashLink Application";
    }
  }
}
