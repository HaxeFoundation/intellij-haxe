package com.intellij.plugins.haxe.runner.debugger;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextPane;

/**
 * The Settings → Debugger → Haxe page's UI, laid out entirely in the bound
 * .form file (editable in the GUI Designer). The warning above the checkbox
 * is a non-editable, non-opaque {@link JTextPane}: pinned to a small
 * preferred width with horizontal fill, it wraps to the pane's actual width
 * without ever demanding one (see the comment in the .form).
 */
public class HaxeDebuggerSettingsForm {
  private JPanel myPanel;
  private JTextPane myWarningText;
  private JCheckBox myRenderWithToString;

  public HaxeDebuggerSettingsForm() {
    myWarningText.setEditable(false);
  }

  public JComponent getPanel() {
    return myPanel;
  }

  public boolean isModified(HaxeDebuggerSettings settings) {
    return myRenderWithToString.isSelected() != settings.isRenderObjectsWithToString();
  }

  public void applyEditorTo(HaxeDebuggerSettings settings) {
    settings.setRenderObjectsWithToString(myRenderWithToString.isSelected());
  }

  public void resetEditorFrom(HaxeDebuggerSettings settings) {
    myRenderWithToString.setSelected(settings.isRenderObjectsWithToString());
  }
}
