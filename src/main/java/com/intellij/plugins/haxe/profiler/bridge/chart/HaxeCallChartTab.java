package com.intellij.plugins.haxe.profiler.bridge.chart;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.Disposer;
import com.intellij.plugins.haxe.HaxeProfilerBundle;
import com.intellij.plugins.haxe.profiler.bridge.HaxeLiveCaptures;
import com.intellij.plugins.haxe.profiler.bridge.data.HaxeCallStackElement;
import com.intellij.plugins.haxe.profiler.hxt.HxtZoneStore;
import com.intellij.plugins.haxe.profiler.model.ProfilerSnapshot;
import com.intellij.plugins.haxe.profiler.model.ProfilerThread;
import com.intellij.plugins.haxe.profiler.timeline.ProfilerTimeline.FlameNode;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.dsl.listCellRenderer.BuilderKt;
import com.intellij.util.ui.components.BorderLayoutPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;

/**
 * The Call Chart tab: one thread's capture explored over a time axis, a
 * {@link HaxeCallChartPanel} over the thread's flame tree. Sampled snapshots
 * (runs reconstructed from samples) and tracy zone stores (exact measured
 * intervals) feed the same UI. A thread picker on top switches the charted
 * thread, selections on the chart fill the collapsible details pane on the
 * right, and the toolbar on the left zooms and opens the Configure View
 * dialog for lane visibility.
 */
public final class HaxeCallChartTab {
  private static final Logger LOG = Logger.getInstance(HaxeCallChartTab.class);
  /** The live starting view: a sliding window this wide pinned to the tail. */
  private static final long LIVE_WINDOW_US = 2_000_000;

  private final Project project;
  private final HaxeChartViewSettings viewSettings;
  private final HaxeChartLanes lanes = new HaxeChartLanes();
  private final HaxeChartDetailsPanel detailPanel;
  private final HaxeChartSelectionDetails details;
  private final HaxeCallChartPanel chart;
  private final HaxeChartMinimapPanel minimap;
  private final HaxeChartWindowLoader loader;
  private final ComboBox<ProfilerThread> threadPicker;
  private final ActionListener threadListener = event -> showSelectedThread();
  private final HaxeChartSearch search;
  private HaxeChartData data;
  /** The chart's current view; live refreshes build their trees for it. */
  private long viewStartUs;
  private long viewVisibleUs;
  private boolean viewWholeSession = true;

  private HaxeCallChartTab(Project project, HaxeChartData initialData) {
    this.project = project;
    data = initialData;
    viewSettings = HaxeChartViewSettings.getInstance(project);
    detailPanel = new HaxeChartDetailsPanel(project);
    details = new HaxeChartSelectionDetails(detailPanel);
    chart = createChart();
    minimap = new HaxeChartMinimapPanel(chart::setView);
    loader = new HaxeChartWindowLoader(initialData, chart);
    chart.setViewListener(this::viewChanged);

    threadPicker = new ComboBox<>(initialData.threads().toArray(new ProfilerThread[0]));
    threadPicker.setRenderer(BuilderKt.textListCellRenderer("", ProfilerThread::name));
    threadPicker.addActionListener(threadListener);
    search = new HaxeChartSearch(chart, () -> data, this::selectedThread);
    chart.setSearchUi(search);
    chart.setConfigureViewOpener(this::configureView);
    showSelectedThread();
  }

  @NotNull
  public static JComponent create(@NotNull Project project, @NotNull ProfilerSnapshot snapshot) {
    return new HaxeCallChartTab(project, new HaxeSnapshotChartData(snapshot)).component();
  }

  @NotNull
  public static JComponent create(@NotNull Project project, @NotNull HxtZoneStore store) {
    return new HaxeCallChartTab(project, new HaxeZoneChartData(store)).component();
  }

  /**
   * The live form for a still-streaming sample capture: re-reads the growing
   * session file, follows the tail until the user moves the view, and
   * refreshes once more when the capture completes.
   */
  @NotNull
  public static JComponent createLive(@NotNull Project project, @NotNull ProfilerSnapshot initial,
                                      @NotNull Path sessionFile, HaxeLiveCaptures.@NotNull Entry live,
                                      @NotNull Disposable parent) {
    HaxeCallChartTab tab = new HaxeCallChartTab(project, new HaxeSnapshotChartData(initial));
    tab.followLive(HaxeChartLiveReader.forSamples(sessionFile), live, parent);
    return tab.component();
  }

  /** The live form for a still-streaming zone capture: reopens the store as its file grows. */
  @NotNull
  public static JComponent createLiveZones(@NotNull Project project, @NotNull HxtZoneStore initial,
                                           HaxeLiveCaptures.@NotNull Entry live, @NotNull Disposable parent) {
    HaxeCallChartTab tab = new HaxeCallChartTab(project, new HaxeZoneChartData(initial));
    tab.followLive(HaxeChartLiveReader.forZones(initial.file()), live, parent);
    return tab.component();
  }

  private HaxeCallChartPanel createChart() {
    HaxeCallChartPanel panel = new HaxeCallChartPanel(frame -> HaxeCallStackElement.navigateToFrame(project, frame),
                                                      path -> details.showRun(data, path),
                                                      (lane, span) -> details.showSpan(lanes.frameImage(), lane, span),
                                                      details::showCurve,
                                                      details::showEvent);
    panel.setRunCountText(count -> data.runCountText(count));
    panel.setRangeSelectionListener(details::showRange);
    panel.setViewPreferences(viewSettings.bandOrder(), viewSettings.bandHeights(), viewSettings.collapsedBands(),
                             viewSettings::update);
    return panel;
  }

  private void viewChanged(long startUs, long visibleUs, boolean wholeSession) {
    viewStartUs = startUs;
    viewVisibleUs = visibleUs;
    viewWholeSession = wholeSession;
    minimap.showWindow(startUs, visibleUs, wholeSession);
    loader.viewChanged(startUs, visibleUs, wholeSession);
  }

  private @Nullable ProfilerThread selectedThread() {
    return (ProfilerThread)threadPicker.getSelectedItem();
  }

  private boolean isSelected(int threadId) {
    ProfilerThread thread = selectedThread();
    return thread != null && thread.id() == threadId;
  }

  private void showSelectedThread() {
    ProfilerThread thread = selectedThread();
    if (thread == null) return;

    long floorUs = HaxeChartWindowLoader.sessionFloorUs(data);
    loader.initialize(thread.id(), floorUs);
    FlameNode tree = data.treeFor(thread.id(), 0, Long.MAX_VALUE, floorUs);
    chart.setTree(tree);
    lanes.load(data, thread.id());
    lanes.applyDefaults(viewSettings);
    lanes.applyTo(chart);
    minimap.setContent(tree.durationUs(), lanes.frameSpans(), data.coarseActivity(thread.id()));
    search.refresh();
  }

  private void configureView() {
    HaxeChartViewDialog dialog = new HaxeChartViewDialog(project, lanes.shown(), lanes.dataDefaults());
    if (!dialog.showAndGet()) return;

    HaxeChartLanes.Visibility chosen = dialog.visibility();
    lanes.show(chosen);
    viewSettings.updateLaneVisibility(chosen.toMap());
    lanes.applyTo(chart);
  }

  private JComponent component() {
    BorderLayoutPanel chartSide = new BorderLayoutPanel();
    chartSide.addToTop(threadPicker);
    chartSide.addToLeft(toolbar());
    chartSide.addToCenter(chartArea());

    OnePixelSplitter splitter = new OnePixelSplitter(false, 0.72f);
    splitter.setHonorComponentsMinimumSize(true);
    splitter.setFirstComponent(chartSide);
    new HaxeChartDetailsCollapse(splitter, detailPanel, viewSettings);
    return splitter;
  }

  /** The chart's horizontal axis is virtual with its own scroll bar; the scroll pane only scrolls vertically. */
  private JComponent chartArea() {
    JBScrollPane scrollPane = new JBScrollPane(chart,
                                               ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                               ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    BorderLayoutPanel aboveChart = new BorderLayoutPanel();
    aboveChart.addToTop(search.component());
    aboveChart.addToCenter(minimap);

    BorderLayoutPanel chartArea = new BorderLayoutPanel();
    chartArea.addToTop(aboveChart);
    chartArea.addToCenter(scrollPane);
    chartArea.addToBottom(chart.createHorizontalScrollBar());
    return chartArea;
  }

  /** Zoom and expand controls, the Configure View dialog and a shortcut to the profiler settings page. */
  private JComponent toolbar() {
    String settingsPage = HaxeProfilerBundle.message("haxe.profiler.configurable.name");
    Runnable openSettings = () -> ShowSettingsUtil.getInstance().showSettingsDialog(project, settingsPage);
    DefaultActionGroup actions = new DefaultActionGroup();
    actions.add(action("haxe.profiler.callchart.zoom.in", AllIcons.General.ZoomIn, chart::zoomIn));
    actions.add(action("haxe.profiler.callchart.zoom.out", AllIcons.General.ZoomOut, chart::zoomOut));
    actions.addSeparator();
    actions.add(action("haxe.profiler.callchart.expand.all", AllIcons.Actions.Expandall, chart::expandAll));
    actions.add(action("haxe.profiler.callchart.collapse.all", AllIcons.Actions.Collapseall, chart::collapseAll));
    actions.addSeparator();
    actions.add(action("haxe.profiler.callchart.configure.view", AllIcons.Actions.Show, this::configureView));
    actions.add(action("haxe.profiler.callchart.open.settings", AllIcons.General.Settings, openSettings));

    ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("HaxeCallChart", actions, false);
    toolbar.setTargetComponent(chart);
    return toolbar.getComponent();
  }

  private static AnAction action(String textKey, Icon icon, Runnable perform) {
    return DumbAwareAction.create(HaxeProfilerBundle.message(textKey), icon, event -> perform.run());
  }

  private void followLive(HaxeChartLiveReader reader, HaxeLiveCaptures.Entry live, Disposable parent) {
    chart.setFollowLive(true);
    LiveRefresh refresh = new LiveRefresh(reader);
    Timer timer = new Timer(reader.refreshMs(), event -> refresh.tick(false));
    timer.start();
    live.onCompletion(() -> {
      refresh.tick(true);
      timer.stop();
    });
    Disposer.register(parent, timer::stop);
  }

  /** Adds newly appeared threads to the picker without firing the selection listener or changing the choice. */
  private void refreshThreadItems(List<ProfilerThread> threads) {
    if (threadPicker.getItemCount() == threads.size()) return;
    ProfilerThread selected = selectedThread();
    threadPicker.removeActionListener(threadListener);
    try {
      threadPicker.setModel(new DefaultComboBoxModel<>(threads.toArray(new ProfilerThread[0])));
      if (selected != null) {
        threads.stream()
          .filter(thread -> thread.id() == selected.id())
          .findFirst()
          .ifPresent(threadPicker::setSelectedItem);
      }
    }
    finally {
      threadPicker.addActionListener(threadListener);
    }
  }

  /**
   * Keeps the chart current while the capture streams. A refresh's cost
   * stays flat as the session grows: the reader consumes only what was
   * appended, and the tree builds only for the visible window plus one
   * viewport of margin on each side. The reading runs on a pooled thread;
   * the EDT only swaps the results in.
   */
  private final class LiveRefresh {
    private final HaxeChartLiveReader reader;
    private final AtomicBoolean busy = new AtomicBoolean();
    private boolean windowStarted;

    /** The data read by one refresh; {@code thread} is absent while no thread is selected. */
    private record Update(HaxeChartData data, @Nullable ThreadUpdate thread) {
    }

    /** The refreshed tree and lane inputs of the thread selected when the refresh started. */
    private record ThreadUpdate(int threadId, FlameNode tree, HaxeChartLanes.ThreadData lanes) {
    }

    LiveRefresh(HaxeChartLiveReader reader) {
      this.reader = reader;
    }

    void tick(boolean force) {
      if (!busy.compareAndSet(false, true)) return;
      ProfilerThread thread = selectedThread();
      long fromUs = viewWholeSession ? Long.MIN_VALUE / 2 : viewStartUs - viewVisibleUs;
      long toUs = viewWholeSession ? Long.MAX_VALUE / 2 : viewStartUs + 2 * viewVisibleUs;
      ApplicationManager.getApplication().executeOnPooledThread(() -> {
        Update update = read(force, thread, fromUs, toUs);
        if (update == null) {
          busy.set(false);
          return;
        }
        ApplicationManager.getApplication().invokeLater(() -> {
          busy.set(false);
          apply(update);
        });
      });
    }

    /** Null when nothing new landed or the read failed; the next refresh tries again. */
    private @Nullable Update read(boolean force, @Nullable ProfilerThread thread, long fromUs, long toUs) {
      try {
        HaxeChartData fresh = reader.reload(force);
        if (fresh == null) return null;
        if (thread == null) return new Update(fresh, null);
        FlameNode tree = reader.treeFor(fresh, thread.id(), fromUs, toUs);
        HaxeChartLanes.ThreadData lanes = HaxeChartLanes.ThreadData.compute(fresh, thread.id());
        return new Update(fresh, new ThreadUpdate(thread.id(), tree, lanes));
      }
      catch (IOException tornRead) {
        return null; // the file was read mid-write
      }
      catch (RuntimeException e) {
        // an escaping exception would leave the busy flag set and stop every later refresh
        LOG.warn("live refresh failed", e);
        return null;
      }
    }

    private void apply(Update update) {
      data = update.data();
      loader.setData(update.data());
      refreshThreadItems(update.data().threads());
      ThreadUpdate charted = update.thread();
      if (charted == null || !isSelected(charted.threadId())) return;

      lanes.assign(charted.lanes(), update.data(), charted.threadId());
      lanes.upgradeDefaults(viewSettings);
      lanes.applyTo(chart);
      chart.updateTree(charted.tree());
      minimap.setContent(charted.tree().durationUs(), lanes.frameSpans(), List.of());
      if (!windowStarted) {
        windowStarted = chart.startLiveWindow(LIVE_WINDOW_US);
      }
      chart.liveDataAppended();
    }
  }
}
