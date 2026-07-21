package com.intellij.plugins.haxe.runner.debugger.exceptions;

import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.ui.XBreakpointCustomPropertiesPanel;
import java.awt.GridLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.jetbrains.annotations.NotNull;

/**
 * The "Notifications" section of a Haxe exception breakpoint's editor,
 * modelled on the Java debugger's: checkboxes selecting which stops the
 * "Any exception" breakpoint produces. Hidden for per-class breakpoints —
 * those always mean "stop when this class is thrown, caught or not"
 * (neither runtime can type-filter an uncaught or critical stop).
 */
final class HaxeExceptionBreakpointPropertiesPanel
  extends XBreakpointCustomPropertiesPanel<XBreakpoint<HaxeExceptionBreakpointProperties>> {

  private final JBCheckBox caught = new JBCheckBox("Caught exception");
  private final JBCheckBox uncaught = new JBCheckBox("Uncaught exception");
  private final JBCheckBox critical = new JBCheckBox("Critical errors (null access, out-of-bounds, ...)");
  private final JPanel panel;

  HaxeExceptionBreakpointPropertiesPanel() {
    panel = new JPanel(new GridLayout(0, 1));
    panel.setBorder(IdeBorderFactory.createTitledBorder("Notifications"));
    panel.add(caught);
    panel.add(uncaught);
    panel.add(critical);
  }

  @Override
  public @NotNull JComponent getComponent() {
    return panel;
  }

  @Override
  public void loadFrom(@NotNull XBreakpoint<HaxeExceptionBreakpointProperties> breakpoint) {
    HaxeExceptionBreakpointProperties properties = breakpoint.getProperties();
    boolean typed = properties == null || properties.isTyped();
    panel.setVisible(!typed);
    if (properties != null) {
      caught.setSelected(properties.notifyCaught);
      uncaught.setSelected(properties.notifyUncaught);
      critical.setSelected(properties.notifyCritical);
    }
  }

  @Override
  public void saveTo(@NotNull XBreakpoint<HaxeExceptionBreakpointProperties> breakpoint) {
    HaxeExceptionBreakpointProperties properties = breakpoint.getProperties();
    if (properties == null || properties.isTyped()) {
      return; // per-class breakpoints have no notification choices
    }
    boolean changed = properties.notifyCaught != caught.isSelected()
                      || properties.notifyUncaught != uncaught.isSelected()
                      || properties.notifyCritical != critical.isSelected();
    properties.notifyCaught = caught.isSelected();
    properties.notifyUncaught = uncaught.isSelected();
    properties.notifyCritical = critical.isSelected();

    if (changed && breakpoint.isEnabled()) {
      // forces an "update state and notify" so our debugger can update its internal state
      breakpoint.setEnabled(false);
      breakpoint.setEnabled(true);
    }
  }
}
