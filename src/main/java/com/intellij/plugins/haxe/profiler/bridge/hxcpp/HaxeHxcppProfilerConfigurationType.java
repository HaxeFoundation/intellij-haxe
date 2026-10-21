package com.intellij.plugins.haxe.profiler.bridge.hxcpp;

import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.plugins.haxe.HaxeProfilerBundle;
import com.intellij.plugins.haxe.profiler.HaxeProfilableRunConfiguration;
import com.intellij.plugins.haxe.profiler.bridge.HaxeProfilerConfigurationTypeBase;
import com.intellij.ui.components.JBLabel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

/**
 * The "hxcpp Profiler" entry of the IU Run-with-Profiler executor: builds the
 * program with hxcpp's built-in sampler compiled in (an injected bootstrap
 * starts and stops it around main). No settings: the sampler ticks at a
 * fixed 1 ms.
 */
public class HaxeHxcppProfilerConfigurationType extends HaxeProfilerConfigurationTypeBase<HaxeHxcppProfilerConfigurationState> {

  public static final String ID = "HaxeHxcppProfilerConfiguration";

  @Override
  public @NotNull String getId() {
    return ID;
  }

  @Override
  public @NotNull String getDisplayName() {
    return HaxeProfilerBundle.message("haxe.profiler.hxcpp.configuration.name");
  }

  @Override
  protected @NotNull HaxeProfilableRunConfiguration.Lane lane() {
    return HaxeProfilableRunConfiguration.Lane.HXCPP;
  }

  @Override
  protected @NotNull String stateElementName() {
    return "haxeHxcppProfiler";
  }

  @Override
  public @NotNull HaxeHxcppProfilerConfigurationState getTemplateState() {
    return new HaxeHxcppProfilerConfigurationState(getDisplayName());
  }

  @Override
  public @NotNull HaxeHxcppProfilerConfigurationState copyState(@NotNull HaxeHxcppProfilerConfigurationState state) {
    return new HaxeHxcppProfilerConfigurationState(state.getDisplayName());
  }

  @Override
  public @NotNull UnnamedConfigurable createConfigurable(@NotNull HaxeHxcppProfilerConfigurationState state) {
    return new UnnamedConfigurable() {
      @Override
      public @Nullable JComponent createComponent() {
        return new JBLabel(HaxeProfilerBundle.message("haxe.profiler.hxcpp.settings.fixed.rate"));
      }

      @Override
      public boolean isModified() {
        return false;
      }

      @Override
      public void apply() {
      }
    };
  }
}
