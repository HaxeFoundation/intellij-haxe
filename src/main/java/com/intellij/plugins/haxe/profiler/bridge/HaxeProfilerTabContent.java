package com.intellij.plugins.haxe.profiler.bridge;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import org.jetbrains.annotations.NotNull;

import java.awt.Container;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * Replaces a profiler tab's content component in place when a live
 * capture's completion rebuild swaps the partial view for the final one.
 * The platform's process panel exposes its content through
 * {@link SimpleToolWindowPanel#getContent()} and compare-snapshots CASTS
 * that to MainCallTreeDataComponent to map a component back to its tab —
 * hiding the component behind a wrapper panel makes that lookup throw for
 * every tab action, so the swap happens at the panel itself (or, failing
 * that, in whatever container the platform put the component in).
 */
public final class HaxeProfilerTabContent {

  private HaxeProfilerTabContent() {
  }

  public static void swap(@NotNull JComponent current, @NotNull JComponent replacement) {
    SimpleToolWindowPanel panel =
      (SimpleToolWindowPanel)SwingUtilities.getAncestorOfClass(SimpleToolWindowPanel.class, current);
    if (panel != null && panel.getContent() == current) {
      panel.setContent(replacement);
      panel.revalidate();
      panel.repaint();
      return;
    }
    Container parent = current.getParent();
    if (parent == null) {
      Logger.getInstance(HaxeProfilerTabContent.class)
        .warn("live capture completed but its tab content is not attached - keeping the live view");
      return;
    }
    int index = parent.getComponentZOrder(current);
    parent.remove(current);
    parent.add(replacement, index);
    parent.revalidate();
    parent.repaint();
  }
}
