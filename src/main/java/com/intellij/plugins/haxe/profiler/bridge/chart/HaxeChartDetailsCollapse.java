package com.intellij.plugins.haxe.profiler.bridge.chart;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.popup.IconButton;
import com.intellij.plugins.haxe.HaxeProfilerBundle;
import com.intellij.ui.InplaceButton;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.components.BorderLayoutPanel;

import java.awt.Dimension;
import javax.swing.JComponent;

/**
 * Collapses the details pane into a thin strip at the right edge, freeing
 * the width for the chart; the strip's chevron brings it back. The choice
 * persists with the view settings, and the expanded width survives a
 * collapse/expand round trip.
 */
final class HaxeChartDetailsCollapse {
  private final OnePixelSplitter splitter;
  private final HaxeChartViewSettings settings;
  private final JComponent expandedPane;
  private final JComponent collapsedStrip;
  private float expandedProportion;

  HaxeChartDetailsCollapse(OnePixelSplitter splitter, HaxeChartDetailsPanel details, HaxeChartViewSettings settings) {
    this.splitter = splitter;
    this.settings = settings;
    expandedProportion = splitter.getProportion();
    expandedPane = paneWithCollapseButton(details);
    collapsedStrip = strip();
    if (settings.isDetailsCollapsed()) {
      splitter.setSecondComponent(collapsedStrip);
      splitter.setProportion(1f);
    }
    else {
      splitter.setSecondComponent(expandedPane);
    }
  }

  private JComponent paneWithCollapseButton(HaxeChartDetailsPanel details) {
    IconButton icon = new IconButton(HaxeProfilerBundle.message("haxe.profiler.callchart.details.collapse"),
                                     AllIcons.General.ArrowRight);
    BorderLayoutPanel bar = new BorderLayoutPanel();
    bar.addToRight(new InplaceButton(icon, event -> collapse()));
    BorderLayoutPanel pane = new BorderLayoutPanel();
    pane.addToTop(bar);
    pane.addToCenter(details);
    return pane;
  }

  private JComponent strip() {
    IconButton icon = new IconButton(HaxeProfilerBundle.message("haxe.profiler.callchart.details.expand"),
                                     AllIcons.General.ArrowLeft);
    BorderLayoutPanel strip = new BorderLayoutPanel() {
      @Override
      public Dimension getMinimumSize() {
        return new Dimension(JBUI.scale(24), 0);
      }
    };
    strip.addToTop(new InplaceButton(icon, event -> expand()));
    return strip;
  }

  private void collapse() {
    expandedProportion = splitter.getProportion();
    splitter.setSecondComponent(collapsedStrip);
    splitter.setProportion(1f);
    settings.setDetailsCollapsed(true);
  }

  private void expand() {
    splitter.setSecondComponent(expandedPane);
    splitter.setProportion(expandedProportion);
    settings.setDetailsCollapsed(false);
  }
}
