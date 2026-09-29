package com.intellij.plugins.haxe.profiler.bridge.chart;

import com.intellij.plugins.haxe.profiler.timeline.ProfilerTimeline.UsSpan;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * The whole session in one strip above the Call Chart: frame-duration bars
 * (a taller bar is a slower frame, so hitches stand out), or a coarse
 * activity silhouette when the capture has no frames. The chart's current
 * view shows as a window: drag inside it to pan, drag an edge to zoom,
 * click outside it to jump. The chart reports every view change back
 * through {@link #showWindow}.
 */
final class HaxeChartMinimapPanel extends JComponent {

  private static final Color FRAME_BAR = new JBColor(0x9CB8D6, 0x53687D);
  private static final Color ACTIVITY = new JBColor(0xB8CCB8, 0x4E5E4E);
  private static final Color WINDOW_FILL = new JBColor(new Color(0x33, 0x74, 0xF0, 40), new Color(0x66, 0xA3, 0xE0, 50));
  private static final Color WINDOW_BORDER = new JBColor(0x3574F0, 0x66A3E0);
  private static final Color STRIP_BORDER = new JBColor(0xC8C8C8, 0x515151);

  /** Told the window the user dragged out, as [startUs, endUs]. */
  private final BiConsumer<Long, Long> windowConsumer;
  private List<UsSpan> frameSpans = List.of();
  private List<UsSpan> activitySpans = List.of();
  private long durationUs = 1;
  private long windowStartUs;
  private long windowEndUs = 1;
  private boolean wholeSession = true;

  private Drag drag = Drag.NONE;
  private long dragGrabOffsetUs;

  private enum Drag {
    NONE,
    PAN,
    LEFT_EDGE,
    RIGHT_EDGE
  }

  HaxeChartMinimapPanel(@NotNull BiConsumer<Long, Long> windowConsumer) {
    this.windowConsumer = windowConsumer;
    setOpaque(false);
    MouseAdapter mouse = new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent event) {
        beginDrag(event.getX());
      }

      @Override
      public void mouseDragged(MouseEvent event) {
        dragTo(event.getX());
      }

      @Override
      public void mouseReleased(MouseEvent event) {
        drag = Drag.NONE;
      }

      @Override
      public void mouseMoved(MouseEvent event) {
        setCursor(cursorFor(event.getX()));
      }
    };
    addMouseListener(mouse);
    addMouseMotionListener(mouse);
  }

  /** The strip's session-wide content; frame bars win when the capture has frames. */
  void setContent(long durationUs, @NotNull List<UsSpan> frameSpans, @NotNull List<UsSpan> activitySpans) {
    this.durationUs = Math.max(durationUs, 1);
    this.frameSpans = frameSpans;
    this.activitySpans = activitySpans;
    repaint();
  }

  /** The chart's current view, pushed from its view listener. */
  void showWindow(long startUs, long visibleUs, boolean wholeSession) {
    windowStartUs = startUs;
    windowEndUs = startUs + Math.max(visibleUs, 1);
    this.wholeSession = wholeSession;
    repaint();
  }

  @Override
  protected void paintComponent(Graphics graphics) {
    Graphics2D g = (Graphics2D)graphics.create();
    try {
      int height = getHeight();
      paintBars(g, height);
      g.setColor(STRIP_BORDER);
      g.drawLine(0, height - 1, getWidth(), height - 1);
      if (!wholeSession) {
        int x0 = xOf(windowStartUs);
        int x1 = Math.max(xOf(windowEndUs), x0 + JBUI.scale(4));
        g.setColor(WINDOW_FILL);
        g.fillRect(x0, 0, x1 - x0, height - 1);
        g.setColor(WINDOW_BORDER);
        g.drawRect(x0, 0, x1 - x0 - 1, height - 2);
      }
    }
    finally {
      g.dispose();
    }
  }

  private void paintBars(Graphics2D g, int height) {
    if (!frameSpans.isEmpty()) {
      long maxDurationUs = 1;
      for (UsSpan span : frameSpans) {
        maxDurationUs = Math.max(maxDurationUs, span.durationUs());
      }
      g.setColor(FRAME_BAR);
      for (UsSpan span : frameSpans) {
        int x0 = xOf(span.startUs());
        int x1 = Math.max(xOf(span.endUs()), x0 + 1);
        int barHeight = Math.max(1, (int)(span.durationUs() * (height - JBUI.scale(4)) / maxDurationUs));
        g.fillRect(x0, height - 1 - barHeight, x1 - x0, barHeight);
      }
      return;
    }
    g.setColor(ACTIVITY);
    int silhouetteHeight = height - JBUI.scale(8);
    for (UsSpan span : activitySpans) {
      int x0 = xOf(span.startUs());
      int x1 = Math.max(xOf(span.endUs()), x0 + 1);
      g.fillRect(x0, height - 1 - silhouetteHeight, x1 - x0, silhouetteHeight);
    }
  }

  private void beginDrag(int x) {
    if (wholeSession) {
      // no window yet - dragging carves one out around the press point
      drag = Drag.RIGHT_EDGE;
      windowStartUs = timeAt(x);
      windowEndUs = windowStartUs + 1;
      return;
    }
    int x0 = xOf(windowStartUs);
    int x1 = xOf(windowEndUs);
    int grip = JBUI.scale(4);
    if (Math.abs(x - x0) <= grip) {
      drag = Drag.LEFT_EDGE;
    }
    else if (Math.abs(x - x1) <= grip) {
      drag = Drag.RIGHT_EDGE;
    }
    else if (x > x0 && x < x1) {
      drag = Drag.PAN;
      dragGrabOffsetUs = timeAt(x) - windowStartUs;
    }
    else {
      // jump: center the window on the click, then keep panning with it
      drag = Drag.PAN;
      long spanUs = windowEndUs - windowStartUs;
      dragGrabOffsetUs = spanUs / 2;
      publish(timeAt(x) - spanUs / 2, timeAt(x) + spanUs - spanUs / 2);
    }
  }

  private void dragTo(int x) {
    long spanUs = windowEndUs - windowStartUs;
    switch (drag) {
      case PAN -> publish(timeAt(x) - dragGrabOffsetUs, timeAt(x) - dragGrabOffsetUs + spanUs);
      case LEFT_EDGE -> publish(Math.min(timeAt(x), windowEndUs - 1), windowEndUs);
      case RIGHT_EDGE -> publish(windowStartUs, Math.max(timeAt(x), windowStartUs + 1));
      case NONE -> {
      }
    }
  }

  /** Clamps into the session and hands the window to the chart; the chart's echo repaints the rect. */
  private void publish(long startUs, long endUs) {
    long spanUs = Math.max(endUs - startUs, 1);
    long clampedStart = Math.max(0, Math.min(startUs, durationUs - spanUs));
    windowConsumer.accept(clampedStart, Math.min(clampedStart + spanUs, durationUs));
  }

  private Cursor cursorFor(int x) {
    if (!wholeSession) {
      int grip = JBUI.scale(4);
      if (Math.abs(x - xOf(windowStartUs)) <= grip || Math.abs(x - xOf(windowEndUs)) <= grip) {
        return Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
      }
      if (x > xOf(windowStartUs) && x < xOf(windowEndUs)) {
        return Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
      }
    }
    return Cursor.getDefaultCursor();
  }

  private int xOf(long timeUs) {
    return (int)(timeUs * (long)Math.max(1, getWidth()) / durationUs);
  }

  private long timeAt(int x) {
    return Math.max(0, Math.min((long)x * durationUs / Math.max(1, getWidth()), durationUs));
  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(0, JBUI.scale(44));
  }
}
