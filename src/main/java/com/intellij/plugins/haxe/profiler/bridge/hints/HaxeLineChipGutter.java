package com.intellij.plugins.haxe.profiler.bridge.hints;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.TextAnnotationGutterProvider;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.editor.colors.ColorKey;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.EditorGutterComponentEx;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.plugins.haxe.HaxeProfilerBundle;
import com.intellij.plugins.haxe.profiler.bridge.HaxeProfilerFormats;
import com.intellij.ui.JBColor;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.xml.util.XmlStringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * The rounded per-line time chips in the editor gutter, styled after the
 * stock In-Editor Performance Hints (same theme keys, same two-tier
 * red/grey design). The platform's annotation column cannot round its
 * corners, so an invisible {@link TextAnnotationGutterProvider} only
 * RESERVES the column's width and a lightweight overlay child of the
 * gutter paints the chips over it — rounded rect, left-aligned text, an
 * HTML tooltip on hover.
 */
final class HaxeLineChipGutter {

  private static final Color HOT_BACKGROUND =
    JBColor.namedColor("LineProfiler.HotLine.labelBackground", new JBColor(0xFFE0E0, 0x593D41));
  private static final Color HOT_HOVER_BACKGROUND =
    JBColor.namedColor("LineProfiler.HotLine.hoverBackground", new JBColor(0xFFCCCC, 0x704745));
  private static final Color LINE_BACKGROUND =
    JBColor.namedColor("LineProfiler.Line.labelBackground", new JBColor(0xDFDFDF, 0x43474A));
  private static final Color LINE_HOVER_BACKGROUND =
    JBColor.namedColor("LineProfiler.Line.hoverBackground", new JBColor(0xD1D1D1, 0x4A4E52));
  private static final Color HOT_FOREGROUND =
    JBColor.namedColor("LineProfiler.HotLine.foreground", new JBColor(0xC7222D, 0xFF5261));
  private static final Color LINE_FOREGROUND =
    JBColor.namedColor("LineProfiler.Line.foreground", new JBColor(0x616769, 0x787878));
  /** A line at or above this share of the session renders hot. */
  private static final int HOT_PERCENT = 5;

  private HaxeLineChipGutter() {
  }

  /** One editor's installed chips; uninstall removes both the overlay and the reserved column. */
  record Installed(@NotNull WidthHolder widthHolder,
                   @NotNull JComponent overlay,
                   @NotNull EditorGutterComponentEx gutter,
                   @NotNull ComponentListener relayout) {

    void uninstall(@NotNull Editor editor) {
      gutter.removeComponentListener(relayout);
      gutter.remove(overlay);
      widthHolder.closingInternally = true;
      editor.getGutter().closeTextAnnotations(List.of(widthHolder));
      gutter.repaint();
    }
  }

  /**
   * Installs the chip column on the editor, or null when the editor exposes
   * no extended gutter. {@code closedExternally} fires when the gutter's own
   * "Close Annotations" removed the column (never on {@link Installed#uninstall}).
   */
  @Nullable
  static Installed install(@NotNull Editor editor, @NotNull Map<Integer, HaxeLineTimes.LineTime> lines,
                           long sessionUs, @NotNull Runnable closedExternally) {
    if (!(editor instanceof EditorEx editorEx)) return null;
    EditorGutterComponentEx gutter = editorEx.getGutterComponentEx();

    ChipOverlay overlay = new ChipOverlay(editorEx, lines, sessionUs);
    WidthHolder widthHolder = new WidthHolder(overlay.blankOfColumnWidth(), closedExternally);
    editor.getGutter().registerTextAnnotation(widthHolder);

    ComponentListener relayout = new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent event) {
        fitToAnnotationsArea(overlay, gutter);
      }
    };
    gutter.add(overlay);
    gutter.addComponentListener(relayout);
    fitToAnnotationsArea(overlay, gutter);
    gutter.repaint();
    return new Installed(widthHolder, overlay, gutter, relayout);
  }

  private static void fitToAnnotationsArea(JComponent overlay, EditorGutterComponentEx gutter) {
    overlay.setBounds(gutter.getAnnotationsAreaOffset(), 0, gutter.getAnnotationsAreaWidth(), gutter.getHeight());
  }

  /**
   * Reserves the annotation column at the chips' width and paints nothing:
   * one all-spaces line is enough — the column is as wide as its widest
   * line, and spaces leave the chips' canvas clean.
   */
  static final class WidthHolder implements TextAnnotationGutterProvider {
    private final String blank;
    private final Runnable closedExternally;
    boolean closingInternally;

    WidthHolder(String blank, Runnable closedExternally) {
      this.blank = blank;
      this.closedExternally = closedExternally;
    }

    @Override
    public @Nullable String getLineText(int line, Editor editor) {
      return line == 0 ? blank : null;
    }

    @Override
    public @Nullable String getToolTip(int line, Editor editor) {
      return null;
    }

    @Override
    public EditorFontType getStyle(int line, Editor editor) {
      return EditorFontType.PLAIN;
    }

    @Override
    public @Nullable ColorKey getColor(int line, Editor editor) {
      return null;
    }

    @Override
    public @Nullable Color getBgColor(int line, Editor editor) {
      return null;
    }

    /**
     * Fallback for popup paths that bypass the overlay (mouse right-clicks
     * open the standard menu from the overlay itself): the annotations area
     * builds its own popup from the providers' actions plus the platform's
     * Close Annotations — handing the standard group over keeps even that
     * popup close to the rest of the gutter.
     */
    @Override
    public List<AnAction> getPopupActions(int line, Editor editor) {
      AnAction gutterMenu = ActionManager.getInstance().getAction(IdeActions.GROUP_EDITOR_GUTTER);
      return gutterMenu == null ? List.of() : List.of(gutterMenu);
    }

    @Override
    public void gutterClosed() {
      if (closingInternally) return;
      // the platform's Close Annotations removed the column; the notification
      // is deferred because this fires mid-iteration over the gutter's
      // provider list, and the handler closes annotations itself
      ApplicationManager.getApplication().invokeLater(closedExternally);
    }
  }

  /** Paints the visible lines' chips, hover-highlights them and shows their instant tooltip. */
  private static final class ChipOverlay extends JComponent {
    private final EditorEx editor;
    private final Map<Integer, HaxeLineTimes.LineTime> lines;
    private final long sessionUs;
    private int hoveredLine = -1;
    private @Nullable JBPopup tooltip;

    ChipOverlay(EditorEx editor, Map<Integer, HaxeLineTimes.LineTime> lines, long sessionUs) {
      this.editor = editor;
      this.lines = lines;
      this.sessionUs = sessionUs;
      setOpaque(false);
      // this overlay sits ON the gutter and owns hover + tooltip; the popup
      // trigger opens the STANDARD gutter menu itself (the gutter's own
      // annotation-area popup would append Close Annotations), and every
      // other press/release/click is forwarded so the gutter underneath
      // keeps its interactions. Mouse MOVES are not forwarded: the gutter's
      // hover handling would cancel the tooltip.
      MouseAdapter mouseHandler = new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent event) {
          handleOrForward(event);
        }

        @Override
        public void mouseReleased(MouseEvent event) {
          handleOrForward(event);
        }

        @Override
        public void mouseClicked(MouseEvent event) {
          forwardToGutter(event);
        }

        @Override
        public void mouseMoved(MouseEvent event) {
          updateHover(event);
        }

        @Override
        public void mouseExited(MouseEvent event) {
          setHoveredLine(-1, null, null);
        }
      };
      addMouseListener(mouseHandler);
      addMouseMotionListener(mouseHandler);
    }

    private void handleOrForward(MouseEvent event) {
      if (event.isPopupTrigger()) {
        setHoveredLine(-1, null, null);
        showGutterMenu(event);
        return;
      }
      forwardToGutter(event);
    }

    private void forwardToGutter(MouseEvent event) {
      Container gutter = getParent();
      if (gutter != null) {
        gutter.dispatchEvent(SwingUtilities.convertMouseEvent(this, event, gutter));
      }
    }

    /** The standard gutter menu, with the gutter as context — matching the line-number area exactly. */
    private void showGutterMenu(MouseEvent event) {
      if (!(ActionManager.getInstance().getAction(IdeActions.GROUP_EDITOR_GUTTER) instanceof ActionGroup group)) return;
      ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.EDITOR_GUTTER_POPUP, group);
      menu.setTargetComponent(editor.getGutterComponentEx());
      menu.getComponent().show(this, event.getX(), event.getY());
    }

    private void updateHover(MouseEvent event) {
      int line = lineAt(event.getY());
      HaxeLineTimes.LineTime time = lines.get(line);
      boolean overChip = time != null && event.getX() <= chipWidth(time);
      setHoveredLine(overChip ? line : -1, overChip ? time : null, event);
    }

    /** Repaints the highlight and swaps the tooltip when the hovered chip changes; -1 clears both. */
    private void setHoveredLine(int line, @Nullable HaxeLineTimes.LineTime time, @Nullable MouseEvent event) {
      if (line == hoveredLine) return;
      hoveredLine = line;
      repaint();
      if (tooltip != null) {
        tooltip.cancel();
        tooltip = null;
      }
      if (time != null && event != null) {
        tooltip = showTooltip(time, event);
      }
    }

    /**
     * The tooltip is an instant lightweight popup (the stock hints do the
     * same) — the shared ToolTipManager both delays it and loses it to the
     * gutter's own tooltip handling.
     */
    private JBPopup showTooltip(HaxeLineTimes.LineTime time, MouseEvent event) {
      JBLabel content = new JBLabel(tooltipText(time));
      content.setBorder(JBUI.Borders.empty(6, 10));
      JBPopup popup = JBPopupFactory.getInstance()
        .createComponentPopupBuilder(content, null)
        .setRequestFocus(false)
        .setFocusable(false)
        .createPopup();
      int belowRow = editor.visualLineToY(editor.yToVisualLine(event.getY())) + editor.getLineHeight() + JBUI.scale(2);
      popup.show(new RelativePoint(this, new Point(event.getX() + JBUI.scale(8), belowRow)));
      return popup;
    }

    /** The 1-based document line under an overlay y, matching the chip table's keys. */
    private int lineAt(int y) {
      return editor.visualToLogicalPosition(new VisualPosition(editor.yToVisualLine(y), 0)).line + 1;
    }

    private int chipWidth(HaxeLineTimes.LineTime time) {
      String text = HaxeProfilerFormats.formatUs(time.totalUs());
      return getFontMetrics(chipFont()).stringWidth(text) + 2 * textPadding();
    }

    @Override
    public void removeNotify() {
      setHoveredLine(-1, null, null);
      super.removeNotify();
    }

    private Font chipFont() {
      return editor.getColorsScheme().getFont(EditorFontType.PLAIN);
    }

    /** Spaces wide enough for the widest chip, measured in the same font the chips use. */
    String blankOfColumnWidth() {
      FontMetrics metrics = getFontMetrics(chipFont());
      int widest = 0;
      for (HaxeLineTimes.LineTime time : lines.values()) {
        widest = Math.max(widest, metrics.stringWidth(HaxeProfilerFormats.formatUs(time.totalUs())));
      }
      int columnWidth = widest + 2 * textPadding() + JBUI.scale(4);
      int spaces = Math.max(1, (int)Math.ceil(columnWidth / (double)Math.max(metrics.charWidth(' '), 1)));
      return " ".repeat(spaces);
    }

    private static int textPadding() {
      return JBUI.scale(5);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
      Graphics2D g = (Graphics2D)graphics.create();
      try {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(chipFont());
        Rectangle clip = g.getClipBounds();
        if (clip == null) clip = new Rectangle(0, 0, getWidth(), getHeight());

        int lineCount = editor.getDocument().getLineCount();
        int visual = editor.yToVisualLine(Math.max(clip.y, 0));
        int bottom = clip.y + clip.height;
        while (true) {
          int y = editor.visualLineToY(visual);
          if (y > bottom) break;
          int logical = editor.visualToLogicalPosition(new VisualPosition(visual, 0)).line;
          if (logical >= lineCount) break;
          HaxeLineTimes.LineTime time = lines.get(logical + 1);
          if (time != null) {
            paintChip(g, y, time, logical + 1 == hoveredLine);
          }
          visual++;
        }
      }
      finally {
        g.dispose();
      }
    }

    private void paintChip(Graphics2D g, int y, HaxeLineTimes.LineTime time, boolean hovered) {
      boolean hot = time.totalUs() * 100 / sessionUs >= HOT_PERCENT;
      String text = HaxeProfilerFormats.formatUs(time.totalUs());
      FontMetrics metrics = g.getFontMetrics();
      int height = editor.getLineHeight() - JBUI.scale(2);
      int width = metrics.stringWidth(text) + 2 * textPadding();
      int arc = JBUI.scale(6);
      g.setColor(chipBackground(hot, hovered));
      g.fillRoundRect(0, y + JBUI.scale(1), width, height, arc, arc);
      g.setColor(hot ? HOT_FOREGROUND : LINE_FOREGROUND);
      int baseline = y + JBUI.scale(1) + (height + metrics.getAscent() - metrics.getDescent()) / 2;
      g.drawString(text, textPadding(), baseline);
    }

    private static Color chipBackground(boolean hot, boolean hovered) {
      if (hot) return hovered ? HOT_HOVER_BACKGROUND : HOT_BACKGROUND;
      return hovered ? LINE_HOVER_BACKGROUND : LINE_BACKGROUND;
    }

    /**
     * Like the Java hints: "X% of all — Y% of <method>" when the line knows
     * its enclosing method (sampled captures); tracy's per-function lines
     * show the figures instead.
     */
    private String tooltipText(HaxeLineTimes.LineTime time) {
      String title = HaxeProfilerBundle.message("haxe.profiler.hints.tooltip.title");
      String detail;
      if (time.enclosing() != null && time.enclosingTotalUs() > 0) {
        detail = HaxeProfilerBundle.message("haxe.profiler.hints.tooltip.of.method",
                                            percentOfSession(time),
                                            formatPercent(time.totalUs(), time.enclosingTotalUs()),
                                            time.enclosing());
      }
      else {
        detail = HaxeProfilerBundle.message("haxe.profiler.hints.tooltip.detail",
                                            percentOfSession(time),
                                            HaxeProfilerFormats.formatUs(time.totalUs()),
                                            HaxeProfilerFormats.formatUs(time.selfUs()));
      }
      return XmlStringUtil.wrapInHtml(title + "<br/>" + detail);
    }

    private String percentOfSession(HaxeLineTimes.LineTime time) {
      return formatPercent(time.totalUs(), sessionUs);
    }

    /** Whole percent as text; a nonzero share that floors to 0 reads "&lt;1", never a flat "0". */
    private static String formatPercent(long partUs, long wholeUs) {
      long percent = partUs * 100 / Math.max(wholeUs, 1);
      return percent == 0 && partUs > 0 ? "<1" : String.valueOf(percent);
    }
  }
}
