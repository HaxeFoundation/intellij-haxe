package com.intellij.plugins.haxe.profiler.model;

/**
 * One heap reading, taken at a frame boundary by targets that report their
 * allocator state per frame (hxcpp telemetry, flash). Exact runtime
 * numbers, not estimates. {@code allocatedBytes}/{@code freedBytes} hold the
 * window's allocation traffic; no collector fills them yet, so they read 0
 * (reserved layout of the v1 session FRAME record).
 * TODO: a collector producing the allocation traffic (flash's alloc/free
 *  samples are the candidate)
 *
 * @param time seconds since the target's own capture epoch
 */
public record ProfilerMemorySample(double time, long usedBytes, long reservedBytes,
                                   long allocatedBytes, long freedBytes) {

  public ProfilerMemorySample(double time, long usedBytes, long reservedBytes) {
    this(time, usedBytes, reservedBytes, 0, 0);
  }
}
