package com.intellij.plugins.haxe.profiler.bridge.flash;

import com.intellij.plugins.haxe.profiler.bridge.HaxeProfilerConfigurationStateBase;
import org.jetbrains.annotations.NotNull;

/**
 * State of one "Flash Profiler" configuration: its user-visible name. The
 * data comes from the runtime's own telemetry channel, whose content is
 * governed by {@code ~/.telemetry.cfg} rather than per-profile options, and
 * the sampler's ~1 ms cadence is the runtime's fixed rate.
 */
public final class HaxeFlashProfilerConfigurationState extends HaxeProfilerConfigurationStateBase {

  public HaxeFlashProfilerConfigurationState(@NotNull String displayName) {
    super(displayName);
  }

  @Override
  public @NotNull String getConfigurationTypeId() {
    return HaxeFlashProfilerConfigurationType.ID;
  }
}
