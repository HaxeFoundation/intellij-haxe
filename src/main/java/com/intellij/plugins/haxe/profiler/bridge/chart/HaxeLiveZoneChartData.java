package com.intellij.plugins.haxe.profiler.bridge.chart;

import com.intellij.plugins.haxe.profiler.hxt.HxtZoneStore;
import com.intellij.plugins.haxe.profiler.model.ProfilerThread;
import com.intellij.plugins.haxe.profiler.model.TimelineEvent;
import com.intellij.plugins.haxe.profiler.timeline.ProfilerTimeline.FlameNode;
import com.intellij.plugins.haxe.profiler.timeline.ProfilerTimeline.UsSpan;
import com.intellij.plugins.haxe.profiler.tracy.TracySession;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A still-growing zone capture: everything the store serves, except the
 * thread activity curve, which comes from an incremental fold because the
 * store's whole-file scan is too costly per live refresh.
 */
record HaxeLiveZoneChartData(@NotNull HaxeZoneChartData zones, @NotNull Activity activity) implements HaxeChartData {

  @Override
  public List<ProfilerThread> threads() {
    return zones.threads();
  }

  @Override
  public FlameNode treeFor(int threadId, long fromUs, long toUs, long minDurationUs) {
    return zones.treeFor(threadId, fromUs, toUs, minDurationUs);
  }

  @Override
  public boolean windowedLoads() {
    return true;
  }

  @Override
  public long durationUs() {
    return zones.durationUs();
  }

  @Override
  public List<UsSpan> coarseActivity(int threadId) {
    return zones.coarseActivity(threadId);
  }

  @Override
  public List<TimelineEvent> events() {
    return zones.events();
  }

  @Override
  public GcSpans gcSpansFor(int threadId) {
    return zones.gcSpansFor(threadId);
  }

  @Override
  public List<UsSpan> frameSpansFor(int threadId) {
    return zones.frameSpansFor(threadId);
  }

  @Override
  public Map<String, List<TracySession.PlotPoint>> curveSeries(CurveCategory category, int threadId) {
    return category == CurveCategory.CPU_LOAD
           ? zones.cpuSeriesWith(activity.series(threadId))
           : zones.curveSeries(category, threadId);
  }

  @Override
  public List<FrameSlice> frameBreakdown(int threadId, UsSpan frame) {
    return zones.frameBreakdown(threadId, frame);
  }

  @Override
  public String runCountText(int count) {
    return zones.runCountText(count);
  }

  @Override
  public List<HaxeCallChartPanel.SearchMatch> searchMatches(int threadId, String query, int limit) {
    return zones.searchMatches(threadId, query, limit);
  }

  /** Per-thread busy buckets grown only from chunks not folded before, so a refresh's cost stays flat. */
  static final class Activity {
    private static final long BUCKET_NS = 50_000_000;

    private final Map<Integer, long[]> busyByThread = new HashMap<>();
    private int foldedChunks;

    /** Top-level zones are the busy time; their children nest inside them. */
    void foldNewChunks(HxtZoneStore store) throws IOException {
      if (store.chunkCount() <= foldedChunks) return;
      store.scanZonesFromChunk(foldedChunks, (thread, depth, startNs, endNs, location) -> {
        if (depth == 0) {
          HaxeChartData.addBusy(bucketsFor(thread, endNs), BUCKET_NS, startNs, endNs);
        }
      });
      foldedChunks = store.chunkCount();
    }

    private long[] bucketsFor(int thread, long endNs) {
      long[] busy = busyByThread.computeIfAbsent(thread, key -> new long[64]);
      int needed = (int)(endNs / BUCKET_NS) + 1;
      if (needed > busy.length) {
        busy = Arrays.copyOf(busy, Math.max(needed, busy.length + busy.length / 2));
        busyByThread.put(thread, busy);
      }
      return busy;
    }

    List<TracySession.PlotPoint> series(int threadId) {
      long[] busy = busyByThread.get(threadId);
      return busy == null ? List.of() : HaxeChartData.busyPercent(busy, BUCKET_NS, 1);
    }
  }
}
