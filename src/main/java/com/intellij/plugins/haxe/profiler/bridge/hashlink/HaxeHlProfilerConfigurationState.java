package com.intellij.plugins.haxe.profiler.bridge.hashlink;

import com.intellij.plugins.haxe.profiler.bridge.HaxeProfilerConfigurationStateBase;
import org.jetbrains.annotations.NotNull;

/** State of one "HashLink Profiler" configuration: its user-visible name and the sampling rate. */
public final class HaxeHlProfilerConfigurationState extends HaxeProfilerConfigurationStateBase {

  /** 10000/s gives ~0.1 ms precision. */
  public static final int DEFAULT_SAMPLES_PER_SECOND = 10000;

  private int samplesPerSecond;

  public HaxeHlProfilerConfigurationState(@NotNull String displayName, int samplesPerSecond) {
    super(displayName);
    this.samplesPerSecond = samplesPerSecond;
  }

  @Override
  public @NotNull String getConfigurationTypeId() {
    return HaxeHlProfilerConfigurationType.ID;
  }

  public int getSamplesPerSecond() {
    return samplesPerSecond;
  }

  /** A non-positive rate falls back to the default. */
  public void setSamplesPerSecond(int samplesPerSecond) {
    this.samplesPerSecond = samplesPerSecond > 0 ? samplesPerSecond : DEFAULT_SAMPLES_PER_SECOND;
  }
}
