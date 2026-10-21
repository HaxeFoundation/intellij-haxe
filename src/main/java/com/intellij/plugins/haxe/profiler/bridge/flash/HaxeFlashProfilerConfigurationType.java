package com.intellij.plugins.haxe.profiler.bridge.flash;

import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.plugins.haxe.HaxeProfilerBundle;
import com.intellij.plugins.haxe.profiler.HaxeProfilableRunConfiguration;
import com.intellij.plugins.haxe.profiler.bridge.HaxeProfilerConfigurationTypeBase;
import org.jetbrains.annotations.NotNull;

/**
 * The "Flash Profiler" entry of the IU Run-with-Profiler executor: an AIR
 * launch in the DEBUGGER runtime (adl without -nodebug) whose swf carries
 * {@code -D advanced-telemetry}. The runtime streams its own telemetry
 * (frames, render spans, sampler stacks at its fixed ~1 ms rate, memory and
 * GC) to the IDE, which transcodes it while the program runs.
 */
public class HaxeFlashProfilerConfigurationType extends HaxeProfilerConfigurationTypeBase<HaxeFlashProfilerConfigurationState> {

  public static final String ID = "HaxeFlashProfilerConfiguration";

  @Override
  public @NotNull String getId() {
    return ID;
  }

  @Override
  public @NotNull String getDisplayName() {
    return HaxeProfilerBundle.message("haxe.profiler.flash.configuration.name");
  }

  @Override
  protected @NotNull HaxeProfilableRunConfiguration.Lane lane() {
    return HaxeProfilableRunConfiguration.Lane.FLASH;
  }

  @Override
  protected @NotNull String stateElementName() {
    return "haxeFlashProfiler";
  }

  @Override
  public @NotNull HaxeFlashProfilerConfigurationState getTemplateState() {
    return new HaxeFlashProfilerConfigurationState(getDisplayName());
  }

  @Override
  public @NotNull HaxeFlashProfilerConfigurationState copyState(@NotNull HaxeFlashProfilerConfigurationState state) {
    return new HaxeFlashProfilerConfigurationState(state.getDisplayName());
  }

  @Override
  public @NotNull UnnamedConfigurable createConfigurable(@NotNull HaxeFlashProfilerConfigurationState state) {
    return new HaxeFlashProfilerConfigurable();
  }
}
