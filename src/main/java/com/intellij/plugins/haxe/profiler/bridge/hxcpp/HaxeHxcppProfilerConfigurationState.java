package com.intellij.plugins.haxe.profiler.bridge.hxcpp;

import com.intellij.plugins.haxe.profiler.bridge.HaxeProfilerConfigurationStateBase;
import org.jetbrains.annotations.NotNull;

/** State of one "hxcpp Profiler" configuration: only its user-visible name; the hxcpp sampler ticks at a fixed 1 ms. */
public final class HaxeHxcppProfilerConfigurationState extends HaxeProfilerConfigurationStateBase {

  public HaxeHxcppProfilerConfigurationState(@NotNull String displayName) {
    super(displayName);
  }

  @Override
  public @NotNull String getConfigurationTypeId() {
    return HaxeHxcppProfilerConfigurationType.ID;
  }
}
