package com.intellij.plugins.haxe.profiler.bridge.chart;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The user's Call Chart arrangement — band order, band heights and collapsed
 * bands, keyed the way the chart keys its bands ("curve:Small Object Heap",
 * "marker:Frames", "events"); lane visibility keyed by the tab's lane ids
 * ("frames", "gc", "events", "curve:MEMORY"); whether the details pane is
 * collapsed. A per-user preference, so it lives in the workspace file,
 * never in shared project settings.
 */
@Service(Service.Level.PROJECT)
@State(name = "HaxeProfilerChartView", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public final class HaxeChartViewSettings implements PersistentStateComponent<HaxeChartViewSettings.State> {

  public static class State {
    public List<String> bandOrder = new ArrayList<>();
    public Map<String, Integer> bandHeights = new HashMap<>();
    public List<String> collapsedBands = new ArrayList<>();
    public Map<String, Boolean> laneVisibility = new HashMap<>();
    public boolean detailsCollapsed;
  }

  private State state = new State();

  static HaxeChartViewSettings getInstance(@NotNull Project project) {
    return project.getService(HaxeChartViewSettings.class);
  }

  @Override
  public @NotNull State getState() {
    return state;
  }

  @Override
  public void loadState(@NotNull State state) {
    this.state = state;
  }

  @NotNull
  List<String> bandOrder() {
    return List.copyOf(state.bandOrder);
  }

  @NotNull
  Map<String, Integer> bandHeights() {
    return Map.copyOf(state.bandHeights);
  }

  @NotNull
  List<String> collapsedBands() {
    return List.copyOf(state.collapsedBands);
  }

  void update(@NotNull List<String> bandOrder, @NotNull Map<String, Integer> bandHeights,
              @NotNull List<String> collapsedBands) {
    state.bandOrder = new ArrayList<>(bandOrder);
    state.bandHeights = new HashMap<>(bandHeights);
    state.collapsedBands = new ArrayList<>(collapsedBands);
  }

  /** Explicit lane show/hide choices; a lane without an entry falls back to its data-driven default. */
  @NotNull
  Map<String, Boolean> laneVisibility() {
    return Map.copyOf(state.laneVisibility);
  }

  void updateLaneVisibility(@NotNull Map<String, Boolean> visibility) {
    state.laneVisibility = new HashMap<>(visibility);
  }

  boolean isDetailsCollapsed() {
    return state.detailsCollapsed;
  }

  void setDetailsCollapsed(boolean collapsed) {
    state.detailsCollapsed = collapsed;
  }
}
