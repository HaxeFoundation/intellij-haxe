package com.intellij.plugins.haxe.profiler.bridge.js;

import com.intellij.plugins.haxe.profiler.bridge.HaxeProfilerConfigurationStateBase;
import org.jetbrains.annotations.NotNull;

/**
 * State of one "JavaScript Profiler" configuration: its user-visible name
 * and V8's sampling interval in microseconds (CDP
 * {@code Profiler.setSamplingInterval}; 1000 is V8's own default).
 */
public final class HaxeJsProfilerConfigurationState extends HaxeProfilerConfigurationStateBase {

  static final int DEFAULT_SAMPLING_INTERVAL_US = 1000;
  private static final int MIN_SAMPLING_INTERVAL_US = 100;
  private static final int MAX_SAMPLING_INTERVAL_US = 1_000_000;

  private int samplingIntervalUs = DEFAULT_SAMPLING_INTERVAL_US;

  public HaxeJsProfilerConfigurationState(@NotNull String displayName) {
    super(displayName);
  }

  @Override
  public @NotNull String getConfigurationTypeId() {
    return HaxeJsProfilerConfigurationType.ID;
  }

  public int getSamplingIntervalUs() {
    return samplingIntervalUs;
  }

  public void setSamplingIntervalUs(int intervalUs) {
    samplingIntervalUs = Math.clamp(intervalUs, MIN_SAMPLING_INTERVAL_US, MAX_SAMPLING_INTERVAL_US);
  }
}
