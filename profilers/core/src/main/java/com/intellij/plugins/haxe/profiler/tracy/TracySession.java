package com.intellij.plugins.haxe.profiler.tracy;

import com.intellij.plugins.haxe.profiler.model.TimelineEvent;
import com.intellij.plugins.haxe.profiler.tracy.wire.TracyWelcome;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Everything decoded from one tracy connection, times in nanoseconds
 * relative to the session's first event. Zones are start-ordered. Plots are
 * keyed by their resolved name when a PlotName answer arrived (the live
 * query channel), else by a stable {@code plot@<pointer>} label.
 * {@code memoryCurves} are live-bytes-over-time series per allocation pool,
 * accumulated from the client's memory alloc/free events (hxcpp names its
 * pools "Small Object Heap" and "Large Object Heap"); keyed like plots —
 * resolved name or {@code pool@<pointer>}. {@code cpuUsage} carries the
 * client's periodic system-load reports (percent). {@code processCpu} is
 * the profiled process's own scheduler-exact CPU use folded from the
 * client's context-switch stream (percent of ONE core, so several busy
 * threads exceed 100); empty unless the process ran with the privileges
 * system tracing needs. Unmatched zone ends happen when the connection
 * started mid-zone; they are counted, not errors.
 */
public record TracySession(@NotNull TracyWelcome welcome,
                           @NotNull List<TracyZone> zones,
                           @NotNull List<Long> frameMarksNs,
                           @NotNull Map<String, List<PlotPoint>> plots,
                           @NotNull Map<String, List<PlotPoint>> memoryCurves,
                           @NotNull List<GcSweep> gcSweeps,
                           @NotNull List<TimelineEvent> events,
                           @NotNull List<PlotPoint> cpuUsage,
                           @NotNull List<PlotPoint> processCpu,
                           @NotNull Map<Integer, String> threadNames,
                           long durationNs,
                           int unmatchedZoneEnds) {

  public record PlotPoint(long timeNs, double value) {
  }

  /**
   * One collection's reclaim, derived from its free burst: hxcpp's tracy
   * integration emits small-object frees only from the collector's
   * after-mark sweep (large frees come from its large-object free path), so
   * a run of free events uninterrupted by an alloc is one collection. The
   * window covers the sweep, not the whole collection; bytes count only
   * frees whose alloc was captured.
   */
  public record GcSweep(long startNs, long endNs, long freedBytes, int freedObjects) {
  }
}
