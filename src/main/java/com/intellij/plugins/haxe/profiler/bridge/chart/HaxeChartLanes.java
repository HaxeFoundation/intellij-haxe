package com.intellij.plugins.haxe.profiler.bridge.chart;

import com.intellij.plugins.haxe.HaxeProfilerBundle;
import com.intellij.plugins.haxe.profiler.bridge.chart.HaxeCallChartPanel.MarkerLane;
import com.intellij.plugins.haxe.profiler.bridge.chart.HaxeChartData.CurveCategory;
import com.intellij.plugins.haxe.profiler.bridge.chart.HaxeChartData.GcSpans;
import com.intellij.plugins.haxe.profiler.model.TimelineEvent;
import com.intellij.plugins.haxe.profiler.timeline.ProfilerTimeline.UsSpan;
import com.intellij.plugins.haxe.profiler.tracy.TracySession;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.awt.Image;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * The charted thread's lane data plus which lanes the user chose to see.
 * Every lane stays toggleable; the first data load decides the defaults (a
 * lane without data starts hidden), and the user's choices survive thread
 * switches.
 */
final class HaxeChartLanes {

  static final Color GC_LANE = new JBColor(0xD66A6A, 0xA14A4A);
  private static final Color FRAME_LANE_EVEN = new JBColor(0x9CB8D6, 0x53687D);
  private static final Color FRAME_LANE_ODD = new JBColor(0xC4D4E4, 0x3E4E5E);
  private static final Color[] MEMORY_LANE_COLORS = {
    new JBColor(0x7FB58A, 0x4E7157),
    new JBColor(0x8AA6C8, 0x50647C)};
  private static final Color CPU_LANE = new JBColor(0xC79A50, 0x8A6D3F);
  private static final Color GPU_MEMORY_LANE = new JBColor(0x5FA3A3, 0x487878);
  private static final Color GPU_LOAD_LANE = new JBColor(0x9B7FBF, 0x6A5688);

  /** Every lane's visibility; {@code calls} is the call chart itself. */
  record Visibility(boolean frames, boolean gc, boolean events, boolean calls,
                    @NotNull Map<CurveCategory, Boolean> curves) {

    /** Keyed the way {@link HaxeChartViewSettings} persists it. */
    Map<String, Boolean> toMap() {
      Map<String, Boolean> map = new HashMap<>();
      map.put("frames", frames);
      map.put("gc", gc);
      map.put("events", events);
      map.put("calls", calls);
      for (CurveCategory category : CurveCategory.values()) {
        map.put(curveKey(category), curves.getOrDefault(category, false));
      }
      return map;
    }

    /** The inverse of {@link #toMap}; a missing key reads as hidden. */
    static Visibility fromMap(Map<String, Boolean> map) {
      Map<CurveCategory, Boolean> curves = new EnumMap<>(CurveCategory.class);
      for (CurveCategory category : CurveCategory.values()) {
        curves.put(category, map.getOrDefault(curveKey(category), false));
      }
      return new Visibility(map.getOrDefault("frames", false), map.getOrDefault("gc", false),
                            map.getOrDefault("events", false), map.getOrDefault("calls", false), curves);
    }

    private static String curveKey(CurveCategory category) {
      return "curve:" + category.name();
    }
  }

  /** One thread's lane inputs, computable off the EDT (several linear scans over the capture). */
  record ThreadData(@NotNull GcSpans gc, @NotNull List<UsSpan> frameSpans, @NotNull List<TimelineEvent> events,
                    @NotNull Map<CurveCategory, Map<String, List<TracySession.PlotPoint>>> curves) {

    static ThreadData compute(HaxeChartData data, int threadId) {
      Map<CurveCategory, Map<String, List<TracySession.PlotPoint>>> curves = new EnumMap<>(CurveCategory.class);
      for (CurveCategory category : CurveCategory.values()) {
        curves.put(category, data.curveSeries(category, threadId));
      }
      return new ThreadData(data.gcSpansFor(threadId), data.frameSpansFor(threadId), data.events(), curves);
    }

    private static ThreadData empty() {
      Map<CurveCategory, Map<String, List<TracySession.PlotPoint>>> curves = new EnumMap<>(CurveCategory.class);
      for (CurveCategory category : CurveCategory.values()) {
        curves.put(category, Map.of());
      }
      GcSpans gc = new GcSpans(List.of(), "haxe.profiler.callchart.span.gc", null);
      return new ThreadData(gc, List.of(), List.of(), curves);
    }
  }

  private ThreadData thread = ThreadData.empty();
  private Visibility shown = Visibility.fromMap(Map.of("calls", true));
  private boolean defaultsApplied;
  /** Loads a selected frame's breakdown rows; runs on a pooled thread. */
  private @Nullable Function<UsSpan, HaxeFrameBreakdownView.Rows> frameBreakdown;
  /** Loads a selected frame's screenshot; runs on a pooled thread. */
  private @Nullable Function<UsSpan, Image> frameImage;

  /** Pulls one thread's lane inputs out of the data source. */
  void load(HaxeChartData data, int threadId) {
    assign(ThreadData.compute(data, threadId), data, threadId);
  }

  /** Takes over lane inputs computed off the EDT. */
  void assign(ThreadData computed, HaxeChartData data, int threadId) {
    thread = computed;
    List<UsSpan> gcSpans = computed.gc().spans();
    frameBreakdown = span -> HaxeFrameBreakdown.rows(data.frameBreakdown(threadId, span), span, gcSpans);
    frameImage = data::frameImage;
  }

  List<UsSpan> frameSpans() {
    return thread.frameSpans();
  }

  @Nullable Function<UsSpan, Image> frameImage() {
    return frameImage;
  }

  Visibility shown() {
    return shown;
  }

  void show(Visibility chosen) {
    shown = chosen;
  }

  /** The data-driven defaults: a lane with data shown, one without hidden; the call chart always shown. */
  Visibility dataDefaults() {
    Map<CurveCategory, Boolean> curves = new EnumMap<>(CurveCategory.class);
    for (CurveCategory category : CurveCategory.values()) {
      curves.put(category, !thread.curves().get(category).isEmpty());
    }
    return new Visibility(!thread.frameSpans().isEmpty(), !thread.gc().spans().isEmpty(),
                          !thread.events().isEmpty(), true, curves);
  }

  /** On the first data load: the user's saved choice where one exists, else the data-driven default. */
  void applyDefaults(HaxeChartViewSettings settings) {
    if (defaultsApplied) return;
    defaultsApplied = true;
    Map<String, Boolean> merged = dataDefaults().toMap();
    merged.putAll(settings.laneVisibility());
    shown = Visibility.fromMap(merged);
  }

  /**
   * On a live refresh: a lane without a saved choice turns on once it gains
   * data, since the first parse of a streaming capture is thin (GC may only
   * appear after seconds). Lanes with a saved choice stay as chosen.
   */
  void upgradeDefaults(HaxeChartViewSettings settings) {
    Map<String, Boolean> stored = settings.laneVisibility();
    Map<String, Boolean> upgraded = shown.toMap();
    dataDefaults().toMap().forEach((key, hasData) -> {
      if (hasData && !stored.containsKey(key)) {
        upgraded.put(key, true);
      }
    });
    shown = Visibility.fromMap(upgraded);
  }

  /** Rebuilds the chart's lanes from the visibility; a shown lane without data renders its placeholder. */
  void applyTo(HaxeCallChartPanel chart) {
    List<MarkerLane> markerLanes = new ArrayList<>();
    if (shown.frames()) {
      markerLanes.add(frameLane());
    }
    if (shown.gc()) {
      markerLanes.add(gcLane());
    }
    chart.setLanes(markerLanes);
    chart.setCurveLanes(curveLanes());
    chart.setEvents(shown.events() ? eventMarks() : List.of(), shown.events());
    chart.setCallsVisible(shown.calls());
  }

  private MarkerLane frameLane() {
    String name = HaxeProfilerBundle.message("haxe.profiler.callchart.lane.frames");
    List<UsSpan> spans = thread.frameSpans();
    return new MarkerLane(name, span -> frameTitle(spans, span), spans, FRAME_LANE_EVEN, FRAME_LANE_ODD, null,
                          frameBreakdown);
  }

  private MarkerLane gcLane() {
    String name = HaxeProfilerBundle.message("haxe.profiler.callchart.lane.gc");
    GcSpans gc = thread.gc();
    String spanTitle = HaxeProfilerBundle.message(gc.spanKindKey());
    return new MarkerLane(name, span -> spanTitle, gc.spans(), GC_LANE, GC_LANE, gc.spanInfo(), null);
  }

  /** "Frame N", numbered from 1 in capture order. */
  private static String frameTitle(List<UsSpan> spans, UsSpan span) {
    return HaxeProfilerBundle.message("haxe.profiler.callchart.span.frame", String.valueOf(spans.indexOf(span) + 1));
  }

  private List<HaxeCallChartPanel.TimeEvent> eventMarks() {
    return thread.events().stream()
      .map(HaxeChartLanes::timeEventOf)
      .toList();
  }

  /** A core event (ns, RGB int with 0 for none) as a chart mark (µs, optional Color). */
  private static HaxeCallChartPanel.TimeEvent timeEventOf(TimelineEvent event) {
    Color color = event.color() == 0 ? null : new Color(event.color());
    return new HaxeCallChartPanel.TimeEvent(event.timeNs() / 1000, event.text(), color);
  }

  /**
   * The shown categories' curve bands in display order, one per series in
   * the order the data source gives them; a shown category without series
   * contributes one empty placeholder band.
   */
  private List<HaxeCallChartPanel.CurveLane> curveLanes() {
    List<HaxeCallChartPanel.CurveLane> curves = new ArrayList<>();
    for (CurveCategory category : CurveCategory.values()) {
      if (!shown.curves().get(category)) continue;
      Map<String, List<TracySession.PlotPoint>> series = thread.curves().get(category);
      if (series.isEmpty()) {
        String label = HaxeProfilerBundle.message(category.labelKey);
        curves.add(HaxeCallChartPanel.CurveLane.of(label, category.unit, List.of(), curveColor(category, 0)));
        continue;
      }
      int index = 0;
      for (Map.Entry<String, List<TracySession.PlotPoint>> curve : series.entrySet()) {
        List<HaxeCallChartPanel.CurvePoint> points = curve.getValue().stream()
          .map(point -> new HaxeCallChartPanel.CurvePoint(point.timeNs() / 1000, point.value()))
          .toList();
        curves.add(HaxeCallChartPanel.CurveLane.of(curve.getKey(), category.unit, points, curveColor(category, index++)));
      }
    }
    return curves;
  }

  private static Color curveColor(CurveCategory category, int index) {
    return switch (category) {
      case MEMORY -> MEMORY_LANE_COLORS[index % MEMORY_LANE_COLORS.length];
      case GPU_MEMORY -> GPU_MEMORY_LANE;
      case CPU_LOAD -> CPU_LANE;
      case GPU_LOAD -> GPU_LOAD_LANE;
    };
  }
}
