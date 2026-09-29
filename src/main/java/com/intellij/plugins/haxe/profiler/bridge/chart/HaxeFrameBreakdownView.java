package com.intellij.plugins.haxe.profiler.bridge.chart;

import com.intellij.plugins.haxe.profiler.bridge.HaxeProfilerFormats;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import javax.swing.JComponent;

/**
 * A frame's time shares as bar rows: the category summary above the biggest
 * individual parts. Each row shows a color chip, the name, the duration and
 * a share bar with its percent of the frame.
 */
final class HaxeFrameBreakdownView extends JComponent {

  /** One share of the frame; {@code emphasized} marks the summary group's rows. */
  record Row(@NotNull String name, long totalUs, long frameUs, @NotNull Color color, boolean emphasized) {
    int percent() {
      return frameUs > 0 ? (int)(totalUs * 100 / frameUs) : 0;
    }
  }

  /** The summary categories above the individual parts; either list may be empty. */
  record Rows(@NotNull List<Row> summary, @NotNull List<Row> parts) {
    boolean isEmpty() {
      return summary.isEmpty() && parts.isEmpty();
    }
  }

  private static final int ROW_HEIGHT = 20;
  private static final int GROUP_GAP = 8;
  private static final int BAR_WIDTH = 96;
  private static final int PERCENT_WIDTH = 34;
  private static final int TIME_WIDTH = 64;
  private static final int CHIP_SIZE = 9;

  private Rows rows = new Rows(List.of(), List.of());

  void show(@NotNull Rows content) {
    rows = content;
    revalidate();
    repaint();
  }

  @Override
  public Dimension getPreferredSize() {
    int count = rows.summary().size() + rows.parts().size();
    if (count == 0) return new Dimension(0, 0);
    int gap = !rows.summary().isEmpty() && !rows.parts().isEmpty() ? GROUP_GAP : 0;
    return new Dimension(JBUI.scale(280), JBUI.scale(count * ROW_HEIGHT + gap + 4));
  }

  @Override
  protected void paintComponent(Graphics graphics) {
    Graphics2D g = (Graphics2D)graphics.create();
    try {
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      int y = 0;
      for (Row row : rows.summary()) {
        paintRow(g, row, y);
        y += JBUI.scale(ROW_HEIGHT);
      }
      if (!rows.summary().isEmpty() && !rows.parts().isEmpty()) {
        g.setColor(JBColor.border());
        int lineY = y + JBUI.scale(GROUP_GAP) / 2;
        g.drawLine(JBUI.scale(6), lineY, getWidth() - JBUI.scale(6), lineY);
        y += JBUI.scale(GROUP_GAP);
      }
      for (Row row : rows.parts()) {
        paintRow(g, row, y);
        y += JBUI.scale(ROW_HEIGHT);
      }
    }
    finally {
      g.dispose();
    }
  }

  private void paintRow(Graphics2D g, Row row, int top) {
    int rowHeight = JBUI.scale(ROW_HEIGHT);
    int middle = top + rowHeight / 2;
    Font font = row.emphasized() ? JBFont.label().asBold() : JBFont.label();
    g.setFont(font);
    FontMetrics metrics = g.getFontMetrics();
    int textBaseline = middle + (metrics.getAscent() - metrics.getDescent()) / 2;

    int chip = JBUI.scale(CHIP_SIZE);
    g.setColor(row.color());
    g.fillRoundRect(JBUI.scale(6), middle - chip / 2, chip, chip, 3, 3);

    int percentRight = getWidth() - JBUI.scale(6);
    int barRight = percentRight - JBUI.scale(PERCENT_WIDTH);
    int barLeft = barRight - JBUI.scale(BAR_WIDTH);
    int timeRight = barLeft - JBUI.scale(10);
    int nameLeft = JBUI.scale(6) + chip + JBUI.scale(7);

    g.setColor(UIUtil.getLabelForeground());
    String name = truncate(row.name(), metrics, timeRight - JBUI.scale(TIME_WIDTH) - nameLeft - JBUI.scale(8));
    g.drawString(name, nameLeft, textBaseline);

    String time = HaxeProfilerFormats.formatUs(row.totalUs());
    g.drawString(time, timeRight - metrics.stringWidth(time), textBaseline);

    int barHeight = JBUI.scale(7);
    int barTop = middle - barHeight / 2;
    g.setColor(ColorUtil.withAlpha(row.color(), 0.25));
    g.fillRoundRect(barLeft, barTop, JBUI.scale(BAR_WIDTH), barHeight, barHeight, barHeight);
    int fill = (int)Math.round(JBUI.scale(BAR_WIDTH) * Math.min(row.totalUs() / (double)Math.max(row.frameUs(), 1), 1.0));
    if (fill > 0) {
      g.setColor(row.color());
      g.fillRoundRect(barLeft, barTop, Math.max(fill, barHeight), barHeight, barHeight, barHeight);
    }

    int percent = row.percent();
    String share = (percent == 0 ? "<1" : String.valueOf(percent)) + " %";
    g.setColor(UIUtil.getContextHelpForeground());
    g.setFont(JBFont.label());
    FontMetrics percentMetrics = g.getFontMetrics();
    g.drawString(share, percentRight - percentMetrics.stringWidth(share), textBaseline);
  }

  private static String truncate(String text, FontMetrics metrics, int available) {
    if (metrics.stringWidth(text) <= available) return text;
    String ellipsis = "…";
    int end = text.length();
    while (end > 1 && metrics.stringWidth(text.substring(0, end) + ellipsis) > available) {
      end--;
    }
    return text.substring(0, end) + ellipsis;
  }
}
