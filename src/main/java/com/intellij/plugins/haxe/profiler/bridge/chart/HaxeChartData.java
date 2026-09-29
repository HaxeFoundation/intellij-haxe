package com.intellij.plugins.haxe.profiler.bridge.chart;

import com.intellij.plugins.haxe.HaxeProfilerBundle;
import com.intellij.plugins.haxe.profiler.model.ProfilerThread;
import com.intellij.plugins.haxe.profiler.model.StackFrame;
import com.intellij.plugins.haxe.profiler.model.TimelineEvent;
import com.intellij.plugins.haxe.profiler.timeline.ProfilerTimeline.FlameNode;
import com.intellij.plugins.haxe.profiler.timeline.ProfilerTimeline.UsSpan;
import com.intellij.plugins.haxe.profiler.tracy.TracySession;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Image;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/** The per-thread inputs of the Call Chart that one capture kind provides. */
interface HaxeChartData {

  /** Deeper frames fold into their ancestor; the flamegraph tab still shows full depth. */
  int MAX_DEPTH = 64;
  /** Buckets of a derived thread-activity series across the session. */
  int ACTIVITY_BUCKETS = 512;

  /** The curve-lane categories a capture may fill, in display order. */
  enum CurveCategory {
    MEMORY("haxe.profiler.callchart.lane.memory", HaxeCallChartPanel.CurveUnit.BYTES),
    GPU_MEMORY("haxe.profiler.callchart.lane.gpu.memory", HaxeCallChartPanel.CurveUnit.BYTES),
    CPU_LOAD("haxe.profiler.callchart.lane.cpu", HaxeCallChartPanel.CurveUnit.PERCENT),
    GPU_LOAD("haxe.profiler.callchart.lane.gpu.load", HaxeCallChartPanel.CurveUnit.PERCENT);

    final String labelKey;
    final HaxeCallChartPanel.CurveUnit unit;

    CurveCategory(String labelKey, HaxeCallChartPanel.CurveUnit unit) {
      this.labelKey = labelKey;
      this.unit = unit;
    }
  }

  /** One entry of a frame's time breakdown. */
  record FrameSlice(@NotNull String name, long totalUs) {
  }

  /** A GC lane's spans plus how the detail view describes ONE of them. */
  record GcSpans(@NotNull List<UsSpan> spans, @NotNull String spanKindKey, @Nullable Function<UsSpan, String> spanInfo) {
  }

  List<ProfilerThread> threads();

  /**
   * The thread's tree for [fromUs, toUs] with zones under
   * {@code minDurationUs} dropped; sources without windowed loading ignore
   * the bounds and return the whole capture. The returned root always spans
   * the whole session, so the chart's axis stays put.
   */
  FlameNode treeFor(int threadId, long fromUs, long toUs, long minDurationUs);

  /** True when {@link #treeFor} serves real windows and zooming should reload. */
  default boolean windowedLoads() {
    return false;
  }

  /** The capture length; only meaningful for windowed sources. */
  default long durationUs() {
    return 0;
  }

  /** Coarse top-level activity for the minimap when the capture has no frames. */
  default List<UsSpan> coarseActivity(int threadId) {
    return List.of();
  }

  /** Instant marks for the events row; any lane with a user-event API can supply them. */
  default List<TimelineEvent> events() {
    return List.of();
  }

  GcSpans gcSpansFor(int threadId);

  List<UsSpan> frameSpansFor(int threadId);

  /** The category's value-over-time series by name; categories a capture cannot fill honestly stay empty. */
  default Map<String, List<TracySession.PlotPoint>> curveSeries(CurveCategory category, int threadId) {
    return Map.of();
  }

  /**
   * A selected frame's time breakdown: top-level work inside the span merged
   * by function, exact per the capture's acquisition, and empty when the
   * capture cannot answer without guesswork. May read the capture file; call
   * off the EDT.
   */
  default List<FrameSlice> frameBreakdown(int threadId, UsSpan frame) {
    return List.of();
  }

  /** A small screenshot of the frame for the details panel; null until a lane can supply one. */
  default @Nullable Image frameImage(UsSpan frame) {
    return null;
  }

  /** What a run's count means in this capture: samples for sampled data, invocations for exact zones. */
  default String runCountText(int count) {
    return HaxeProfilerBundle.message("haxe.profiler.callchart.samples", count);
  }

  /**
   * Runs whose symbol contains {@code query} (case-insensitive, anywhere in
   * the qualified name, so a bare method name and a package prefix both
   * match), time-ordered, at most {@code limit}. May read the capture file;
   * call off the EDT.
   */
  default List<HaxeCallChartPanel.SearchMatch> searchMatches(int threadId, String query, int limit) {
    List<HaxeCallChartPanel.SearchMatch> matches = new ArrayList<>();
    collectMatches(treeFor(threadId, 0, Long.MAX_VALUE, 0), 0, query.toLowerCase(Locale.ROOT), limit, matches);
    matches.sort(Comparator.comparingLong(HaxeCallChartPanel.SearchMatch::startUs));
    return matches;
  }

  /** Depth-first over an in-memory tree, non-idle runs only. */
  private static void collectMatches(FlameNode node, int depth, String needle, int limit,
                                     List<HaxeCallChartPanel.SearchMatch> matches) {
    for (FlameNode child : node.children()) {
      if (matches.size() >= limit) return;
      StackFrame frame = child.frame();
      if (!child.idle() && frame != null && frame.symbol().toLowerCase(Locale.ROOT).contains(needle)) {
        matches.add(new HaxeCallChartPanel.SearchMatch(child.startUs(), child.endUs(), depth, frame.symbol()));
      }
      collectMatches(child, depth + 1, needle, limit, matches);
    }
  }

  /** Adds the part of [start, end) falling into each bucket of width {@code bucketSize} to {@code busy}. */
  static void addBusy(long[] busy, long bucketSize, long start, long end) {
    int first = (int)Math.max(start / bucketSize, 0);
    int last = (int)Math.min(end / bucketSize, busy.length - 1);
    for (int bucket = first; bucket <= last; bucket++) {
      long bucketStart = bucket * bucketSize;
      long overlap = Math.min(end, bucketStart + bucketSize) - Math.max(start, bucketStart);
      if (overlap > 0) busy[bucket] += overlap;
    }
  }

  /** The buckets as a 0..100 busy-percent curve; {@code nsPerUnit} converts a bucket start to the curve's nanoseconds. */
  static List<TracySession.PlotPoint> busyPercent(long[] busy, long bucketSize, long nsPerUnit) {
    List<TracySession.PlotPoint> points = new ArrayList<>(busy.length);
    for (int bucket = 0; bucket < busy.length; bucket++) {
      double percent = Math.min(busy[bucket] * 100.0 / bucketSize, 100.0);
      points.add(new TracySession.PlotPoint(bucket * bucketSize * nsPerUnit, percent));
    }
    return points;
  }
}
