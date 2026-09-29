package com.intellij.plugins.haxe.profiler.bridge.chart;

import com.intellij.plugins.haxe.profiler.bridge.chart.HaxeChartData.CurveCategory;
import com.intellij.plugins.haxe.profiler.bridge.chart.HaxeChartLanes.Visibility;
import com.intellij.plugins.haxe.profiler.model.ProfilerThread;
import com.intellij.plugins.haxe.profiler.timeline.ProfilerTimeline.FlameNode;
import com.intellij.plugins.haxe.profiler.timeline.ProfilerTimeline.UsSpan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Profiler: chart lanes")
public class HaxeChartLanesTest {
  private static final List<UsSpan> SPANS = List.of(new UsSpan(0, 10), new UsSpan(10, 20));

  @Test
  @DisplayName("lanes without data start hidden")
  public void testLanesWithoutDataStartHidden() {
    HaxeChartLanes lanes = new HaxeChartLanes();
    lanes.load(new FakeData(SPANS, List.of()), 1);

    lanes.applyDefaults(settingsWith(Map.of()));

    assertEquals(new Visibility(false, true, false, true, curves(false)), lanes.shown());
  }

  @Test
  @DisplayName("saved choice overrides data default")
  public void testSavedChoiceOverridesDataDefault() {
    HaxeChartLanes lanes = new HaxeChartLanes();
    lanes.load(new FakeData(SPANS, List.of()), 1);

    lanes.applyDefaults(settingsWith(Map.of("gc", false, "frames", true)));

    assertTrue(lanes.shown().frames());
    assertFalse(lanes.shown().gc());
  }

  @Test
  @DisplayName("live data turns on only unsaved lanes")
  public void testLiveDataTurnsOnOnlyUnsavedLanes() {
    HaxeChartLanes lanes = new HaxeChartLanes();
    HaxeChartViewSettings settings = settingsWith(Map.of("gc", false));
    lanes.load(new FakeData(List.of(), List.of()), 1);
    lanes.applyDefaults(settings);
    assertFalse(lanes.shown().frames());

    lanes.load(new FakeData(SPANS, SPANS), 1);
    lanes.upgradeDefaults(settings);

    assertTrue(lanes.shown().frames());
    assertFalse(lanes.shown().gc());
  }

  @Test
  @DisplayName("visibility survives the settings map")
  public void testVisibilitySurvivesTheSettingsMap() {
    Map<CurveCategory, Boolean> curves = curves(false);
    curves.put(CurveCategory.CPU_LOAD, true);
    Visibility visibility = new Visibility(true, false, true, false, curves);

    assertEquals(visibility, Visibility.fromMap(visibility.toMap()));
  }

  private static HaxeChartViewSettings settingsWith(Map<String, Boolean> laneVisibility) {
    HaxeChartViewSettings settings = new HaxeChartViewSettings();
    HaxeChartViewSettings.State state = new HaxeChartViewSettings.State();
    state.laneVisibility.putAll(laneVisibility);
    settings.loadState(state);
    return settings;
  }

  private static Map<CurveCategory, Boolean> curves(boolean shown) {
    Map<CurveCategory, Boolean> curves = new EnumMap<>(CurveCategory.class);
    for (CurveCategory category : CurveCategory.values()) {
      curves.put(category, shown);
    }
    return curves;
  }

  /** A capture with one thread that has only the given GC and frame spans. */
  private record FakeData(List<UsSpan> gcSpans, List<UsSpan> frameSpans) implements HaxeChartData {
    @Override
    public List<ProfilerThread> threads() {
      return List.of(new ProfilerThread(1, "main"));
    }

    @Override
    public FlameNode treeFor(int threadId, long fromUs, long toUs, long minDurationUs) {
      return new FlameNode(null, 0, 20, 0, List.of(), false);
    }

    @Override
    public GcSpans gcSpansFor(int threadId) {
      return new GcSpans(gcSpans, "haxe.profiler.callchart.span.gc", null);
    }

    @Override
    public List<UsSpan> frameSpansFor(int threadId) {
      return frameSpans;
    }
  }
}
