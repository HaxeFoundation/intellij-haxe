package com.intellij.plugins.haxe.profiler.bridge;

import com.intellij.profiler.api.configurations.ProfilerConfigurationState;
import org.jetbrains.annotations.NotNull;

/** The user-visible name every Haxe profiler configuration state carries. */
public abstract class HaxeProfilerConfigurationStateBase implements ProfilerConfigurationState {

  private String displayName;

  protected HaxeProfilerConfigurationStateBase(@NotNull String displayName) {
    this.displayName = displayName;
  }

  @Override
  public final @NotNull String getDisplayName() {
    return displayName;
  }

  @Override
  public final void setDisplayName(@NotNull String name) {
    displayName = name;
  }
}
