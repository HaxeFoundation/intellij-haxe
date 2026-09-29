package com.intellij.plugins.haxe.profiler.bridge.chart;

import com.intellij.plugins.haxe.HaxeProfilerBundle;
import com.intellij.plugins.haxe.profiler.model.ProfilerEvent;
import com.intellij.plugins.haxe.profiler.model.ProfilerMemorySample;
import com.intellij.plugins.haxe.profiler.model.ProfilerSnapshot;
import com.intellij.plugins.haxe.profiler.model.ProfilerThread;
import com.intellij.plugins.haxe.profiler.model.PseudoFrames;
import com.intellij.plugins.haxe.profiler.model.StackFrame;
import com.intellij.plugins.haxe.profiler.model.StackSample;
import com.intellij.plugins.haxe.profiler.model.TimelineEvent;
import com.intellij.plugins.haxe.profiler.timeline.ProfilerTimeline;
import com.intellij.plugins.haxe.profiler.timeline.ProfilerTimeline.FlameNode;
import com.intellij.plugins.haxe.profiler.timeline.ProfilerTimeline.UsSpan;
import com.intellij.plugins.haxe.profiler.tracy.TracySession;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sampled captures: runs reconstructed from the samples, GC from sample flags
 * (per-frame GC events as fallback), frames from the frame events.
 */
record HaxeSnapshotChartData(@NotNull ProfilerSnapshot snapshot) implements HaxeChartData {

  @Override
  public List<ProfilerThread> threads() {
    return snapshot.threads();
  }

  /** Sampled captures are small: the whole tree loads regardless of the window. */
  @Override
  public FlameNode treeFor(int threadId, long fromUs, long toUs, long minDurationUs) {
    return ProfilerTimeline.flameTree(snapshot, threadId, MAX_DEPTH);
  }

  /**
   * Sample-flagged spans where the capture has them; telemetry captures carry
   * GC as per-frame time events instead, which are stop-the-world and so not
   * per-thread.
   */
  @Override
  public GcSpans gcSpansFor(int threadId) {
    List<UsSpan> flagged = ProfilerTimeline.gcSpans(snapshot, threadId);
    if (!flagged.isEmpty()) return new GcSpans(flagged, "haxe.profiler.callchart.span.gc", null);
    return new GcSpans(ProfilerTimeline.gcSpansFromEvents(snapshot), "haxe.profiler.callchart.span.gc.frame", null);
  }

  /** The thread's own frame spans, else those of the first thread with end-of-frame markers. */
  @Override
  public List<UsSpan> frameSpansFor(int threadId) {
    List<UsSpan> own = ProfilerTimeline.frameSpans(snapshot, threadId);
    if (!own.isEmpty()) return own;
    for (ProfilerThread thread : snapshot.threads()) {
      List<UsSpan> spans = ProfilerTimeline.frameSpans(snapshot, thread.id());
      if (!spans.isEmpty()) return spans;
    }
    return List.of();
  }

  @Override
  public Map<String, List<TracySession.PlotPoint>> curveSeries(CurveCategory category, int threadId) {
    return switch (category) {
      case CPU_LOAD -> activitySeries(threadId);
      case MEMORY -> memorySeries();
      case GPU_MEMORY, GPU_LOAD -> Map.of();
    };
  }

  /**
   * The per-frame heap readings some collectors report; captures without
   * them chart nothing. A series without a single non-zero reading (no
   * reservation figure, allocation tracking off) is left out rather than
   * charted as a flat zero.
   */
  private Map<String, List<TracySession.PlotPoint>> memorySeries() {
    if (snapshot.memory().isEmpty()) return Map.of();
    double start = ProfilerTimeline.captureStartSeconds(snapshot);
    List<TracySession.PlotPoint> used = new ArrayList<>();
    List<TracySession.PlotPoint> reserved = new ArrayList<>();
    List<TracySession.PlotPoint> allocated = new ArrayList<>();
    boolean anyReserved = false;
    boolean anyAllocated = false;
    for (ProfilerMemorySample sample : snapshot.memory()) {
      long timeNs = Math.round((sample.time() - start) * 1_000_000_000);
      used.add(new TracySession.PlotPoint(timeNs, sample.usedBytes()));
      reserved.add(new TracySession.PlotPoint(timeNs, sample.reservedBytes()));
      allocated.add(new TracySession.PlotPoint(timeNs, sample.allocatedBytes()));
      anyReserved |= sample.reservedBytes() > 0;
      anyAllocated |= sample.allocatedBytes() > 0;
    }

    Map<String, List<TracySession.PlotPoint>> series = new LinkedHashMap<>();
    series.put(HaxeProfilerBundle.message("haxe.profiler.callchart.series.heap.used"), used);
    if (anyReserved) {
      series.put(HaxeProfilerBundle.message("haxe.profiler.callchart.series.heap.reserved"), reserved);
    }
    if (anyAllocated) {
      series.put(HaxeProfilerBundle.message("haxe.profiler.callchart.series.allocated"), allocated);
    }
    return series;
  }

  /**
   * Busy share per bucket: the fraction of wall time covered by samples
   * whose root is application code; idle roots and uncovered gaps count as
   * idle. This is the sampled stand-in for a CPU curve; a real process-CPU
   * figure needs scheduler data no sampler has.
   */
  private Map<String, List<TracySession.PlotPoint>> activitySeries(int threadId) {
    double start = ProfilerTimeline.captureStartSeconds(snapshot);
    double tickUs = tickUs();
    long durationUs = 0;
    for (StackSample sample : snapshot.samples()) {
      durationUs = Math.max(durationUs, Math.round((sample.time() - start) * 1_000_000));
    }
    if (durationUs <= 0) return Map.of();

    long bucketUs = Math.max(durationUs / ACTIVITY_BUCKETS, 1_000);
    long[] busy = new long[(int)(durationUs / bucketUs) + 1];
    boolean any = false;
    for (StackSample sample : snapshot.samples()) {
      if (sample.threadId() != threadId || PseudoFrames.isIdle(sample.frames().getFirst().symbol())) continue;
      any = true;
      // the weight covers the time BEFORE the sample's tick
      long endUs = Math.round((sample.time() - start) * 1_000_000);
      long startUs = Math.max(endUs - Math.round(sample.weight() * tickUs), 0);
      HaxeChartData.addBusy(busy, bucketUs, startUs, endUs);
    }
    if (!any) return Map.of();

    List<TracySession.PlotPoint> points = HaxeChartData.busyPercent(busy, bucketUs, 1000);
    return Map.of(HaxeProfilerBundle.message("haxe.profiler.callchart.series.thread.activity"), points);
  }

  /**
   * Sample time inside the span, merged by {@link #sliceNameOf}. Each sample
   * covers the interval its weight spans, ending at its tick, clipped to the
   * frame: a coalesced idle stretch then spreads over every frame it crosses
   * instead of landing whole on the frame holding its endpoint. The shares
   * have sampling resolution; exact bounds are unknowable from samples.
   */
  @Override
  public List<FrameSlice> frameBreakdown(int threadId, UsSpan frame) {
    double start = ProfilerTimeline.captureStartSeconds(snapshot);
    double tickUs = tickUs();
    Map<String, long[]> bySlice = new HashMap<>();
    for (StackSample sample : snapshot.samples()) {
      if (sample.threadId() != threadId) continue;
      long endUs = Math.round((sample.time() - start) * 1_000_000);
      long startUs = endUs - Math.round(sample.weight() * tickUs);
      long overlap = Math.min(endUs, frame.endUs()) - Math.max(startUs, frame.startUs());
      if (overlap <= 0) continue;
      bySlice.computeIfAbsent(sliceNameOf(sample), key -> new long[1])[0] += overlap;
    }

    List<FrameSlice> slices = new ArrayList<>();
    bySlice.forEach((name, us) -> slices.add(new FrameSlice(name, us[0])));
    slices.sort(Comparator.comparingLong(FrameSlice::totalUs).reversed());
    return slices;
  }

  private double tickUs() {
    return snapshot.samplesPerSecond() > 0 ? 1_000_000.0 / snapshot.samplesPerSecond() : 1;
  }

  /**
   * The name a sample's share files under: its GC frame when it ran inside
   * the collector (hxcpp's GC entry points sit mid-stack under the
   * allocation site), else its leaf, the function executing when the tick
   * landed. Leaf attribution names the frame's hot methods even when
   * everything nests under one main loop. Pseudo-frames are single-frame
   * stacks, so their leaf is the root.
   */
  private static String sliceNameOf(StackSample sample) {
    for (StackFrame frame : sample.frames()) {
      if (PseudoFrames.isGc(frame.symbol())) return frame.symbol();
    }
    if (sample.inGc()) return PseudoFrames.GC;
    return sample.frames().getLast().symbol();
  }

  /** The application's own marks; the reserved frame and GC-time codes are lanes of their own. */
  @Override
  public List<TimelineEvent> events() {
    double start = ProfilerTimeline.captureStartSeconds(snapshot);
    List<TimelineEvent> marks = new ArrayList<>();
    for (ProfilerEvent event : snapshot.events()) {
      if (event.code() == ProfilerEvent.FRAME_CODE || event.code() == ProfilerEvent.GC_TIME_CODE) continue;
      String text = event.data().isEmpty()
                    ? HaxeProfilerBundle.message("haxe.profiler.callchart.event.code", event.code())
                    : event.data();
      long timeNs = Math.round((event.time() - start) * 1_000_000_000);
      marks.add(new TimelineEvent(event.threadId(), timeNs, text, 0));
    }
    return marks;
  }
}
