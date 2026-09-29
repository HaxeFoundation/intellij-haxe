package com.intellij.plugins.haxe.profiler.bridge.chart;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.profiler.bridge.data.HaxeCallStackElement;
import com.intellij.plugins.haxe.profiler.model.StackFrame;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.components.BorderLayoutPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

/**
 * The Call Chart's details pane: header lines (the first in bold), then
 * optionally a frame breakdown and a screenshot, above a call stack listed
 * leaf first. Double-clicking a stack frame opens its Haxe source.
 */
final class HaxeChartDetailsPanel extends BorderLayoutPanel {
  private final Project project;
  private final JPanel header = new JPanel();
  private final HaxeFrameBreakdownView breakdown = new HaxeFrameBreakdownView();
  private final JBLabel image = new JBLabel();
  private final DefaultListModel<String> rows = new DefaultListModel<>();
  private final JBList<String> list = new JBList<>(rows);
  private List<StackFrame> leafFirstFrames = List.of();
  private int revision;

  HaxeChartDetailsPanel(@NotNull Project project) {
    this.project = project;
    header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
    header.setBorder(JBUI.Borders.empty(4, 6));
    breakdown.setBorder(JBUI.Borders.empty(2, 0));
    image.setBorder(JBUI.Borders.empty(0, 6, 4, 6));
    BorderLayoutPanel top = new BorderLayoutPanel();
    top.addToTop(header);
    top.addToCenter(breakdown);
    top.addToBottom(image);
    addToTop(top);
    // most selections carry no stack; the platform's "Nothing to show" would read as an error
    list.getEmptyText().setText("");
    addToCenter(new JBScrollPane(list));
    list.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent event) {
        if (event.getClickCount() == 2) {
          navigateToSelectedFrame();
        }
      }
    });
  }

  void showText(@NotNull String text) {
    showStack(List.of(text), List.of());
  }

  /** Header lines above the frame's share bars; replaces any stack rows. */
  void showBreakdown(@NotNull List<String> headerLines, @NotNull HaxeFrameBreakdownView.Rows content) {
    showStack(headerLines, List.of());
    breakdown.show(content);
  }

  void showStack(@NotNull List<String> headerLines, @NotNull List<StackFrame> rootFirstStack) {
    revision++;
    header.removeAll();
    for (String line : headerLines) {
      JBLabel label = new JBLabel(line);
      if (header.getComponentCount() == 0) {
        label.setFont(JBFont.label().asBold());
      }
      header.add(label);
    }
    breakdown.show(new HaxeFrameBreakdownView.Rows(List.of(), List.of()));
    image.setIcon(null);
    header.revalidate();
    header.repaint();

    rows.clear();
    leafFirstFrames = rootFirstStack.reversed();
    for (StackFrame frame : leafFirstFrames) {
      rows.addElement(frame.symbol());
    }
  }

  /**
   * Bumped by every {@link #showStack}. An asynchronous loader reads it after
   * its initial render and applies its result only while it still matches,
   * so a newer selection wins.
   */
  int revision() {
    return revision;
  }

  /** A screenshot below the header lines; null clears the slot. */
  void showImage(@Nullable Image screenshot) {
    image.setIcon(screenshot == null ? null : new ImageIcon(screenshot));
  }

  private void navigateToSelectedFrame() {
    int index = list.getSelectedIndex();
    if (index < 0 || index >= leafFirstFrames.size()) return;
    HaxeCallStackElement.navigateToFrame(project, leafFirstFrames.get(index));
  }
}
