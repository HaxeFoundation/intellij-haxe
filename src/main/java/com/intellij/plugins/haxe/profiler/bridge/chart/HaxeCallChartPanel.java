package com.intellij.plugins.haxe.profiler.bridge.chart;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.plugins.haxe.HaxeProfilerBundle;
import com.intellij.plugins.haxe.profiler.bridge.HaxeProfilerFormats;
import com.intellij.plugins.haxe.profiler.model.StackFrame;
import com.intellij.plugins.haxe.profiler.timeline.ProfilerTimeline.FlameNode;
import com.intellij.plugins.haxe.profiler.timeline.ProfilerTimeline.UsSpan;
import com.intellij.plugins.haxe.profiler.timeline.ProfilerTimeline;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Adjustable;
import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;

/**
 * A call chart over one thread's time-ordered flame tree
 * ({@link ProfilerTimeline#flameTree}): time runs left to right under a
 * ruler, one row per stack depth with the outermost call on top. Bands
 * between the ruler and the runs chart other data on the same axis: curves,
 * the events row and marker lanes (frames, GC). Runs narrower than a pixel
 * paint as 1&nbsp;px slivers so activity stays visible at any zoom; idle
 * spans stay unpainted.
 * <p>
 * The horizontal axis is virtual: the panel always fills the viewport and
 * paints the window given by a view start and a scale, because a
 * session-wide component would overflow int pixel coordinates at deep
 * zoom. Its own scrollbar ({@link #createHorizontalScrollBar()}) scrolls
 * that axis. Ctrl+wheel zooms around the pointer, shift+wheel pans, and the
 * plain wheel scrolls the pane vertically. A click selects a run and
 * reports its call chain; a double-click opens its Haxe source.
 */
final class HaxeCallChartPanel extends JComponent implements Scrollable {

  /**
   * A named row of spans above the chart; even/odd colors alternate so span
   * boundaries stay visible. {@code spanTitle} names ONE span for the detail
   * view ("Frame 428", "GC sweep"); {@code spanInfo} may add one per-span
   * detail line ("Freed 1.2 MB in 300 objects"); {@code breakdown} loads a
   * span's multi-line time breakdown on a POOLED thread (it may read the
   * capture file). An empty span list still renders (label plus a no-data
   * placeholder) so an opened toggle never reads as broken.
   */
  record MarkerLane(@NotNull String name, @NotNull Function<UsSpan, String> spanTitle, @NotNull List<UsSpan> spans,
                    @NotNull Color evenColor, @NotNull Color oddColor,
                    @Nullable Function<UsSpan, String> spanInfo,
                    @Nullable Function<UsSpan, HaxeFrameBreakdownView.Rows> breakdown) {
  }

  record CurvePoint(long timeUs, double value) {
  }

  /** Told after every view mutation (zoom, pan, scroll, resize, new tree) — the minimap and window loader feed off it. */
  interface ViewListener {
    void viewChanged(long viewStartUs, long visibleUs, boolean wholeSession);
  }

  /** Told the arrangement to persist after a reorder drop, a divider-resize release or a collapse toggle. */
  interface ViewPreferencesListener {
    void changed(@NotNull List<String> bandOrder, @NotNull Map<String, Integer> bandHeights,
                 @NotNull List<String> collapsedBands);
  }

  /**
   * A selected reading on a curve band: the clicked instant, and the sample
   * whose value holds there (the last change at or before it).
   */
  record CurveSelection(@NotNull CurveLane lane, long instantUs, @NotNull CurvePoint lastChange) {
  }

  /**
   * A dragged time range on one lane, for aggregate statistics in the
   * details panel. Exactly one of {@code curveLane}/{@code markerLane} is
   * set — the lane the drag started on.
   */
  record RangeSelection(@Nullable CurveLane curveLane, @Nullable MarkerLane markerLane, long fromUs, long toUs) {
  }

  /** One search hit in the session — enough to jump there and re-find its node once the window loads. */
  record SearchMatch(long startUs, long endUs, int depth, @NotNull String symbol) {
  }

  /** The tab's in-view search bar, driven from the chart's keys (type-to-search, Ctrl+F, F3, Escape). */
  interface SearchUi {
    void open(@NotNull String seedText);

    void nextMatch();

    void previousMatch();

    /** Closes the bar; false when it was not open (Escape then falls through to the range selection). */
    boolean close();
  }

  /** One instant mark on the events row — a user-emitted message with an optional color. */
  record TimeEvent(long timeUs, @NotNull String text, @Nullable Color color) {
  }

  /** One horizontal band between the ruler and the flame rows; the key names it for heights and ordering. */
  private sealed interface Band {
    String key();

    record CurveBand(CurveLane lane) implements Band {
      @Override
      public String key() {
        return "curve:" + lane.name();
      }
    }

    record MarkerBand(MarkerLane lane) implements Band {
      @Override
      public String key() {
        return "marker:" + lane.name();
      }
    }

    record EventsBand() implements Band {
      @Override
      public String key() {
        return "events";
      }
    }

    /** The run rows themselves; body height follows the stack depth, so the band moves but never resizes by hand. */
    record CallsBand() implements Band {
      @Override
      public String key() {
        return CALLS_BAND_KEY;
      }
    }
  }

  /** How a curve lane's values read; drives every formatted rendering of them. */
  enum CurveUnit {
    BYTES, PERCENT
  }

  /**
   * A named value-over-time band above the chart (memory pools, CPU/GPU
   * load, plots), drawn as a step curve scaled to its own peak: each
   * sample's value holds until the next one. An EMPTY lane still renders
   * (label plus a no-data placeholder) so an opened toggle never reads as
   * broken.
   */
  record CurveLane(@NotNull String name, @NotNull CurveUnit unit, @NotNull List<CurvePoint> points,
                   @NotNull Color color, double peak) {
    static CurveLane of(@NotNull String name, @NotNull CurveUnit unit,
                        @NotNull List<CurvePoint> points, @NotNull Color color) {
      double peak = points.stream().mapToDouble(CurvePoint::value).max().orElse(0);
      return new CurveLane(name, unit, points, color, peak);
    }

    String formatted(double value) {
      return unit == CurveUnit.PERCENT
             ? HaxeProfilerFormats.formatPercentValue(value)
             : HaxeProfilerFormats.formatBytes(value);
    }
  }

  /** Stable per-symbol pastels (light theme) with muted dark-theme partners. */
  private static final Color[] BOX_COLORS = {
    new JBColor(0xF2C09B, 0x8A6A46),
    new JBColor(0xAFD5AF, 0x5E7F5E),
    new JBColor(0xA8C8E8, 0x51687F),
    new JBColor(0xD8BAD8, 0x7A5E76),
    new JBColor(0xE0D898, 0x7F7A50),
    new JBColor(0xA8D8D8, 0x567878),
    new JBColor(0xE0B0A8, 0x7F5F58),
    new JBColor(0xC8C8C8, 0x666666)};
  private static final Color BOX_TEXT = new JBColor(0x1F1F1F, 0xE8E8E8);
  /** Translucent black reads as a border over every palette entry in both themes. */
  private static final Color BOX_BORDER = new Color(0, 0, 0, 90);
  private static final Color RULER_TEXT = new JBColor(0x808080, 0x999999);
  private static final Color RULER_TICK = new JBColor(0xC8C8C8, 0x515151);
  /** A step off the theme background, so header strips read as distinct without border lines. */
  private static final Color HEADER_BACKGROUND = new JBColor(0xEDEDED, 0x393B40);
  private static final Color SELECTION = new JBColor(0x3574F0, 0x66A3E0);
  private static final Color SELECTION_INNER = new JBColor(0xFFFFFF, 0x1E1E1E);
  /** The dragged range's translucent fill over its lane body. */
  private static final Color RANGE_FILL = new JBColor(new Color(0x35, 0x74, 0xF0, 45), new Color(0x66, 0xA3, 0xE0, 55));
  /** Boxes not matching the active search fade to this so the hits carry the color. */
  private static final Color SEARCH_DIMMED_BOX = new JBColor(0xE8E8E8, 0x3C3E42);
  /** Fallback mark color for events that sent none. */
  private static final Color EVENT_MARK = new JBColor(0x3574F0, 0x66A3E0);
  private static final long[] STEP_FACTORS = {1, 2, 5};
  private static final double ZOOM_STEP = 1.3;
  /** One zoom-button press doubles or halves the scale. */
  private static final double BUTTON_ZOOM_STEP = 2.0;
  /** Zoom-in floor: a 1 us tracy zone (the model's finest grain) spans 500 px — room for its label. */
  private static final double MIN_US_PER_PIXEL = 0.002;
  /** The horizontal scrollbar's fixed position count; values map linearly onto the session. */
  private static final int SCROLL_RESOLUTION = 1_000_000_000;

  private final Consumer<StackFrame> navigator;
  /** Told the selected run's call chain, outermost first; an empty list on deselection. */
  private final Consumer<List<FlameNode>> selectionListener;
  /** Told the selected marker-lane span; deselection goes through the run listener's empty list. */
  private final BiConsumer<MarkerLane, UsSpan> spanSelectionListener;
  /** Told the selected curve reading (a memory value at the clicked instant). */
  private final Consumer<CurveSelection> curveSelectionListener;
  /** Told the selected events-row mark. */
  private final Consumer<TimeEvent> eventSelectionListener;
  private FlameNode root = new FlameNode(null, 0, 0, 0, List.of(), false);
  private List<MarkerLane> lanes = List.of();
  private List<CurveLane> curveLanes = List.of();
  /** Time-ordered instant marks; the row shows whenever its toggle is on, placeholder included. */
  private List<TimeEvent> events = List.of();
  private boolean eventsRowVisible;
  /** Every visible band between the ruler and the flame rows, in the user's arrangement. */
  private List<Band> bands = List.of();
  /** The persisted band arrangement: keys in display order; bands with unknown keys keep their default position after these. */
  private List<String> bandOrder = new ArrayList<>();
  /** Keys of bands showing only their header, the body hidden by its collapse chevron. */
  private final Set<String> collapsedBands = new LinkedHashSet<>();
  /** The run rows' own collapse key — the calls area is a lane too, fixed at the bottom. */
  private static final String CALLS_BAND_KEY = "calls";
  /** False hides the calls lane entirely, header included (the Configure View checkbox). */
  private boolean callsVisible = true;
  /** Told the new arrangement after a reorder drop, a divider-resize release or a collapse toggle. */
  private @Nullable ViewPreferencesListener viewPreferenceListener;
  /** Opens the Configure View dialog; the tab owns it (lane visibility lives there). */
  private @Nullable Runnable configureViewOpener;
  /** What a run's count means in the shown capture; the tab overrides it for exact-zone data. */
  private IntFunction<String> runCountText =
    count -> HaxeProfilerBundle.message("haxe.profiler.callchart.samples", count);
  /** Index of the band being drag-reordered by its grip; -1 = none. */
  private int draggingBand = -1;
  private int dragY;
  private @Nullable TimeEvent selectedEvent;
  /** User-dragged band heights (curves, events row, marker lanes), kept by band key so they survive thread switches. */
  private final Map<String, Integer> bandHeights = new HashMap<>();
  private @Nullable FlameNode selected;
  private @Nullable UsSpan selectedSpan;
  private @Nullable MarkerLane selectedSpanLane;
  private @Nullable CurveSelection selectedCurve;
  /** Told a completed lane-range drag; null clears the range. */
  private @Nullable Consumer<@Nullable RangeSelection> rangeSelectionListener;
  /** The band a finished (or in-flight) range drag covers, keyed by name so it survives live lane rebuilds. */
  private @Nullable String rangeBandKey;
  private long rangeFromUs;
  private long rangeToUs;
  /** Armed on a lane-body press; a drag past the threshold turns it into a range selection. */
  private @Nullable String armedRangeBandKey;
  private long rangeAnchorUs;
  private int armedPressX;
  private boolean rangeDragging;
  /** The last lane click (or completed drag's start) — a shift+click on the same lane ranges from here. */
  private @Nullable String anchorBandKey;
  private long anchorUs;
  /** Lower-cased search text; while non-null, matching runs get accent borders and the rest dim. */
  private @Nullable String searchQuery;
  /** Per-symbol match verdicts for the current query — symbols repeat across thousands of boxes. */
  private final Map<String, Boolean> searchVerdicts = new HashMap<>();
  /** A jumped-to match not yet in the loaded tree; resolved to a node when a window load lands. */
  private @Nullable SearchMatch pendingSearchTarget;
  private @Nullable SearchUi searchUi;
  /** Key of the band (curve, events row or marker lane) being resized by a divider drag; null = none. */
  private @Nullable String resizingBandKey;
  private int resizeStartY;
  private int resizeStartHeight;
  private int rowCount;
  private @Nullable ViewListener viewListener;
  /** Microseconds one pixel covers; 0 = fit the whole capture to the viewport. */
  private double usPerPixel;
  /** The time at the panel's left edge while zoomed; fit mode pins it to the tree start. */
  private long viewStartUs;
  private @Nullable JScrollBar horizontalScrollBar;
  /** Guards against the scrollbar's own change events while this panel updates its model. */
  private boolean syncingScrollBar;
  /**
   * While set, every live-data update pins the view's right edge to the
   * session's end. ANY user view gesture — wheel zoom/pan, the scrollbar,
   * a minimap jump, the zoom buttons — disarms it: data keeps appending,
   * the viewport stays where the user put it. Scrolling flush to the right
   * end re-arms it.
   */
  private boolean followLive;

  HaxeCallChartPanel(@NotNull Consumer<StackFrame> navigator,
                     @NotNull Consumer<List<FlameNode>> selectionListener,
                     @NotNull BiConsumer<MarkerLane, UsSpan> spanSelectionListener,
                     @NotNull Consumer<CurveSelection> curveSelectionListener,
                     @NotNull Consumer<TimeEvent> eventSelectionListener) {
    this.navigator = navigator;
    this.selectionListener = selectionListener;
    this.spanSelectionListener = spanSelectionListener;
    this.curveSelectionListener = curveSelectionListener;
    this.eventSelectionListener = eventSelectionListener;
    setOpaque(false);
    setFocusable(true); // arrow keys walk the selection (see installKeyboardNavigation)
    installKeyboardNavigation();
    addKeyListener(new KeyAdapter() {
      @Override
      public void keyTyped(KeyEvent event) {
        // type-to-search: a plain printable character opens the search bar seeded with it
        char typed = event.getKeyChar();
        int handledModifiers = KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK | KeyEvent.META_DOWN_MASK;
        if ((event.getModifiersEx() & handledModifiers) == 0 && !Character.isISOControl(typed)) {
          openSearch(String.valueOf(typed));
        }
      }
    });
    ToolTipManager.sharedInstance().registerComponent(this);
    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent event) {
        if (!SwingUtilities.isLeftMouseButton(event)) return;
        requestFocusInWindow();
        if (event.getClickCount() == 1) {
          // a click in a divider's grip zone is a (no-op) resize, not a toggle of the band below
          int headerBand = dividerKeyAt(event.getY()) == null ? headerBandAt(event.getPoint()) : -1;
          if (headerBand >= 0) {
            toggleCollapsed(bands.get(headerBand).key());
            return;
          }
          if (event.isShiftDown() && extendRangeTo(event.getPoint())) {
            return;
          }
          selectAt(event.getPoint());
        }
        else if (event.getClickCount() == 2) {
          navigateAt(event.getPoint());
        }
      }

      @Override
      public void mousePressed(MouseEvent event) {
        if (event.isPopupTrigger()) {
          showContextMenu(event);
          return;
        }
        String divider = dividerKeyAt(event.getY());
        if (divider != null) {
          resizingBandKey = divider;
          resizeStartY = event.getY();
          resizeStartHeight = bodyHeight(divider);
          return;
        }
        if (event.getX() <= gripWidth()) {
          draggingBand = bandIndexAt(event.getY());
          dragY = event.getY();
          return;
        }
        if (SwingUtilities.isLeftMouseButton(event)) {
          armRangeDrag(event);
        }
      }

      @Override
      public void mouseReleased(MouseEvent event) {
        if (resizingBandKey != null) {
          resizingBandKey = null;
          fireViewPreferencesChanged();
        }
        if (draggingBand >= 0) {
          dropDraggedBand(event.getY());
        }
        finishRangeDrag();
        if (event.isPopupTrigger()) {
          showContextMenu(event);
        }
      }
    });
    addMouseMotionListener(new MouseAdapter() {
      @Override
      public void mouseDragged(MouseEvent event) {
        if (resizingBandKey != null) {
          resizeBand(resizingBandKey, resizeStartHeight + event.getY() - resizeStartY);
        }
        else if (draggingBand >= 0) {
          dragY = event.getY();
          repaint();
        }
        else if (armedRangeBandKey != null) {
          dragRangeTo(event.getX());
        }
      }

      @Override
      public void mouseMoved(MouseEvent event) {
        if (dividerKeyAt(event.getY()) != null) {
          setCursor(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
        }
        else if (event.getX() <= gripWidth() && bandIndexAt(event.getY()) >= 0) {
          setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }
        else if (headerBandAt(event.getPoint()) >= 0) {
          setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        else {
          setCursor(Cursor.getDefaultCursor());
        }
      }
    });
    addMouseWheelListener(this::onWheel);
    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent event) {
        if (usPerPixel > 0) {
          setViewStart(viewStartUs);
        }
        syncScrollBar();
        fireViewChanged();
      }
    });
  }

  /**
   * The scroller for the virtual horizontal axis, placed by the tab below
   * the chart's scroll pane (whose own horizontal bar stays off).
   */
  @NotNull
  JScrollBar createHorizontalScrollBar() {
    JScrollBar scrollBar = new JScrollBar(Adjustable.HORIZONTAL);
    scrollBar.addAdjustmentListener(event -> {
      if (!syncingScrollBar) {
        applyScrollValue(event.getValue());
      }
    });
    horizontalScrollBar = scrollBar;
    syncScrollBar();
    return scrollBar;
  }

  void setTree(@NotNull FlameNode tree) {
    root = tree;
    rowCount = ProfilerTimeline.treeDepth(tree);
    usPerPixel = 0;
    viewStartUs = tree.startUs();
    clearPointSelections();
    rangeBandKey = null;
    anchorBandKey = null;
    pendingSearchTarget = null;
    selectionListener.accept(List.of());
    syncScrollBar();
    revalidate();
    repaint();
    fireViewChanged();
  }

  /**
   * Swaps in a reloaded tree (a finer or shifted window over the same
   * session) WITHOUT touching the view; the selected run carries over when
   * the new tree still holds a node with the same bounds and symbol.
   */
  void updateTree(@NotNull FlameNode tree) {
    FlameNode previousSelection = selected;
    root = tree;
    rowCount = ProfilerTimeline.treeDepth(tree);
    selected = previousSelection == null ? null : reselect(previousSelection);
    if (previousSelection != null && selected == null) {
      selectionListener.accept(List.of());
    }
    trySelectSearchTarget(); // a jumped-to match's window may have just loaded
    revalidate();
    repaint();
  }

  /** The new tree's node matching the old selection's bounds and symbol, or null. */
  @Nullable
  private FlameNode reselect(FlameNode old) {
    long midUs = (old.startUs() + old.endUs()) / 2;
    FlameNode candidate = null;
    FlameNode node = root;
    while (node != null) {
      FlameNode next = null;
      for (FlameNode child : node.children()) {
        if (midUs >= child.startUs() && midUs < child.endUs()) {
          next = child;
          break;
        }
      }
      if (next != null && next.startUs() == old.startUs() && next.endUs() == old.endUs()
          && sameSymbol(next, old)) {
        candidate = next;
      }
      node = next;
    }
    return candidate;
  }

  private static boolean sameSymbol(FlameNode left, FlameNode right) {
    String a = left.frame() == null ? null : left.frame().symbol();
    String b = right.frame() == null ? null : right.frame().symbol();
    return Objects.equals(a, b);
  }

  void setViewListener(@Nullable ViewListener listener) {
    viewListener = listener;
    fireViewChanged();
  }

  /** Starts following live data: the right edge tracks the session end until the user moves the view. */
  void setFollowLive(boolean follow) {
    followLive = follow;
  }

  /**
   * The live starting view: a sliding window of {@code spanUs} pinned to
   * the tail — paint and windowed tree loads then cost the window, not the
   * growing session. Needs a laid-out panel; call again when width was 0.
   */
  boolean startLiveWindow(long spanUs) {
    if (getWidth() <= 0) return false;
    followLive = true;
    usPerPixel = Math.max(spanUs / (double)getWidth(), MIN_US_PER_PIXEL);
    setViewStart(root.startUs() + root.durationUs());
    syncScrollBar();
    repaint();
    fireViewChanged();
    return true;
  }

  /**
   * Live data landed (a refreshed tree and lanes): while following, the
   * view's right edge snaps to the new session end; a fit-mode view
   * already shows everything and just repaints.
   */
  void liveDataAppended() {
    if (followLive && usPerPixel > 0) {
      setViewStart(root.startUs() + root.durationUs());
      syncScrollBar();
      fireViewChanged();
    }
    revalidate();
    repaint();
  }

  /** Shows [startUs, endUs]; a span covering the whole session falls back to fit. */
  void setView(long startUs, long endUs) {
    followLive = false;
    long spanUs = Math.max(endUs - startUs, 1);
    int width = Math.max(1, getWidth());
    if (spanUs >= root.durationUs()) {
      usPerPixel = 0;
      viewStartUs = root.startUs();
    }
    else {
      usPerPixel = Math.max(spanUs / (double)width, MIN_US_PER_PIXEL);
      setViewStart(startUs);
    }
    syncScrollBar();
    repaint();
    fireViewChanged();
  }

  private void fireViewChanged() {
    if (viewListener == null) return;
    boolean wholeSession = usPerPixel <= 0;
    long visibleUs = wholeSession ? Math.max(root.durationUs(), 1) : zoomedVisibleUs();
    viewListener.viewChanged(viewLeftUs(), visibleUs, wholeSession);
  }

  /** Replaces the marker lanes; a selected span carries over when the same-named lane still holds an equal span (live refreshes). */
  void setLanes(@NotNull List<MarkerLane> lanes) {
    MarkerLane previousLane = selectedSpanLane;
    UsSpan previousSpan = selectedSpan;
    this.lanes = lanes;
    clearSpanSelection();
    if (previousLane != null && previousSpan != null) {
      for (MarkerLane lane : lanes) {
        int index = lane.name().equals(previousLane.name()) ? lane.spans().indexOf(previousSpan) : -1;
        if (index >= 0) {
          // the NEW list's instance - the painters match the selection by identity
          selectedSpanLane = lane;
          selectedSpan = lane.spans().get(index);
          break;
        }
      }
      if (selectedSpan == null) {
        selectionListener.accept(List.of());
      }
    }
    rebuildBands();
  }

  /** Replaces the curve lanes; a selected reading rebinds to the same-named lane's data (live refreshes). */
  void setCurveLanes(@NotNull List<CurveLane> lanes) {
    CurveSelection previous = selectedCurve;
    curveLanes = lanes;
    clearCurveSelection();
    if (previous != null) {
      for (CurveLane lane : lanes) {
        if (lane.name().equals(previous.lane().name())) {
          CurvePoint lastChange = lastChangeAt(lane, previous.instantUs());
          if (lastChange != null) {
            selectedCurve = new CurveSelection(lane, previous.instantUs(), lastChange);
          }
          break;
        }
      }
      if (selectedCurve == null) {
        selectionListener.accept(List.of());
      }
    }
    rebuildBands();
  }

  /** Time-ordered instant marks for the events row; {@code rowVisible} keeps the row (with a placeholder) even when empty. */
  void setEvents(@NotNull List<TimeEvent> events, boolean rowVisible) {
    TimeEvent previous = selectedEvent;
    this.events = events;
    this.eventsRowVisible = rowVisible;
    // the NEW list's instance - the painter matches the selection by identity
    int index = previous == null ? -1 : events.indexOf(previous);
    selectedEvent = index >= 0 ? events.get(index) : null;
    if (previous != null && selectedEvent == null) {
      selectionListener.accept(List.of());
    }
    rebuildBands();
  }

  /**
   * Seeds the persisted arrangement (band order, heights, collapsed bands)
   * and the listener told about changes; call once before the first lane
   * data.
   */
  void setViewPreferences(@NotNull List<String> order, @NotNull Map<String, Integer> heights,
                          @NotNull List<String> collapsed, @NotNull ViewPreferencesListener listener) {
    bandOrder = new ArrayList<>(order);
    bandHeights.putAll(heights);
    collapsedBands.addAll(collapsed);
    viewPreferenceListener = listener;
  }

  /** Wires the context menu's Configure View entry to the tab's dialog. */
  void setConfigureViewOpener(@NotNull Runnable opener) {
    configureViewOpener = opener;
  }

  /** What a run's count means in the shown capture (samples vs invocations), for the hover tooltip. */
  void setRunCountText(@NotNull IntFunction<String> formatter) {
    runCountText = formatter;
  }

  /** Told a completed lane-range drag (curve or marker lane); null when the range is cleared. */
  void setRangeSelectionListener(@NotNull Consumer<@Nullable RangeSelection> listener) {
    rangeSelectionListener = listener;
  }

  /** Wires the tab's in-view search bar; the chart's keys drive it (type-to-search, Ctrl+F, F3, Escape). */
  void setSearchUi(@NotNull SearchUi ui) {
    searchUi = ui;
  }

  /** The active search text (null or blank = off): matching runs get accent borders, the rest dim. */
  void setSearchQuery(@Nullable String query) {
    searchQuery = query == null || query.isBlank() ? null : query.toLowerCase(Locale.ROOT);
    searchVerdicts.clear();
    if (searchQuery == null) {
      pendingSearchTarget = null;
    }
    repaint();
  }

  /**
   * Jumps to a match: scrolls it into view, or — when it would paint too
   * narrow to read its name — zooms so it spans a quarter of the viewport,
   * centered. The selection lands once the node is in the loaded tree (a
   * windowed source loads it only after the jump).
   */
  void showSearchMatch(@NotNull SearchMatch match) {
    long durationUs = Math.max(match.endUs() - match.startUs(), 1);
    if (durationUs / scale() < JBUI.scale(120)) {
      followLive = false;
      int viewportWidth = Math.max(getWidth(), 1);
      zoomTo(durationUs / (0.25 * viewportWidth), (match.startUs() + match.endUs()) / 2, viewportWidth / 2);
    }
    else {
      revealTime(match.startUs(), match.endUs());
    }
    pendingSearchTarget = match;
    trySelectSearchTarget();
    repaint();
  }

  /** Resolves the pending match against the loaded tree; keeps waiting while the window has not caught up. */
  private void trySelectSearchTarget() {
    SearchMatch target = pendingSearchTarget;
    if (target == null) return;
    FlameNode node = findSearchNode(target);
    if (node == null) return;
    pendingSearchTarget = null;
    clearPointSelections();
    selected = node;
    selectionListener.accept(pathOfNode(node));
  }

  /** Containment descent to the match's depth; the symbol must agree (bounds may differ by rounding). */
  private @Nullable FlameNode findSearchNode(SearchMatch match) {
    long midUs = (match.startUs() + match.endUs()) / 2;
    FlameNode node = root;
    for (int depth = 0; depth <= match.depth(); depth++) {
      FlameNode within = null;
      for (FlameNode child : node.children()) {
        if (child.startUs() <= midUs && midUs < Math.max(child.endUs(), child.startUs() + 1)) {
          within = child;
          break;
        }
      }
      if (within == null) return null;
      node = within;
    }
    boolean sameSymbol = node.frame() != null && node.frame().symbol().equals(match.symbol());
    return sameSymbol && !node.idle() ? node : null;
  }

  private boolean matchesSearch(String symbol) {
    String needle = searchQuery;
    if (needle == null) return false;
    return searchVerdicts.computeIfAbsent(symbol, key -> key.toLowerCase(Locale.ROOT).contains(needle));
  }

  /** Shows or hides the calls lane (header included); hiding clears a selected run. */
  void setCallsVisible(boolean visible) {
    if (callsVisible == visible) return;
    callsVisible = visible;
    if (!visible && selected != null) {
      selected = null;
      selectionListener.accept(List.of());
    }
    rebuildBands();
  }

  /** The y where the run rows start, or -1 while the calls lane is hidden or collapsed to its header. */
  private int callsBodyTop() {
    for (int index = 0; index < bands.size(); index++) {
      if (bands.get(index) instanceof Band.CallsBand) {
        return collapsedBands.contains(CALLS_BAND_KEY) ? -1 : bandTop(index) + headerHeight();
      }
    }
    return -1;
  }

  /** The visible bands in default structural order, then rearranged by the persisted key order. */
  private void rebuildBands() {
    List<Band> rebuilt = new ArrayList<>();
    for (CurveLane lane : curveLanes) {
      rebuilt.add(new Band.CurveBand(lane));
    }
    if (eventsRowVisible) {
      rebuilt.add(new Band.EventsBand());
    }
    for (MarkerLane lane : lanes) {
      rebuilt.add(new Band.MarkerBand(lane));
    }
    if (callsVisible) {
      rebuilt.add(new Band.CallsBand());
    }
    // stable sort: bands the order does not know keep their default position after the known ones
    rebuilt.sort(Comparator.comparingInt(band -> {
      int index = bandOrder.indexOf(band.key());
      return index >= 0 ? index : bandOrder.size();
    }));
    bands = rebuilt;
    draggingBand = -1;
    revalidate();
    repaint();
  }

  private void fireViewPreferencesChanged() {
    if (viewPreferenceListener != null) {
      viewPreferenceListener.changed(List.copyOf(bandOrder), Map.copyOf(bandHeights), List.copyOf(collapsedBands));
    }
  }

  private void clearSpanSelection() {
    selectedSpan = null;
    selectedSpanLane = null;
  }

  /** Clears the selected run, span, curve reading and event; only one of them shows at a time. */
  private void clearPointSelections() {
    selected = null;
    clearSpanSelection();
    clearCurveSelection();
    selectedEvent = null;
  }

  @Override
  protected void paintComponent(Graphics graphics) {
    Graphics2D g = (Graphics2D)graphics.create();
    try {
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g.setFont(JBUI.Fonts.smallFont());
      Rectangle clip = g.getClipBounds();
      if (clip == null) clip = new Rectangle(0, 0, getWidth(), getHeight());
      paintRuler(g, clip);
      paintBands(g, clip);
      paintRangeSelection(g);
    }
    finally {
      g.dispose();
    }
  }

  /** The dragged range over its lane's body: a translucent fill with accent edges. */
  private void paintRangeSelection(Graphics2D g) {
    if (rangeBandKey == null || collapsedBands.contains(rangeBandKey)) return;
    int index = bands.indexOf(bandByKey(rangeBandKey));
    if (index < 0) return;
    int x0 = xOf(rangeFromUs);
    int x1 = Math.max(xOf(rangeToUs), x0 + 1);
    int top = bandTop(index) + headerHeight();
    int height = bodyHeight(rangeBandKey);
    g.setColor(RANGE_FILL);
    g.fillRect(x0, top, x1 - x0, height);
    g.setColor(SELECTION);
    g.drawLine(x0, top, x0, top + height - 1);
    g.drawLine(x1 - 1, top, x1 - 1, top + height - 1);
  }

  private void paintNode(Graphics2D g, FlameNode node, int row, int baseY, Rectangle clip) {
    int x0 = xOf(node.startUs());
    int x1 = Math.max(xOf(node.endUs()), x0 + 1);
    if (x1 <= clip.x || x0 >= clip.x + clip.width) return;
    int y = baseY + row * rowHeight();
    if (y >= clip.y + clip.height) return;

    if (!node.idle() && y + rowHeight() > clip.y) {
      paintBox(g, node, x0, y, x1 - x0);
    }
    if (x1 - x0 <= 1) return; // the callees share this pixel column - nothing more to show
    for (FlameNode child : node.children()) {
      paintNode(g, child, row + 1, baseY, clip);
    }
  }

  private void paintBox(Graphics2D g, FlameNode node, int x, int y, int width) {
    int height = rowHeight() - 1;
    String symbol = node.frame() == null ? "" : node.frame().symbol();
    boolean searchHit = searchQuery != null && matchesSearch(symbol);
    boolean dimmed = searchQuery != null && !searchHit;
    g.setColor(dimmed ? SEARCH_DIMMED_BOX : colorOf(node));
    g.fillRect(x, y, width, height);
    if (width > 2) {
      g.setColor(BOX_BORDER);
      g.drawRect(x, y, width - 1, height - 1);
    }
    if (searchHit) {
      g.setColor(SELECTION);
      g.drawRect(x, y, Math.max(width - 1, 1), height - 1);
    }
    if (node == selected) {
      paintSelection(g, x, y, width, height);
    }

    if (!symbol.isEmpty() && width > JBUI.scale(30)) {
      Shape outerClip = g.getClip();
      g.clipRect(x + 2, y, width - 4, height);
      g.setColor(BOX_TEXT);
      FontMetrics metrics = g.getFontMetrics();
      g.drawString(symbol, x + JBUI.scale(4), y + (height + metrics.getAscent() - metrics.getDescent()) / 2);
      g.setClip(outerClip);
    }
  }

  /** The selected run: a thick accent frame with an inner contrast line, widened so even a sliver stays findable. */
  private static void paintSelection(Graphics2D g, int x, int y, int width, int height) {
    int frameWidth = Math.max(width, JBUI.scale(3));
    g.setColor(SELECTION);
    g.drawRect(x, y, frameWidth - 1, height - 1);
    g.drawRect(x + 1, y + 1, frameWidth - 3, height - 3);
    if (frameWidth > JBUI.scale(6)) {
      g.setColor(SELECTION_INNER);
      g.drawRect(x + 2, y + 2, frameWidth - 5, height - 5);
    }
  }

  private void paintRuler(Graphics2D g, Rectangle clip) {
    long stepUs = niceStepUs(scale() * JBUI.scale(80));
    int height = rulerHeight();
    g.setColor(RULER_TICK);
    g.drawLine(clip.x, height - 1, clip.x + clip.width, height - 1);

    // ticks anchor to the CAPTURE's zero, not the thread's first sample, so
    // the axis matches the times shown in the run details and the timeline
    long firstTickUs = Math.max(0, timeAt(clip.x)) / stepUs * stepUs;
    for (long tickUs = firstTickUs; ; tickUs += stepUs) {
      int x = xOf(tickUs);
      if (x > clip.x + clip.width) break;
      g.setColor(RULER_TICK);
      g.drawLine(x, height - JBUI.scale(5), x, height - 1);
      g.setColor(RULER_TEXT);
      g.drawString(formatTick(tickUs, stepUs), x + JBUI.scale(3), height - JBUI.scale(6));
    }
  }

  /**
   * Every band in the user's arrangement: a header strip (label plus the
   * collapse chevron) above the body — curve bands scaled to their own
   * peak, the events row, marker-span lanes — each with its reorder grip on
   * the left and its bottom divider doubling as the resize grip. A
   * collapsed band shows only its header. During a grip drag an accent
   * line marks where the band would land.
   */
  private void paintBands(Graphics2D g, Rectangle clip) {
    for (int index = 0; index < bands.size(); index++) {
      Band band = bands.get(index);
      int top = bandTop(index);
      int height = bandHeight(band.key());
      paintBandHeader(g, band, top, clip);
      if (!collapsedBands.contains(band.key())) {
        int bodyTop = top + headerHeight();
        int bodyHeight = height - headerHeight() - 1;
        switch (band) {
          case Band.CurveBand(CurveLane lane) -> paintCurveBand(g, lane, bodyTop, bodyHeight, clip);
          case Band.EventsBand ignored -> paintEventsBand(g, bodyTop, bodyHeight, clip);
          case Band.MarkerBand(MarkerLane lane) -> paintMarkerBand(g, lane, bodyTop, bodyHeight, clip);
          case Band.CallsBand ignored -> paintCallsBand(g, bodyTop, bodyHeight, clip);
        }
      }
      paintGrip(g, top, height - 1);
    }
    if (draggingBand >= 0) {
      int gap = dropGapAt(dragY);
      g.setColor(SELECTION);
      int y = bandTop(gap);
      g.fillRect(clip.x, y - 1, clip.width, JBUI.scale(2));
    }
  }

  /** The band's title strip: a tinted background, label left, expansion chevron right; the WHOLE strip is the collapse toggle. */
  private void paintBandHeader(Graphics2D g, Band band, int top, Rectangle clip) {
    g.setColor(HEADER_BACKGROUND);
    g.fillRect(clip.x, top, clip.width, headerHeight());
    FontMetrics metrics = g.getFontMetrics();
    g.setColor(RULER_TEXT);
    int baseline = top + (headerHeight() + metrics.getAscent() - metrics.getDescent()) / 2;
    g.drawString(bandLabel(band), labelX(), baseline);
    Icon chevron = collapsedBands.contains(band.key()) ? AllIcons.General.ArrowRight : AllIcons.General.ArrowDown;
    chevron.paintIcon(this, g, chevronX(), top + (headerHeight() - chevron.getIconHeight()) / 2);
  }

  private String bandLabel(Band band) {
    return switch (band) {
      case Band.CurveBand(CurveLane lane) -> curveBandLabel(lane);
      case Band.MarkerBand(MarkerLane lane) -> lane.name();
      case Band.EventsBand ignored -> HaxeProfilerBundle.message("haxe.profiler.callchart.lane.events");
      case Band.CallsBand ignored -> HaxeProfilerBundle.message("haxe.profiler.callchart.lane.calls");
    };
  }

  private static String curveBandLabel(CurveLane lane) {
    if (lane.points().isEmpty()) return lane.name();
    return HaxeProfilerBundle.message("haxe.profiler.callchart.lane.peak", lane.name(), lane.formatted(lane.peak()));
  }

  /** The run rows, the outermost call on top; an empty tree keeps the placeholder like any other lane. */
  private void paintCallsBand(Graphics2D g, int bodyTop, int bodyHeight, Rectangle clip) {
    if (root.children().isEmpty()) {
      paintNoData(g, bodyTop, bodyHeight);
      return;
    }
    for (FlameNode child : root.children()) {
      paintNode(g, child, 0, bodyTop, clip);
    }
  }

  /** Padded well clear of the scroll pane's vertical scrollbar, which sits over the panel's right edge. */
  private int chevronX() {
    return Math.max(0, getWidth() - JBUI.scale(40));
  }

  /** The left-edge drag handle: two dotted columns, the visual promise that the band can be rearranged. */
  private void paintGrip(Graphics2D g, int top, int height) {
    g.setColor(RULER_TEXT);
    int dot = JBUI.scale(2);
    int centerY = top + height / 2;
    for (int column = 0; column < 2; column++) {
      int x = JBUI.scale(3) + column * (dot + 1);
      for (int row = -1; row <= 1; row++) {
        g.fillOval(x, centerY + row * (dot + 2) - dot / 2, dot, dot);
      }
    }
  }

  private void paintCurveBand(Graphics2D g, CurveLane lane, int top, int height, Rectangle clip) {
    paintCurve(g, lane, top, height, clip);
    if (selectedCurve != null && selectedCurve.lane() == lane) {
      paintCurveSelection(g, selectedCurve, top, height);
    }
    if (lane.points().isEmpty()) {
      paintNoData(g, top, height);
    }
  }

  private static int labelX() {
    return JBUI.scale(12);
  }

  /** Centered placeholder for a lane the user opened without the capture carrying its data. */
  private void paintNoData(Graphics2D g, int top, int height) {
    FontMetrics metrics = g.getFontMetrics();
    String text = HaxeProfilerBundle.message("haxe.profiler.callchart.no.data");
    g.setColor(RULER_TEXT);
    int x = Math.max(JBUI.scale(4), (getWidth() - metrics.stringWidth(text)) / 2);
    g.drawString(text, x, top + (height + metrics.getAscent() - metrics.getDescent()) / 2);
  }

  /** An accent line at the CLICKED instant with a dot at the value holding there. */
  private void paintCurveSelection(Graphics2D g, CurveSelection selection, int top, int height) {
    CurveLane lane = selection.lane();
    if (lane.peak() <= 0) return;
    int x = xOf(selection.instantUs());
    int dot = JBUI.scale(3);
    double value = selection.lastChange().value();
    int valueY = top + height - (int)Math.ceil(value / lane.peak() * (height - JBUI.scale(2)));
    g.setColor(SELECTION);
    g.drawLine(x, top, x, top + height - 1);
    g.fillOval(x - dot, valueY - dot, dot * 2, dot * 2);
  }

  /**
   * Step semantics: a sample's value holds until the next sample. Sub-pixel
   * steps coalesce keeping the tallest, so allocation spikes stay visible at
   * any zoom; the last sample's value holds to the right edge.
   */
  private void paintCurve(Graphics2D g, CurveLane lane, int top, int height, Rectangle clip) {
    List<CurvePoint> points = lane.points();
    if (points.isEmpty() || lane.peak() <= 0) return;
    g.setColor(lane.color());
    int baseline = top + height;
    int maxBarHeight = height - JBUI.scale(2);
    int clipRight = clip.x + clip.width;

    int i = lastIndexAtOrBefore(points, timeAt(clip.x));
    while (i < points.size()) {
      int x0 = xOf(points.get(i).timeUs());
      if (x0 > clipRight) break;
      double value = points.get(i).value();
      int j = i + 1;
      int x1 = j < points.size() ? xOf(points.get(j).timeUs()) : clipRight;
      while (j < points.size() && x1 <= x0 + 1) {
        value = Math.max(value, points.get(j).value());
        j++;
        x1 = j < points.size() ? xOf(points.get(j).timeUs()) : clipRight;
      }
      int barHeight = (int)Math.ceil(value / lane.peak() * maxBarHeight);
      if (barHeight > 0) {
        g.fillRect(Math.max(x0, clip.x), baseline - barHeight, Math.max(1, x1 - Math.max(x0, clip.x)), barHeight);
      }
      i = j;
    }
  }

  /** The greatest index whose time is at or before {@code timeUs}; 0 when all lie after it. */
  private static int lastIndexAtOrBefore(List<CurvePoint> points, long timeUs) {
    int low = 0;
    int high = points.size() - 1;
    int result = 0;
    while (low <= high) {
      int middle = (low + high) >>> 1;
      if (points.get(middle).timeUs() <= timeUs) {
        result = middle;
        low = middle + 1;
      }
      else {
        high = middle - 1;
      }
    }
    return result;
  }

  /** The events row: one small diamond per mark, in the event's own color when it sent one. */
  private void paintEventsBand(Graphics2D g, int y, int height, Rectangle clip) {
    int centerY = y + height / 2;
    int radius = JBUI.scale(3);

    int from = firstEventAtOrAfter(timeAt(clip.x) - 1);
    for (int i = from; i < events.size(); i++) {
      TimeEvent event = events.get(i);
      int x = xOf(event.timeUs());
      if (x > clip.x + clip.width) break;
      g.setColor(event.color() != null ? event.color() : EVENT_MARK);
      g.fillOval(x - radius, centerY - radius, radius * 2, radius * 2);
      if (event == selectedEvent) {
        g.setColor(SELECTION);
        g.drawOval(x - radius - 2, centerY - radius - 2, radius * 2 + 4, radius * 2 + 4);
      }
    }
    if (events.isEmpty()) {
      paintNoData(g, y, height);
    }
  }

  /** The first event index whose time is at or after {@code timeUs}. */
  private int firstEventAtOrAfter(long timeUs) {
    int low = 0;
    int high = events.size() - 1;
    int result = events.size();
    while (low <= high) {
      int middle = (low + high) >>> 1;
      if (events.get(middle).timeUs() >= timeUs) {
        result = middle;
        high = middle - 1;
      }
      else {
        low = middle + 1;
      }
    }
    return result;
  }

  /** The mark within a few pixels of the point on the events row, or null. */
  @Nullable
  private TimeEvent eventAt(Point point) {
    if (events.isEmpty() || !onEventsBand(point.y)) return null;
    int grip = JBUI.scale(4);
    TimeEvent best = null;
    int bestDistance = grip + 1;
    int from = Math.max(firstEventAtOrAfter(timeAt(point.x - grip)) - 1, 0);
    for (int i = from; i < events.size(); i++) {
      int distance = Math.abs(xOf(events.get(i).timeUs()) - point.x);
      if (distance < bestDistance) {
        bestDistance = distance;
        best = events.get(i);
      }
      if (xOf(events.get(i).timeUs()) > point.x + grip) break;
    }
    return best;
  }

  private void paintMarkerBand(Graphics2D g, MarkerLane lane, int y, int height, Rectangle clip) {
    List<UsSpan> spans = lane.spans();
    for (int i = 0; i < spans.size(); i++) {
      UsSpan span = spans.get(i);
      int x0 = xOf(span.startUs());
      int x1 = Math.max(xOf(span.endUs()), x0 + 1);
      if (x1 <= clip.x || x0 >= clip.x + clip.width) continue;
      g.setColor(i % 2 == 0 ? lane.evenColor() : lane.oddColor());
      g.fillRect(x0, y, x1 - x0, height);
      if (span == selectedSpan && lane == selectedSpanLane) {
        paintSelection(g, x0, y, x1 - x0, height);
      }
    }
    if (spans.isEmpty()) {
      paintNoData(g, y, height);
    }
  }

  private static Color colorOf(FlameNode node) {
    StackFrame frame = node.frame();
    int index = frame == null ? BOX_COLORS.length - 1 : Math.floorMod(frame.symbol().hashCode(), BOX_COLORS.length);
    return BOX_COLORS[index];
  }

  @Override
  public @Nullable String getToolTipText(@NotNull MouseEvent event) {
    TimeEvent mark = eventAt(event.getPoint());
    if (mark != null) {
      return mark.text();
    }
    CurveLane curveLane = curveLaneAt(event.getPoint());
    if (curveLane != null && !curveLane.points().isEmpty()) {
      int index = lastIndexAtOrBefore(curveLane.points(), timeAt(event.getX()));
      return curveLane.name() + " — " + curveLane.formatted(curveLane.points().get(index).value());
    }
    MarkerLane lane = laneAt(event.getPoint());
    if (lane != null) {
      UsSpan span = spanAt(lane, timeAt(event.getX()));
      return span == null ? null : lane.name() + " — " + HaxeProfilerFormats.formatUs(span.durationUs());
    }
    FlameNode node = nodeAt(event.getPoint());
    if (node == null || node.frame() == null) return null;
    return node.frame().symbol() + " — " + HaxeProfilerFormats.formatUs(node.durationUs()) + ", "
           + runCountText.apply(node.samples());
  }

  private void navigateAt(Point point) {
    FlameNode node = nodeAt(point);
    StackFrame frame = node == null ? null : node.frame();
    if (frame != null) {
      navigator.accept(frame);
    }
  }

  /**
   * Selects the clicked run, marker-lane span or curve sample and reports
   * it; empty space, an idle filler or the gap between spans clears the
   * selection.
   */
  private void selectAt(Point point) {
    clearRangeSelection(); // a plain click hands the details panel back to point selections
    rememberRangeAnchor(point);
    clearPointSelections();
    if (onEventsBand(point.y)) {
      TimeEvent event = eventAt(point);
      selectedEvent = event;
      if (event != null) {
        eventSelectionListener.accept(event);
      }
      else {
        selectionListener.accept(List.of());
      }
      repaint();
      return;
    }

    CurveLane curveLane = curveLaneAt(point);
    if (curveLane != null) {
      long instantUs = timeAt(point.x);
      CurvePoint lastChange = lastChangeAt(curveLane, instantUs);
      if (lastChange == null) {
        // before the curve's first point there is no reading to select
        selectionListener.accept(List.of());
      }
      else {
        selectedCurve = new CurveSelection(curveLane, instantUs, lastChange);
        curveSelectionListener.accept(selectedCurve);
      }
      repaint();
      return;
    }

    MarkerLane lane = laneAt(point);
    if (lane != null) {
      UsSpan span = spanAt(lane, timeAt(point.x));
      selectedSpan = span;
      selectedSpanLane = span == null ? null : lane;
      if (span != null) {
        spanSelectionListener.accept(lane, span);
      }
      else {
        selectionListener.accept(List.of());
      }
      repaint();
      return;
    }

    List<FlameNode> path = pathAt(point);
    selected = path.isEmpty() ? null : path.getLast();
    selectionListener.accept(path);
    repaint();
  }

  private void clearCurveSelection() {
    selectedCurve = null;
  }

  /** A lane-body click becomes the anchor a later shift+click on the same lane ranges from. */
  private void rememberRangeAnchor(Point point) {
    int index = bandBodyIndexAt(point.y);
    Band band = index < 0 ? null : bands.get(index);
    if (band instanceof Band.CurveBand || band instanceof Band.MarkerBand) {
      anchorBandKey = band.key();
      anchorUs = Math.max(timeAt(point.x), 0);
    }
  }

  /**
   * Shift+click: the range from the last lane click (or a completed
   * drag's start) to this point. False — no anchor on this lane — falls
   * back to a plain click.
   */
  private boolean extendRangeTo(Point point) {
    int index = bandBodyIndexAt(point.y);
    if (index < 0 || anchorBandKey == null) return false;
    Band band = bands.get(index);
    if (!band.key().equals(anchorBandKey)) return false;
    long clickUs = Math.max(timeAt(point.x), 0);
    if (clickUs == anchorUs) return false;
    rangeBandKey = band.key();
    rangeFromUs = Math.min(anchorUs, clickUs);
    rangeToUs = Math.max(anchorUs, clickUs);
    // the range replaces any point selection; both would compete for the details panel
    clearPointSelections();
    fireRangeSelected();
    repaint();
    return true;
  }

  /** Arms a possible range drag when the press lands on a curve or marker lane's body. */
  private void armRangeDrag(MouseEvent event) {
    int index = bandBodyIndexAt(event.getY());
    if (index < 0) return;
    Band band = bands.get(index);
    if (!(band instanceof Band.CurveBand) && !(band instanceof Band.MarkerBand)) return;
    armedRangeBandKey = band.key();
    rangeAnchorUs = Math.max(timeAt(event.getX()), 0);
    armedPressX = event.getX();
  }

  private void dragRangeTo(int x) {
    if (!rangeDragging && Math.abs(x - armedPressX) < JBUI.scale(4)) return;
    if (!rangeDragging) {
      rangeDragging = true;
      rangeBandKey = armedRangeBandKey;
      // the range replaces any point selection; both would compete for the details panel
      clearPointSelections();
    }
    long draggedUs = Math.max(timeAt(x), 0);
    rangeFromUs = Math.min(rangeAnchorUs, draggedUs);
    rangeToUs = Math.max(rangeAnchorUs, draggedUs);
    repaint();
  }

  private void finishRangeDrag() {
    boolean started = rangeDragging;
    boolean completed = started && rangeToUs > rangeFromUs;
    rangeDragging = false;
    armedRangeBandKey = null;
    if (completed) {
      anchorBandKey = rangeBandKey;
      anchorUs = rangeAnchorUs;
      fireRangeSelected();
    }
    else if (started) {
      // a sub-microsecond drag at deep zoom rounds to nothing selectable
      rangeBandKey = null;
      repaint();
    }
  }

  private void fireRangeSelected() {
    Consumer<@Nullable RangeSelection> listener = rangeSelectionListener;
    if (listener == null || rangeBandKey == null) return;
    switch (bandByKey(rangeBandKey)) {
      case Band.CurveBand(CurveLane lane) -> listener.accept(new RangeSelection(lane, null, rangeFromUs, rangeToUs));
      case Band.MarkerBand(MarkerLane lane) -> listener.accept(new RangeSelection(null, lane, rangeFromUs, rangeToUs));
      case null, default -> {
      }
    }
  }

  private void clearRangeSelection() {
    if (rangeBandKey == null) return;
    rangeBandKey = null;
    Consumer<@Nullable RangeSelection> listener = rangeSelectionListener;
    if (listener != null) {
      listener.accept(null);
    }
    repaint();
  }

  private @Nullable Band bandByKey(String key) {
    for (Band band : bands) {
      if (band.key().equals(key)) return band;
    }
    return null;
  }

  private void installKeyboardNavigation() {
    bindKey("LEFT", () -> navigateSelection(-1, 0));
    bindKey("RIGHT", () -> navigateSelection(1, 0));
    bindKey("UP", () -> navigateSelection(0, -1));
    bindKey("DOWN", () -> navigateSelection(0, 1));
    // the zoom anchors on the selection (selectionAnchorUs), so ctrl+plus
    // dives onto the selected node; EQUALS is the main-row plus key,
    // ADD/SUBTRACT the numpad pair
    bindKey("control EQUALS", this::zoomIn);
    bindKey("control shift EQUALS", this::zoomIn);
    bindKey("control ADD", this::zoomIn);
    bindKey("control MINUS", this::zoomOut);
    bindKey("control SUBTRACT", this::zoomOut);
    bindKey("ESCAPE", this::escapePressed);
    bindKey("control F", () -> openSearch(""));
    bindKey("F3", () -> withSearchUi(SearchUi::nextMatch));
    bindKey("shift F3", () -> withSearchUi(SearchUi::previousMatch));
  }

  /** Escape closes an open search bar first; without one it clears the range selection. */
  private void escapePressed() {
    if (searchUi == null || !searchUi.close()) {
      clearRangeSelection();
    }
  }

  private void openSearch(String seedText) {
    if (searchUi != null) {
      searchUi.open(seedText);
    }
  }

  private void withSearchUi(Consumer<SearchUi> action) {
    if (searchUi != null) {
      action.accept(searchUi);
    }
  }

  private void bindKey(String stroke, Runnable action) {
    getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(stroke), stroke);
    getActionMap().put(stroke, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent event) {
        action.run();
      }
    });
  }

  /**
   * Arrow-key selection walking: a marker-lane span (frame, GC) steps
   * left/right through its lane; a call-chart run steps to its time
   * neighbors at the SAME depth (crossing into the next stack where the
   * flame splits), up to its caller and down into its first callee. The
   * chart's edges stop the walk.
   */
  private void navigateSelection(int dx, int dy) {
    if (selectedSpanLane != null && selectedSpan != null) {
      if (dx != 0) stepSpanSelection(dx);
    }
    else if (selected != null) {
      stepFlameSelection(dx, dy);
    }
  }

  private void stepSpanSelection(int direction) {
    List<UsSpan> spans = selectedSpanLane.spans();
    int index = spans.indexOf(selectedSpan) + direction;
    if (index < 0 || index >= spans.size()) return;
    selectedSpan = spans.get(index);
    spanSelectionListener.accept(selectedSpanLane, selectedSpan);
    revealTime(selectedSpan.startUs(), selectedSpan.endUs());
    repaint();
  }

  private void stepFlameSelection(int dx, int dy) {
    List<FlameNode> path = pathOfNode(selected);
    if (path.isEmpty()) return; // a live refresh replaced the tree under the selection
    FlameNode next;
    if (dy < 0) {
      next = path.size() >= 2 ? path.get(path.size() - 2) : null;
    }
    else if (dy > 0) {
      next = firstRunChild(selected);
    }
    else {
      next = dx > 0 ? nextAtDepth(root, 0, path.size(), selected.startUs())
                    : previousAtDepth(root, 0, path.size(), selected.startUs());
    }
    if (next == null) return;
    selected = next;
    selectionListener.accept(pathOfNode(next));
    revealTime(next.startUs(), next.endUs());
    repaint();
  }

  /** The root path to {@code target} (outermost first, the synthetic root excluded), empty when it left the tree. */
  private List<FlameNode> pathOfNode(FlameNode target) {
    List<FlameNode> path = new ArrayList<>();
    FlameNode node = root;
    while (node != target) {
      FlameNode next = null;
      for (FlameNode child : node.children()) {
        if (child.startUs() <= target.startUs() && target.startUs() < child.endUs()) {
          next = child;
          break;
        }
      }
      if (next == null) return List.of();
      path.add(next);
      node = next;
    }
    return path;
  }

  @Nullable
  private static FlameNode firstRunChild(FlameNode node) {
    for (FlameNode child : node.children()) {
      if (!child.idle()) return child;
    }
    return null;
  }

  /** The earliest run at {@code targetDepth} starting after {@code fromStartUs} — same-depth runs never overlap, so time order is walk order. */
  @Nullable
  private static FlameNode nextAtDepth(FlameNode node, int depth, int targetDepth, long fromStartUs) {
    if (depth == targetDepth) {
      return !node.idle() && node.startUs() > fromStartUs ? node : null;
    }
    for (FlameNode child : node.children()) {
      if (child.endUs() <= fromStartUs) continue; // its whole subtree starts earlier
      FlameNode found = nextAtDepth(child, depth + 1, targetDepth, fromStartUs);
      if (found != null) return found;
    }
    return null;
  }

  /** The latest run at {@code targetDepth} starting before {@code fromStartUs}. */
  @Nullable
  private static FlameNode previousAtDepth(FlameNode node, int depth, int targetDepth, long fromStartUs) {
    if (depth == targetDepth) {
      return !node.idle() && node.startUs() < fromStartUs ? node : null;
    }
    FlameNode best = null;
    for (FlameNode child : node.children()) {
      if (child.startUs() >= fromStartUs) break; // no descendant starts before its parent
      FlameNode found = previousAtDepth(child, depth + 1, targetDepth, fromStartUs);
      if (found != null) best = found; // later children hold closer predecessors
    }
    return best;
  }

  /** Scrolls just enough to bring a keyboard selection into view; scrolling away counts as leaving live-follow. */
  private void revealTime(long startUs, long endUs) {
    if (usPerPixel <= 0) return; // the whole session is on screen
    long visibleUs = zoomedVisibleUs();
    long marginUs = visibleUs / 10;
    long newStartUs;
    if (startUs < viewStartUs) {
      newStartUs = startUs - marginUs;
    }
    else if (endUs > viewStartUs + visibleUs) {
      // a span wider than the view aligns its start instead of its end
      newStartUs = endUs - startUs > visibleUs ? startUs - marginUs : endUs + marginUs - visibleUs;
    }
    else {
      return;
    }
    followLive = false;
    setViewStart(newStartUs);
    syncScrollBar();
    fireViewChanged();
  }

  /** The sample whose value holds at {@code instantUs}; null before the lane's first sample. */
  @Nullable
  private static CurvePoint lastChangeAt(CurveLane lane, long instantUs) {
    if (lane.points().isEmpty()) return null;
    CurvePoint floor = lane.points().get(lastIndexAtOrBefore(lane.points(), instantUs));
    return floor.timeUs() <= instantUs ? floor : null;
  }

  private List<FlameNode> pathAt(Point point) {
    int row = chartRowAt(point);
    if (row < 0) return List.of();
    List<FlameNode> path = ProfilerTimeline.pathTo(root, timeAt(point.x), row + 1);
    // a shorter path means the clicked row is idle or below the stack there
    return path.size() == row + 1 ? path : List.of();
  }

  @Nullable
  private FlameNode nodeAt(Point point) {
    int row = chartRowAt(point);
    if (row < 0) return null;
    return ProfilerTimeline.nodeAt(root, timeAt(point.x), row + 1);
  }

  /** The chart row under the point, -1 outside the run area (other lanes, past the tree, rows hidden). */
  private int chartRowAt(Point point) {
    int top = callsBodyTop();
    if (top < 0 || point.y < top) return -1;
    int row = (point.y - top) / rowHeight();
    return row < rowCount ? row : -1;
  }

  @Nullable
  private MarkerLane laneAt(Point point) {
    int index = bandBodyIndexAt(point.y);
    return index >= 0 && bands.get(index) instanceof Band.MarkerBand(MarkerLane lane) ? lane : null;
  }

  @Nullable
  private CurveLane curveLaneAt(Point point) {
    int index = bandBodyIndexAt(point.y);
    return index >= 0 && bands.get(index) instanceof Band.CurveBand(CurveLane lane) ? lane : null;
  }

  private boolean onEventsBand(int y) {
    int index = bandBodyIndexAt(y);
    return index >= 0 && bands.get(index) instanceof Band.EventsBand;
  }

  /** The band under {@code y} when the point is in its BODY; -1 on header strips (collapsed bands have no body). */
  private int bandBodyIndexAt(int y) {
    int index = bandIndexAt(y);
    if (index < 0) return -1;
    return y >= bandTop(index) + headerHeight() ? index : -1;
  }

  /** The band whose header strip is under the point, or -1; the whole strip toggles the collapse. */
  private int headerBandAt(Point point) {
    int index = bandIndexAt(point.y);
    if (index < 0) return -1;
    return point.y < bandTop(index) + headerHeight() ? index : -1;
  }

  private void toggleCollapsed(String key) {
    if (!collapsedBands.remove(key)) {
      collapsedBands.add(key);
    }
    fireViewPreferencesChanged();
    revalidate();
    repaint();
  }

  /** Folds every visible lane (the calls lane included) to its header. */
  void collapseAll() {
    for (Band band : bands) {
      collapsedBands.add(band.key());
    }
    fireViewPreferencesChanged();
    revalidate();
    repaint();
  }

  /** Expands every lane, remembered-but-hidden ones included. */
  void expandAll() {
    collapsedBands.clear();
    fireViewPreferencesChanged();
    revalidate();
    repaint();
  }

  /** Binary search over a lane's time-ordered spans; null between spans. */
  @Nullable
  private static UsSpan spanAt(MarkerLane lane, long timeUs) {
    List<UsSpan> spans = lane.spans();
    int low = 0;
    int high = spans.size() - 1;
    while (low <= high) {
      int middle = (low + high) >>> 1;
      UsSpan span = spans.get(middle);
      if (timeUs < span.startUs()) {
        high = middle - 1;
      }
      else if (timeUs >= span.endUs()) {
        low = middle + 1;
      }
      else {
        return span;
      }
    }
    return null;
  }

  /** Ctrl+wheel zooms around the pointer, shift+wheel pans; anything else scrolls the pane vertically as usual. */
  private void onWheel(MouseWheelEvent event) {
    if (event.isControlDown()) {
      event.consume();
      followLive = false;
      zoomTo(scale() * Math.pow(ZOOM_STEP, event.getPreciseWheelRotation()), timeAt(event.getX()), event.getX());
      return;
    }
    if (event.isShiftDown() && usPerPixel > 0) {
      event.consume();
      followLive = false;
      int deltaPx = (int)Math.round(event.getPreciseWheelRotation() * getWidth() * 0.1);
      setViewStart(viewStartUs + (long)(deltaPx * usPerPixel));
      syncScrollBar();
      repaint();
      fireViewChanged();
      return;
    }
    Container parent = getParent();
    if (parent != null) {
      parent.dispatchEvent(SwingUtilities.convertMouseEvent(this, event, parent));
    }
  }

  void zoomIn() {
    followLive = false;
    zoomAtCenter(1 / BUTTON_ZOOM_STEP);
  }

  void zoomOut() {
    followLive = false;
    zoomAtCenter(BUTTON_ZOOM_STEP);
  }

  /** Button zoom anchors on the selected run so it stays centered in frame; without a selection the view center holds. */
  private void zoomAtCenter(double factor) {
    Rectangle visible = getVisibleRect();
    int centerX = visible.x + visible.width / 2;
    zoomTo(scale() * factor, selectionAnchorUs(timeAt(centerX)), centerX);
  }

  /** The instant a selection-anchored zoom centers on: whatever is selected, else the view's own center. */
  private long selectionAnchorUs(long fallbackUs) {
    if (selected != null) return (selected.startUs() + selected.endUs()) / 2;
    if (selectedSpan != null) return (selectedSpan.startUs() + selectedSpan.endUs()) / 2;
    if (selectedEvent != null) return selectedEvent.timeUs();
    if (selectedCurve != null) return selectedCurve.instantUs();
    return fallbackUs;
  }

  /** Applies the target scale keeping {@code anchorUs} under pixel {@code mouseX}; zooming past the session falls back to fit. */
  private void zoomTo(double targetUsPerPixel, long anchorUs, int mouseX) {
    if (targetUsPerPixel >= fitScale(getWidth())) {
      usPerPixel = 0;
      viewStartUs = root.startUs();
    }
    else {
      usPerPixel = Math.max(targetUsPerPixel, MIN_US_PER_PIXEL);
      setViewStart(anchorUs - (long)(mouseX * usPerPixel));
    }
    syncScrollBar();
    repaint();
    fireViewChanged();
  }

  /** The time the zoomed view spans across the panel's width. */
  private long zoomedVisibleUs() {
    return (long)Math.ceil(Math.max(1, getWidth()) * usPerPixel);
  }

  /** Moves the zoomed view's left edge, clamped so the view never leaves the session. */
  private void setViewStart(long startUs) {
    long visibleUs = zoomedVisibleUs();
    long maxStartUs = root.startUs() + Math.max(0, root.durationUs() - visibleUs);
    viewStartUs = Math.max(root.startUs(), Math.min(startUs, maxStartUs));
  }

  /** Mirrors the view into the scrollbar: proportional thumb, disabled in fit mode. */
  private void syncScrollBar() {
    JScrollBar scrollBar = horizontalScrollBar;
    if (scrollBar == null) return;
    syncingScrollBar = true;
    try {
      long durationUs = root.durationUs();
      if (usPerPixel <= 0 || durationUs <= 0) {
        scrollBar.setEnabled(false);
        scrollBar.setValues(0, SCROLL_RESOLUTION, 0, SCROLL_RESOLUTION);
        return;
      }
      long visibleUs = zoomedVisibleUs();
      int extent = (int)Math.min(SCROLL_RESOLUTION, Math.max(1, (long)(SCROLL_RESOLUTION * (double)visibleUs / durationUs)));
      int value = (int)Math.round((viewStartUs - root.startUs()) / (double)durationUs * SCROLL_RESOLUTION);
      scrollBar.setEnabled(extent < SCROLL_RESOLUTION);
      scrollBar.setValues(Math.min(value, SCROLL_RESOLUTION - extent), extent, 0, SCROLL_RESOLUTION);
      scrollBar.setUnitIncrement(Math.max(1, extent / 20));
      scrollBar.setBlockIncrement(Math.max(1, extent * 9 / 10));
    }
    finally {
      syncingScrollBar = false;
    }
  }

  private void applyScrollValue(int value) {
    if (usPerPixel <= 0) return;
    // dragging the thumb disarms live-follow; parking it flush right re-arms
    JScrollBar scrollBar = horizontalScrollBar;
    followLive = scrollBar != null && value + scrollBar.getVisibleAmount() >= SCROLL_RESOLUTION;
    long offsetUs = (long)((double)value / SCROLL_RESOLUTION * root.durationUs());
    setViewStart(root.startUs() + offsetUs);
    repaint();
    fireViewChanged();
  }

  private int rowHeight() {
    return JBUI.scale(17);
  }

  private int rulerHeight() {
    return JBUI.scale(20);
  }

  /** A band's total height: its header strip plus, unless collapsed, its body. */
  private int bandHeight(String key) {
    return headerHeight() + (collapsedBands.contains(key) ? 0 : bodyHeight(key));
  }

  /** A band body's current height: the calls lane follows its stack depth; otherwise the user's dragged value or the kind's default. */
  private int bodyHeight(String key) {
    if (CALLS_BAND_KEY.equals(key)) return Math.max(rowCount, 1) * rowHeight();
    return bandHeights.getOrDefault(key, key.startsWith("curve:") ? JBUI.scale(40) : rowHeight());
  }

  private int headerHeight() {
    return JBUI.scale(16);
  }

  /** The y where the band at {@code index} starts; {@code bands.size()} gives the stack's bottom. */
  private int bandTop(int index) {
    int top = rulerHeight();
    for (int i = 0; i < index; i++) {
      top += bandHeight(bands.get(i).key());
    }
    return top;
  }

  /** The band under {@code y}, or -1 outside the band stack. */
  private int bandIndexAt(int y) {
    if (y < rulerHeight()) return -1;
    int top = rulerHeight();
    for (int i = 0; i < bands.size(); i++) {
      top += bandHeight(bands.get(i).key());
      if (y < top) return i;
    }
    return -1;
  }

  /** The key of the band whose bottom divider is under {@code y}; null when none is. Collapsed bands and the depth-sized calls lane are not resizable. */
  @Nullable
  private String dividerKeyAt(int y) {
    int grip = JBUI.scale(3);
    for (int index = 0; index < bands.size(); index++) {
      String key = bands.get(index).key();
      if (collapsedBands.contains(key) || CALLS_BAND_KEY.equals(key)) continue;
      if (Math.abs(y - bandTop(index + 1)) <= grip) return key;
    }
    return null;
  }

  private void resizeBand(String key, int height) {
    int clamped = Math.max(JBUI.scale(12), Math.min(JBUI.scale(200), height));
    bandHeights.put(key, clamped);
    revalidate();
    repaint();
  }

  private static int gripWidth() {
    return JBUI.scale(10);
  }

  /** The insertion gap for a drop at {@code y}: below every band whose middle lies above it. */
  private int dropGapAt(int y) {
    int gap = 0;
    for (int i = 0; i < bands.size(); i++) {
      int middle = bandTop(i) + bandHeight(bands.get(i).key()) / 2;
      if (y > middle) gap = i + 1;
    }
    return gap;
  }

  /** The chart's own menu: view configuration and layout housekeeping (the gutter and tool window have their own). */
  private void showContextMenu(MouseEvent event) {
    DefaultActionGroup group = new DefaultActionGroup();
    Runnable opener = configureViewOpener;
    if (opener != null) {
      group.add(DumbAwareAction.create(HaxeProfilerBundle.message("haxe.profiler.callchart.configure.view"),
                                       action -> opener.run()));
    }
    group.add(DumbAwareAction.create(HaxeProfilerBundle.message("haxe.profiler.callchart.reset.layout"),
                                     action -> resetBandLayout()));
    ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.POPUP, group);
    menu.getComponent().show(this, event.getX(), event.getY());
  }

  /** Back to the default arrangement, heights and expansion, persisted immediately. */
  private void resetBandLayout() {
    bandHeights.clear();
    bandOrder = new ArrayList<>();
    collapsedBands.clear();
    fireViewPreferencesChanged();
    rebuildBands();
  }

  /**
   * Lands the dragged band at the gap under {@code y} and persists the
   * arrangement: the visible keys in their new order, with previously
   * remembered but currently hidden keys kept after them.
   */
  private void dropDraggedBand(int y) {
    int from = draggingBand;
    draggingBand = -1;
    int gap = dropGapAt(y);
    int to = gap > from ? gap - 1 : gap;
    if (from < 0 || from >= bands.size() || to == from) {
      repaint();
      return;
    }
    List<Band> arranged = new ArrayList<>(bands);
    arranged.add(to, arranged.remove(from));
    bands = arranged;

    List<String> order = new ArrayList<>(arranged.stream().map(Band::key).toList());
    for (String known : bandOrder) {
      if (!order.contains(known)) order.add(known);
    }
    bandOrder = order;
    fireViewPreferencesChanged();
    revalidate();
    repaint();
  }

  private double scale() {
    return usPerPixel > 0 ? usPerPixel : fitScale(getWidth());
  }

  private double fitScale(int width) {
    return Math.max(1, root.durationUs()) / (double)Math.max(1, width);
  }

  /** The time at the panel's left edge — the view start while zoomed, the tree start in fit mode. */
  private long viewLeftUs() {
    return usPerPixel > 0 ? viewStartUs : root.startUs();
  }

  /**
   * Clamped far outside any viewport: at deep zoom a session-distant time
   * maps to billions of pixels, and a raw int cast would wrap negative and
   * break the painters' culling. The clamp keeps ordering, so culling and
   * fill widths stay correct.
   */
  private int xOf(long timeUs) {
    double x = (timeUs - viewLeftUs()) / scale();
    return (int)Math.round(Math.max(-10_000_000, Math.min(10_000_000, x)));
  }

  private long timeAt(double x) {
    return viewLeftUs() + (long)(x * scale());
  }

  /** The smallest 1/2/5 x 10^k microsecond step at least minUs wide. */
  private static long niceStepUs(double minUs) {
    for (long magnitude = 1; ; magnitude *= 10) {
      for (long factor : STEP_FACTORS) {
        long step = factor * magnitude;
        if (step >= minUs) return step;
      }
    }
  }

  /** Tick label with the precision the STEP demands — at 5 ms steps every tick inside one second must still differ. */
  private static String formatTick(long us, long stepUs) {
    if (stepUs >= 1_000_000) return us / 1_000_000 + " s";
    if (stepUs >= 1000) return us / 1000 + " ms";
    int decimals = 4 - Long.toString(stepUs).length();
    return String.format(Locale.ROOT, "%." + decimals + "f ms", us / 1000.0);
  }

  @Override
  public Dimension getPreferredSize() {
    // width always tracks the viewport (the horizontal axis is virtual); only the height is real
    return new Dimension(0, bandTop(bands.size()));
  }

  @Override
  public Dimension getPreferredScrollableViewportSize() {
    return getPreferredSize();
  }

  @Override
  public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
    return rowHeight();
  }

  @Override
  public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
    return orientation == SwingConstants.HORIZONTAL ? visible.width : visible.height;
  }

  @Override
  public boolean getScrollableTracksViewportWidth() {
    return true;
  }

  @Override
  public boolean getScrollableTracksViewportHeight() {
    return getParent() instanceof JViewport viewport && viewport.getHeight() > getPreferredSize().height;
  }
}
