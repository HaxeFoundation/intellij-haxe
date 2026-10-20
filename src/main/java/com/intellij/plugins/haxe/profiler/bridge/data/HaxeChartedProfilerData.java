package com.intellij.plugins.haxe.profiler.bridge.data;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeProfilerBundle;
import com.intellij.plugins.haxe.profiler.bridge.HaxeLiveCaptures;
import com.intellij.plugins.haxe.profiler.bridge.HaxeProfilerTabContent;
import com.intellij.plugins.haxe.profiler.bridge.hints.HaxeIuPerformanceHints;
import com.intellij.profiler.DummyCallTreeBuilder;
import com.intellij.profiler.api.BaseCallStackElement;
import com.intellij.profiler.api.CallTreeBuildingData;
import com.intellij.profiler.api.MultipleCallTreesProfilerData;
import com.intellij.profiler.api.ProfilerData;
import com.intellij.profiler.ui.MainCallTreeDataComponent;
import com.intellij.ui.tabs.TabInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Haxe profiling data for the IU profiler views: the standard call-tree
 * tabs plus the Call Chart. Wraps a {@link MultipleCallTreesProfilerData}
 * rather than subclassing it (the sampling hierarchy is sealed).
 *
 * A capture still being written renders LIVE: the tree tabs from the
 * partial file, the chart refreshing itself off the growing file, and one
 * rebuild of the whole component from the final file when the capture
 * completes, so the tree tabs stop being an early partial snapshot. The
 * component goes to the platform UNWRAPPED and the completion swap happens
 * in place ({@link HaxeProfilerTabContent}): the process panel's tab lookup
 * casts its content to MainCallTreeDataComponent, and a wrapper makes every
 * tab action throw.
 */
abstract class HaxeChartedProfilerData implements ProfilerData {

  private final MultipleCallTreesProfilerData trees;

  protected HaxeChartedProfilerData(@NotNull MultipleCallTreesProfilerData trees) {
    this.trees = trees;
  }

  /** The session file a live capture may still be writing; null for data from elsewhere. */
  @Nullable
  protected abstract Path sessionFile();

  /** Whether a finished capture has anything for the Call Chart to show. */
  protected abstract boolean hasChartData();

  /** The Call Chart: self-refreshing while {@code live} is non-null. */
  @NotNull
  protected abstract JComponent createCallChart(@NotNull Project project, HaxeLiveCaptures.@Nullable Entry live,
                                                @NotNull Disposable parent);

  /** The data parsed again from the completed session file. */
  @NotNull
  protected abstract HaxeChartedProfilerData reparse(@NotNull Path completedFile) throws IOException;

  /** One call-tree state over the builder's stacks. */
  @NotNull
  static CallTreeBuildingData callTree(@NotNull String nameKey, @NotNull DummyCallTreeBuilder<BaseCallStackElement> builder,
                                       @NotNull String id) {
    return new CallTreeBuildingData(HaxeProfilerBundle.message(nameKey), new HaxeCallStackElementRenderer(), builder, id);
  }

  /** The standard tabs plus the Call Chart (non-closable, not auto-selected, no event-state controller). */
  @Override
  public final @NotNull JComponent doCreateTopLevelComponent(@NotNull Project project, @NotNull Disposable parent) {
    Path file = sessionFile();
    HaxeLiveCaptures.Entry live = file == null ? null : HaxeLiveCaptures.find(file);
    if (live != null && live.isLive()) {
      return liveComponent(project, parent, live, file);
    }
    return buildComponent(project, parent, null, false, null);
  }

  private JComponent liveComponent(Project project, Disposable parent, HaxeLiveCaptures.Entry live, Path file) {
    JComponent[] liveChart = new JComponent[1];
    JComponent liveMain = buildComponent(project, parent, live, false, liveChart);
    live.onCompletion(() -> ApplicationManager.getApplication().executeOnPooledThread(() -> {
      HaxeChartedProfilerData finalData;
      try {
        finalData = reparse(file);
      }
      catch (IOException | RuntimeException e) {
        // the live chart keeps showing its last refresh, but the stale live
        // component stays up, so the log must say why
        Logger.getInstance(HaxeChartedProfilerData.class).warn("completion rebuild failed", e);
        return;
      }
      ApplicationManager.getApplication().invokeLater(() -> {
        // the rebuild must not steal the user's place: a chart being
        // watched stays the selected tab afterwards
        boolean chartShowing = liveChart[0] != null && liveChart[0].isShowing();
        JComponent finalMain = finalData.buildComponent(project, parent, null, chartShowing, null);
        HaxeProfilerTabContent.swap(liveMain, finalMain);
        HaxeIuPerformanceHints.captureDataReplaced(project, this, finalData);
      });
    }));
    return liveMain;
  }

  private JComponent buildComponent(Project project, Disposable parent, HaxeLiveCaptures.@Nullable Entry live,
                                    boolean selectChart, JComponent @Nullable [] chartOut) {
    MainCallTreeDataComponent main = new MainCallTreeDataComponent(project, trees, parent, null, false);
    // a live tab renders even before the first data lands - it fills itself
    if (live != null || hasChartData()) {
      JComponent chart = createCallChart(project, live, parent);
      if (chartOut != null) chartOut[0] = chart;
      TabInfo callChartTab = new TabInfo(chart);
      callChartTab.setText(HaxeProfilerBundle.message("haxe.profiler.callchart.tab"));
      main.addTab(callChartTab, false, selectChart, false);
    }
    return main;
  }
}
