package com.intellij.plugins.haxe.util.ui;

import com.intellij.util.ui.UIUtil;
import javax.swing.JTextPane;
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
}
