package com.intellij.plugins.haxe.profiler.bridge.chart;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.plugins.haxe.HaxeProfilerBundle;
import com.intellij.plugins.haxe.profiler.bridge.HaxeProfilerFormats;
import com.intellij.plugins.haxe.profiler.bridge.chart.HaxeCallChartPanel.CurveLane;
import com.intellij.plugins.haxe.profiler.bridge.chart.HaxeCallChartPanel.CurvePoint;
import com.intellij.plugins.haxe.profiler.bridge.chart.HaxeCallChartPanel.MarkerLane;
import com.intellij.plugins.haxe.profiler.model.StackFrame;
import com.intellij.plugins.haxe.profiler.timeline.ProfilerTimeline.FlameNode;
import com.intellij.plugins.haxe.profiler.timeline.ProfilerTimeline.UsSpan;
import org.jetbrains.annotations.Nullable;

import java.awt.Image;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/** Fills the details pane from what the user selected on the chart. */
final class HaxeChartSelectionDetails {
  private final HaxeChartDetailsPanel panel;

  HaxeChartSelectionDetails(HaxeChartDetailsPanel panel) {
    this.panel = panel;
  }

  /** The selected run's summary above its call chain; an empty path restores the hint. */
  void showRun(HaxeChartData data, List<FlameNode> path) {
    if (path.isEmpty()) {
      showHint();
      return;
    }
    FlameNode run = path.getLast();
    String symbol = run.frame() == null ? "" : run.frame().symbol();
    String timeRange = HaxeProfilerFormats.formatRange(run.startUs(), run.endUs());
    String runCount = data.runCountText(run.samples());
    List<StackFrame> stack = path.stream().map(FlameNode::frame).filter(Objects::nonNull).toList();
    panel.showStack(List.of(symbol, timeRange, runCount), stack);
  }

  /**
   * A selected marker-lane span: what it is, when it ran and the lane's
   * per-span detail if it has one. A frame's breakdown and screenshot load
   * on a pooled thread, since they may read the capture file.
   */
  void showSpan(@Nullable Function<UsSpan, Image> imageLoader, MarkerLane lane, UsSpan span) {
    String timeRange = HaxeProfilerFormats.formatRange(span.startUs(), span.endUs());
    List<String> headerLines = new ArrayList<>(List.of(lane.spanTitle().apply(span), timeRange));
    String spanInfo = lane.spanInfo() == null ? null : lane.spanInfo().apply(span);
    if (spanInfo != null) {
      headerLines.add(spanInfo);
    }
    panel.showStack(headerLines, List.of());

    Function<UsSpan, HaxeFrameBreakdownView.Rows> breakdown = lane.breakdown();
    if (breakdown == null) return;
    int expected = panel.revision();
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      HaxeFrameBreakdownView.Rows rows = breakdown.apply(span);
      Image image = imageLoader == null ? null : imageLoader.apply(span);
      ApplicationManager.getApplication().invokeLater(() -> {
        if (panel.revision() != expected) return; // a newer selection took the panel
        showBreakdown(headerLines, rows);
        panel.showImage(image);
      });
    });
  }

  /** An empty breakdown says so; a sampled frame may simply have caught no ticks. */
  private void showBreakdown(List<String> headerLines, HaxeFrameBreakdownView.Rows rows) {
    if (rows.isEmpty()) {
      List<String> lines = new ArrayList<>(headerLines);
      lines.add(HaxeProfilerBundle.message("haxe.profiler.callchart.no.data"));
      panel.showStack(lines, List.of());
    }
    else {
      panel.showBreakdown(headerLines, rows);
    }
  }

  /**
   * A selected curve reading: the series, the value at the clicked instant
   * and, when the value has held for a while, when it last changed. The
   * curve is a step function, so it is flat between changes.
   */
  void showCurve(HaxeCallChartPanel.CurveSelection selection) {
    CurveLane lane = selection.lane();
    String value = lane.formatted(selection.lastChange().value());
    String instant = HaxeProfilerFormats.formatInstant(selection.instantUs());
    List<String> lines = new ArrayList<>(List.of(lane.name(), value, instant));
    if (selection.instantUs() - selection.lastChange().timeUs() > 1000) {
      String changedAt = HaxeProfilerFormats.formatInstant(selection.lastChange().timeUs());
      lines.add(HaxeProfilerBundle.message("haxe.profiler.callchart.memory.since", changedAt));
    }
    panel.showStack(lines, List.of());
  }

  /** A selected events-row mark: its text and the instant it fired. */
  void showEvent(HaxeCallChartPanel.TimeEvent event) {
    panel.showStack(List.of(event.text(), HaxeProfilerFormats.formatInstant(event.timeUs())), List.of());
  }

  /** A dragged lane range: statistics over the covered stretch; null restores the hint. */
  void showRange(HaxeCallChartPanel.@Nullable RangeSelection range) {
    if (range == null) {
      showHint();
    }
    else if (range.curveLane() != null) {
      showCurveRange(range.curveLane(), range.fromUs(), range.toUs());
    }
    else if (range.markerLane() != null) {
      showMarkerRange(range.markerLane(), range.fromUs(), range.toUs());
    }
  }

  private void showHint() {
    panel.showText(HaxeProfilerBundle.message("haxe.profiler.callchart.details.hint"));
  }

  /** Min, time-weighted average and max of the curve across the range. */
  private void showCurveRange(CurveLane lane, long fromUs, long toUs) {
    List<String> lines = new ArrayList<>();
    lines.add(lane.name());
    lines.add(HaxeProfilerFormats.formatRange(fromUs, toUs));
    CurveRangeStats stats = CurveRangeStats.of(lane.points(), fromUs, toUs);
    if (stats == null) {
      lines.add(HaxeProfilerBundle.message("haxe.profiler.callchart.no.data"));
    }
    else {
      lines.add(HaxeProfilerBundle.message("haxe.profiler.callchart.range.min", lane.formatted(stats.min())));
      lines.add(HaxeProfilerBundle.message("haxe.profiler.callchart.range.average", lane.formatted(stats.average())));
      lines.add(HaxeProfilerBundle.message("haxe.profiler.callchart.range.max", lane.formatted(stats.max())));
    }
    panel.showStack(lines, List.of());
  }

  /**
   * The spans the range covers: their count, rate and extremes. A lane with
   * a breakdown (frames) also aggregates what the whole range's time went to.
   */
  private void showMarkerRange(MarkerLane lane, long fromUs, long toUs) {
    List<UsSpan> covered = lane.spans().stream()
      .filter(span -> span.endUs() > fromUs && span.startUs() < toUs)
      .toList();
    List<String> lines = new ArrayList<>();
    lines.add(lane.name());
    lines.add(HaxeProfilerFormats.formatRange(fromUs, toUs));
    if (covered.isEmpty()) {
      lines.add(HaxeProfilerBundle.message("haxe.profiler.callchart.no.data"));
      panel.showStack(lines, List.of());
      return;
    }

    UsSpan fastest = covered.stream().min(Comparator.comparingLong(UsSpan::durationUs)).orElseThrow();
    UsSpan slowest = covered.stream().max(Comparator.comparingLong(UsSpan::durationUs)).orElseThrow();
    String perSecond = String.format(Locale.ROOT, "%.1f", covered.size() * 1_000_000.0 / (toUs - fromUs));
    String fastestTime = HaxeProfilerFormats.formatUs(fastest.durationUs());
    String slowestTime = HaxeProfilerFormats.formatUs(slowest.durationUs());
    lines.add(HaxeProfilerBundle.message("haxe.profiler.callchart.range.count", covered.size()));
    lines.add(HaxeProfilerBundle.message("haxe.profiler.callchart.range.rate", perSecond));
    lines.add(HaxeProfilerBundle.message("haxe.profiler.callchart.range.fastest", fastestTime));
    lines.add(HaxeProfilerBundle.message("haxe.profiler.callchart.range.slowest", slowestTime));
    panel.showStack(lines, List.of());

    Function<UsSpan, HaxeFrameBreakdownView.Rows> breakdown = lane.breakdown();
    if (breakdown == null) return;
    int expected = panel.revision();
    UsSpan wholeRange = new UsSpan(fromUs, toUs);
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      HaxeFrameBreakdownView.Rows rows = breakdown.apply(wholeRange);
      ApplicationManager.getApplication().invokeLater(() -> {
        if (panel.revision() == expected && !rows.isEmpty()) {
          panel.showBreakdown(lines, rows);
        }
      });
    });
  }

  /** Min, max and time-weighted average a step curve holds across a range. */
  private record CurveRangeStats(double min, double max, double average) {

    /** Each point's value holds until the next point; null when no value holds inside the range. */
    static @Nullable CurveRangeStats of(List<CurvePoint> points, long fromUs, long toUs) {
      double held = Double.NaN;
      long heldSinceUs = fromUs;
      double min = Double.MAX_VALUE;
      double max = -Double.MAX_VALUE;
      double weightedSum = 0;
      long coveredUs = 0;
      for (CurvePoint point : points) {
        if (point.timeUs() >= toUs) break;
        if (point.timeUs() <= fromUs) {
          held = point.value(); // the value entering the range still holds
          continue;
        }
        if (!Double.isNaN(held)) {
          long heldForUs = point.timeUs() - heldSinceUs;
          weightedSum += held * heldForUs;
          coveredUs += heldForUs;
          min = Math.min(min, held);
          max = Math.max(max, held);
        }
        held = point.value();
        heldSinceUs = point.timeUs();
      }
      if (Double.isNaN(held)) return null;

      long tailUs = toUs - heldSinceUs;
      weightedSum += held * tailUs;
      coveredUs += tailUs;
      min = Math.min(min, held);
      max = Math.max(max, held);
      return coveredUs > 0 ? new CurveRangeStats(min, max, weightedSum / coveredUs) : null;
    }
  }
}
