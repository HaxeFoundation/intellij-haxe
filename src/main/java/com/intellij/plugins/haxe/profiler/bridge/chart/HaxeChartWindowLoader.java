package com.intellij.plugins.haxe.profiler.bridge.chart;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.plugins.haxe.profiler.timeline.ProfilerTimeline.FlameNode;

import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.Timer;

/**
 * Reloads the chart's tree as the view moves, for data sources with
 * windowed loads: the visible window plus one viewport of margin on each
 * side, with runs shorter than 1/{@value #RESOLUTION} of the view dropped,
 * so every window loads at full usable detail however long the capture is.
 * Debounced; results land on the EDT and superseded loads are discarded.
 */
final class HaxeChartWindowLoader {
  static final int RESOLUTION = 32_768;
  private static final int DEBOUNCE_MS = 150;

  private final HaxeCallChartPanel chart;
  private final Timer debounce = new Timer(DEBOUNCE_MS, event -> load());
  private final AtomicInteger generation = new AtomicInteger();
  private HaxeChartData data;
  private int threadId;
  private long loadedFromUs;
  private long loadedToUs;
  private long loadedMinDurationUs;
  private long pendingFromUs;
  private long pendingToUs;
  private long pendingMinDurationUs;

  HaxeChartWindowLoader(HaxeChartData data, HaxeCallChartPanel chart) {
    this.data = data;
    this.chart = chart;
    debounce.setRepeats(false);
  }

  /** The shortest run the whole-session tree keeps. */
  static long sessionFloorUs(HaxeChartData data) {
    return data.windowedLoads() ? data.durationUs() / RESOLUTION : 0;
  }

  /** A live refresh swapped the data source; window loads read the new one from here on. */
  void setData(HaxeChartData data) {
    this.data = data;
  }

  /** The thread's whole-session tree is about to load at {@code floorUs} resolution. */
  void initialize(int threadId, long floorUs) {
    this.threadId = threadId;
    loadedFromUs = 0;
    loadedToUs = Long.MAX_VALUE;
    loadedMinDurationUs = floorUs;
    generation.incrementAndGet();
    debounce.stop();
  }

  void viewChanged(long startUs, long visibleUs, boolean wholeSession) {
    if (!data.windowedLoads()) return;
    long needFromUs = wholeSession ? 0 : Math.max(0, startUs - visibleUs);
    long needToUs = wholeSession ? Long.MAX_VALUE : startUs + 2 * visibleUs;
    long needMinDurationUs = visibleUs / RESOLUTION;
    boolean covered = needFromUs >= loadedFromUs && needToUs <= loadedToUs && needMinDurationUs >= loadedMinDurationUs;
    if (covered) return;

    pendingFromUs = needFromUs;
    pendingToUs = needToUs;
    pendingMinDurationUs = needMinDurationUs;
    debounce.restart();
  }

  private void load() {
    int expected = generation.incrementAndGet();
    int thread = threadId;
    long fromUs = pendingFromUs;
    long toUs = pendingToUs;
    long minDurationUs = pendingMinDurationUs;
    HaxeChartData source = data;
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      FlameNode tree = source.treeFor(thread, fromUs, toUs, minDurationUs);
      ApplicationManager.getApplication().invokeLater(() -> {
        if (generation.get() != expected) return; // a newer view superseded this load
        loadedFromUs = fromUs;
        loadedToUs = toUs;
        loadedMinDurationUs = minDurationUs;
        chart.updateTree(tree);
      });
    });
  }
}
