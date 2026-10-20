package com.intellij.plugins.haxe.util.ui;

import com.intellij.util.ui.UIUtil;
import javax.swing.JTextPane;
import java.awt.Dimension;
import org.jetbrains.annotations.NotNull;

/**
 * Styling for the gray hint panes under a dialog's fields. A JTextPane (not a
 * label) so long hints WRAP instead of dictating the dialog's minimum width.
 */
public final class HaxeDialogHints {

  private HaxeDialogHints() {
  }

  public static void style(@NotNull JTextPane hint) {
    hint.setForeground(UIUtil.getContextHelpForeground());
    hint.setFont(UIUtil.getLabelFont(UIUtil.FontSize.SMALL));
    hint.setBorder(null);
  }

  /**
   * Fixes the hint's preferred height to its text wrapped at {@code width}
   * (scaled pixels). Until laid out, a text pane reports one unwrapped line,
   * so a row that gets no spare height would clip the rest.
   */
  public static void fitHeight(@NotNull JTextPane hint, int width) {
    hint.setSize(width, Short.MAX_VALUE);
    hint.setPreferredSize(new Dimension(width, hint.getPreferredSize().height));
  }
}
