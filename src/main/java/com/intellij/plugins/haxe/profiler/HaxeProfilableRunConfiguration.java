package com.intellij.plugins.haxe.profiler;

import org.jetbrains.annotations.NotNull;

/**
 * A run configuration a Haxe profiler executor can launch. The gate is
 * consulted from the profiler button's update loop and must answer cheaply:
 * false while the project is indexing (Run/Debug gray out then too) and while
 * the configuration is STALE — a tool-window target switch leaves the previous
 * target's configuration behind, and profiling it would rerun the previous
 * target's artifact. The lane pairs the configuration with its profiler
 * entry — a HashLink configuration must not light up the hxcpp one.
 */
public interface HaxeProfilableRunConfiguration {

  enum Lane {
    HASHLINK,
    HXCPP,
    FLASH,
    JS
  }

  @NotNull
  Lane profilingLane();

  boolean isProfilingReady();
}
