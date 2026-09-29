package com.intellij.plugins.haxe.profiler.model;

/**
 * One heap reading, taken at a frame boundary by targets that report their
 * allocator state per frame (hxcpp telemetry, flash). Exact runtime
 * numbers, not estimates. {@code allocatedBytes}/{@code freedBytes} are the
 * window's allocation traffic when the collector tracks it (flash's
 * allocation samples); 0 elsewhere.
 *
 * @param time seconds since the target's own capture epoch
 */
public record ProfilerMemorySample(double time, long usedBytes, long reservedBytes,
                                   long allocatedBytes, long freedBytes) {

  public ProfilerMemorySample(double time, long usedBytes, long reservedBytes) {
    this(time, usedBytes, reservedBytes, 0, 0);
  }
}
