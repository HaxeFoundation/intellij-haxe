package com.intellij.plugins.haxe.profiler.bridge.chart;

import com.intellij.plugins.haxe.HaxeProfilerBundle;
import com.intellij.plugins.haxe.profiler.bridge.chart.HaxeChartData.FrameSlice;
import com.intellij.plugins.haxe.profiler.model.PseudoFrames;
import com.intellij.plugins.haxe.profiler.timeline.ProfilerTimeline.UsSpan;
import com.intellij.ui.JBColor;

import java.awt.Color;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Rolls a frame's time slices up into the rows of {@link HaxeFrameBreakdownView}. */
final class HaxeFrameBreakdown {

  /** How many parts a breakdown lists before folding the rest into "other". */
  private static final int PART_ROWS = 8;
  private static final Color FOLDED_PARTS = new JBColor(0xC79A50, 0x8A6D3F);

  /** The summary categories a frame's shares roll up into, in display order. */
  private enum ShareCategory {
    SCRIPT("haxe.profiler.callchart.category.script", new JBColor(0x548AF7, 0x4E76C4)),
    RENDER("haxe.profiler.callchart.category.render", new JBColor(0x59A869, 0x527C5B)),
    GC("haxe.profiler.callchart.category.gc", HaxeChartLanes.GC_LANE),
    IDLE("haxe.profiler.callchart.category.idle", new JBColor(0xA8ADBD, 0x6F737A));

    final String labelKey;
    final Color color;

    ShareCategory(String labelKey, Color color) {
      this.labelKey = labelKey;
      this.color = color;
    }

    /** Pseudo-frames name their category; real functions are script work. */
    static ShareCategory of(String sliceName) {
      if (PseudoFrames.isIdle(sliceName)) return IDLE;
      if (PseudoFrames.RENDER.equals(sliceName)) return RENDER;
      if (PseudoFrames.isGc(sliceName)) return GC;
      return SCRIPT;
    }
  }

  private HaxeFrameBreakdown() {
  }

  /**
   * The category summary (script, render, GC, idle) above the biggest
   * individual parts, the small ones folded into "other"; each part row
   * wears its category's color. Frame time no slice covers is the wait for
   * the next frame and counts as idle.
   */
  static HaxeFrameBreakdownView.Rows rows(List<FrameSlice> slices, UsSpan frame, List<UsSpan> gcSpans) {
    long frameUs = frame.durationUs();
    if (slices.isEmpty() || frameUs <= 0) return new HaxeFrameBreakdownView.Rows(List.of(), List.of());
    return new HaxeFrameBreakdownView.Rows(summaryRows(slices, frameUs, overlapUs(gcSpans, frame)),
                                           partRows(slices, frameUs));
  }

  private static List<HaxeFrameBreakdownView.Row> summaryRows(List<FrameSlice> slices, long frameUs, long gcSpanUs) {
    Map<ShareCategory, Long> byCategory = new EnumMap<>(ShareCategory.class);
    long covered = 0;
    for (FrameSlice slice : slices) {
      byCategory.merge(ShareCategory.of(slice.name()), slice.totalUs(), Long::sum);
      covered += slice.totalUs();
    }
    // A long collection stops the world: no GC-named samples are taken and
    // the stall's time lands in the first application sample after it. The
    // GC lane's spans are the runtime's own figure, so the part of them the
    // samples missed moves from script into GC.
    long unsampledGcUs = gcSpanUs - byCategory.getOrDefault(ShareCategory.GC, 0L);
    long hiddenGcUs = Math.min(Math.max(unsampledGcUs, 0), byCategory.getOrDefault(ShareCategory.SCRIPT, 0L));
    if (hiddenGcUs > 0) {
      byCategory.merge(ShareCategory.GC, hiddenGcUs, Long::sum);
      byCategory.merge(ShareCategory.SCRIPT, -hiddenGcUs, Long::sum);
    }
    if (frameUs > covered) {
      byCategory.merge(ShareCategory.IDLE, frameUs - covered, Long::sum);
    }

    List<HaxeFrameBreakdownView.Row> summary = new ArrayList<>();
    for (ShareCategory category : ShareCategory.values()) {
      Long totalUs = byCategory.get(category);
      if (totalUs != null && totalUs > 0) {
        String label = HaxeProfilerBundle.message(category.labelKey);
        summary.add(new HaxeFrameBreakdownView.Row(label, totalUs, frameUs, category.color, true));
      }
    }
    return summary;
  }

  /** The non-idle slices, biggest first as given, beyond {@link #PART_ROWS} folded into one "other" row. */
  private static List<HaxeFrameBreakdownView.Row> partRows(List<FrameSlice> slices, long frameUs) {
    List<HaxeFrameBreakdownView.Row> parts = new ArrayList<>();
    long foldedUs = 0;
    for (FrameSlice slice : slices) {
      ShareCategory category = ShareCategory.of(slice.name());
      if (category == ShareCategory.IDLE) continue;
      if (parts.size() < PART_ROWS) {
        parts.add(new HaxeFrameBreakdownView.Row(slice.name(), slice.totalUs(), frameUs, category.color, false));
      }
      else {
        foldedUs += slice.totalUs();
      }
    }
    if (foldedUs > 0) {
      String label = HaxeProfilerBundle.message("haxe.profiler.callchart.frame.other");
      parts.add(new HaxeFrameBreakdownView.Row(label, foldedUs, frameUs, FOLDED_PARTS, false));
    }
    return parts;
  }

  /** The microseconds of {@code spans} falling inside {@code frame}. */
  private static long overlapUs(List<UsSpan> spans, UsSpan frame) {
    long overlap = 0;
    for (UsSpan span : spans) {
      overlap += Math.max(0, Math.min(span.endUs(), frame.endUs()) - Math.max(span.startUs(), frame.startUs()));
    }
    return overlap;
  }
}
