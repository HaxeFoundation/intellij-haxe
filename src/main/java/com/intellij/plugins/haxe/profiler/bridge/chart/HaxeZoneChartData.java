package com.intellij.plugins.haxe.profiler.bridge.chart;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.plugins.haxe.HaxeProfilerBundle;
import com.intellij.plugins.haxe.profiler.bridge.HaxeProfilerFormats;
import com.intellij.plugins.haxe.profiler.hxt.HxtZoneStore;
import com.intellij.plugins.haxe.profiler.model.ProfilerThread;
import com.intellij.plugins.haxe.profiler.model.TimelineEvent;
import com.intellij.plugins.haxe.profiler.timeline.ProfilerTimeline.FlameNode;
import com.intellij.plugins.haxe.profiler.timeline.ProfilerTimeline.UsSpan;
import com.intellij.plugins.haxe.profiler.tracy.TracySession;
import com.intellij.plugins.haxe.profiler.tracy.TracyZone;
import com.intellij.plugins.haxe.profiler.tracy.TracyZoneTrees;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Tracy captures: exact zone trees served from the store, GC from the
 * collector's free bursts, frames from the frame marks.
 */
record HaxeZoneChartData(@NotNull HxtZoneStore store) implements HaxeChartData {

  private static final Logger LOG = Logger.getInstance(HaxeZoneChartData.class);

  @Override
  public List<ProfilerThread> threads() {
    return store.threads().stream()
      .map(entry -> new ProfilerThread(entry.id(), entry.name()))
      .toList();
  }

  @Override
  public FlameNode treeFor(int threadId, long fromUs, long toUs, long minDurationUs) {
    long toNs = toUs >= Long.MAX_VALUE / 2000 ? Long.MAX_VALUE : toUs * 1000;
    List<TracyZone> ordered = new ArrayList<>();
    boolean read = scan(threadId, fromUs * 1000, toNs, minDurationUs * 1000,
                        (thread, depth, startNs, endNs, location) ->
                          ordered.add(new TracyZone(thread, startNs, endNs, location)));
    if (!read) {
      ordered.clear();
    }
    ordered.sort(Comparator.comparingLong(TracyZone::startNs));
    return TracyZoneTrees.treeFromOrdered(ordered, store.session().durationNs());
  }

  @Override
  public boolean windowedLoads() {
    return true;
  }

  @Override
  public long durationUs() {
    return store.session().durationNs() / 1000;
  }

  /** Top-level zones at minimap resolution: the strip's silhouette. */
  @Override
  public List<UsSpan> coarseActivity(int threadId) {
    long floorNs = store.session().durationNs() / 2000;
    List<UsSpan> spans = new ArrayList<>();
    boolean read = scan(threadId, 0, Long.MAX_VALUE, floorNs, (thread, depth, startNs, endNs, location) -> {
      if (depth == 0) {
        long startUs = startNs / 1000;
        spans.add(new UsSpan(startUs, Math.max(endNs / 1000, startUs + 1)));
      }
    });
    if (!read) return List.of();
    spans.sort(Comparator.comparingLong(UsSpan::startUs));
    return spans;
  }

  /** The runtime emits no GC zones; the collector's free bursts stand in and carry the reclaim numbers. */
  @Override
  public GcSpans gcSpansFor(int threadId) {
    Map<UsSpan, String> reclaimBySpan = new HashMap<>();
    List<UsSpan> spans = new ArrayList<>();
    for (TracySession.GcSweep sweep : store.session().gcSweeps()) {
      long startUs = sweep.startNs() / 1000;
      UsSpan span = new UsSpan(startUs, Math.max(sweep.endNs() / 1000, startUs + 1));
      spans.add(span);
      String freed = HaxeProfilerFormats.formatBytes(sweep.freedBytes());
      String reclaim = HaxeProfilerBundle.message("haxe.profiler.callchart.gc.freed", freed, sweep.freedObjects());
      reclaimBySpan.put(span, reclaim);
    }
    return new GcSpans(spans, "haxe.profiler.callchart.span.gc.sweep", reclaimBySpan::get);
  }

  @Override
  public List<UsSpan> frameSpansFor(int threadId) {
    return TracyZoneTrees.frameSpans(store.session());
  }

  @Override
  public Map<String, List<TracySession.PlotPoint>> curveSeries(CurveCategory category, int threadId) {
    return switch (category) {
      case MEMORY -> new TreeMap<>(store.session().memoryCurves());
      case CPU_LOAD -> cpuSeriesWith(threadActivity(threadId));
      case GPU_MEMORY -> gpuPlots(true);
      case GPU_LOAD -> gpuPlots(false);
    };
  }

  /**
   * The CPU lane's percent series around the given thread activity,
   * narrowest scope first: the charted thread's activity (the share of wall
   * time inside instrumented code), the process's scheduler CPU use
   * (percent of one core, present only when the run was elevated for system
   * tracing) and the whole system's usage across all cores and processes.
   */
  Map<String, List<TracySession.PlotPoint>> cpuSeriesWith(List<TracySession.PlotPoint> activity) {
    TracySession session = store.session();
    Map<String, List<TracySession.PlotPoint>> series = new LinkedHashMap<>();
    if (!activity.isEmpty()) {
      series.put(HaxeProfilerBundle.message("haxe.profiler.callchart.series.thread.activity"), activity);
    }
    if (!session.processCpu().isEmpty()) {
      series.put(HaxeProfilerBundle.message("haxe.profiler.callchart.series.process.cpu"), session.processCpu());
    }
    if (!session.cpuUsage().isEmpty()) {
      series.put(HaxeProfilerBundle.message("haxe.profiler.callchart.series.system.cpu"), session.cpuUsage());
    }
    return series;
  }

  /** Busy percent per bucket from the top-level zones. */
  private List<TracySession.PlotPoint> threadActivity(int threadId) {
    long durationNs = store.session().durationNs();
    if (durationNs <= 0) return List.of();

    long bucketNs = Math.max(durationNs / ACTIVITY_BUCKETS, 1_000_000);
    long[] busy = new long[(int)(durationNs / bucketNs) + 1];
    boolean read = scan(threadId, 0, Long.MAX_VALUE, bucketNs / 100, (thread, depth, startNs, endNs, location) -> {
      if (depth == 0) {
        HaxeChartData.addBusy(busy, bucketNs, startNs, endNs);
      }
    });
    if (!read) return List.of();
    return HaxeChartData.busyPercent(busy, bucketNs, 1);
  }

  /**
   * GPU counters arrive as tracy plots. A plot named "gpu*" charts on a GPU
   * lane: one whose name contains "mem" as bytes on the memory lane, any
   * other as percent on the load lane. Other plots stay uncharted.
   */
  private Map<String, List<TracySession.PlotPoint>> gpuPlots(boolean memory) {
    Map<String, List<TracySession.PlotPoint>> series = new TreeMap<>();
    for (Map.Entry<String, List<TracySession.PlotPoint>> plot : store.session().plots().entrySet()) {
      String name = plot.getKey().toLowerCase(Locale.ROOT);
      if (name.startsWith("gpu") && name.contains("mem") == memory) {
        series.put(plot.getKey(), plot.getValue());
      }
    }
    return series;
  }

  /** Top-level zone time inside the span, exact and clipped to it; the zone names say what each share is. */
  @Override
  public List<FrameSlice> frameBreakdown(int threadId, UsSpan frame) {
    long fromNs = frame.startUs() * 1000;
    long toNs = frame.endUs() * 1000;
    Map<String, long[]> byFunction = new HashMap<>();
    boolean read = scan(threadId, fromNs, toNs, 0, (thread, depth, startNs, endNs, location) -> {
      long clippedNs = Math.min(endNs, toNs) - Math.max(startNs, fromNs);
      if (depth == 0 && clippedNs > 0) {
        byFunction.computeIfAbsent(location.function(), key -> new long[1])[0] += clippedNs;
      }
    });
    if (!read) return List.of();

    List<FrameSlice> slices = new ArrayList<>();
    byFunction.forEach((function, ns) -> slices.add(new FrameSlice(function, ns[0] / 1000)));
    slices.sort(Comparator.comparingLong(FrameSlice::totalUs).reversed());
    return slices;
  }

  @Override
  public List<TimelineEvent> events() {
    return store.session().events();
  }

  /** Zones are measured, not sampled: a run's count is its invocations. */
  @Override
  public String runCountText(int count) {
    return HaxeProfilerBundle.message("haxe.profiler.callchart.invocations", count);
  }

  /** The loaded tree is windowed and duration-floored, so a session-wide search scans the store instead. */
  @Override
  public List<HaxeCallChartPanel.SearchMatch> searchMatches(int threadId, String query, int limit) {
    String needle = query.toLowerCase(Locale.ROOT);
    Map<String, Boolean> verdicts = new HashMap<>();
    List<HaxeCallChartPanel.SearchMatch> matches = new ArrayList<>();
    scan(threadId, 0, Long.MAX_VALUE, 0, (thread, depth, startNs, endNs, location) -> {
      if (matches.size() >= limit) return;
      boolean hit = verdicts.computeIfAbsent(location.function(),
                                             function -> function.toLowerCase(Locale.ROOT).contains(needle));
      if (hit) {
        matches.add(new HaxeCallChartPanel.SearchMatch(startNs / 1000, endNs / 1000, depth, location.function()));
      }
    });
    matches.sort(Comparator.comparingLong(HaxeCallChartPanel.SearchMatch::startUs));
    return matches;
  }

  /** The store's zone scan; false when the file could not be read, whatever the consumer collected is then partial. */
  private boolean scan(int threadId, long fromNs, long toNs, long minDurationNs, HxtZoneStore.ZoneConsumer consumer) {
    try {
      store.scanZones(threadId, fromNs, toNs, minDurationNs, consumer);
      return true;
    }
    catch (IOException e) {
      LOG.warn("could not read the zone capture", e);
      return false;
    }
  }
}
