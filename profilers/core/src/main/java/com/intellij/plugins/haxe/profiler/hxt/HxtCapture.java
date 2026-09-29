package com.intellij.plugins.haxe.profiler.hxt;

import com.intellij.plugins.haxe.profiler.model.ProfilerSnapshot;
import org.jetbrains.annotations.NotNull;

/**
 * What an HXTS session file holds, by container version: v1 files carry a
 * sampled capture (the telemetry lane), v3 files an exact zone capture (the
 * tracy lane) served from disk through its store. One extension, one
 * container, two data shapes.
 */
public sealed interface HxtCapture {

  record Samples(@NotNull ProfilerSnapshot snapshot) implements HxtCapture {
  }

  record Zones(@NotNull HxtZoneStore store) implements HxtCapture {
  }
}
