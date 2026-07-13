package com.intellij.plugins.haxe.hashlink;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointManager;
import com.intellij.xdebugger.breakpoints.XBreakpointType;
import javax.swing.JComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Per-class HashLink exception breakpoints (Java-style): the user clicks "+" in
 * the Breakpoints dialog, names an exception class, and gets a breakpoint that
 * stops only on throws of that class (or a subclass). Each breakpoint carries its
 * class name in {@link HashLinkExceptionBreakpointProperties}; the debug process
 * forwards the enabled class names to the adapter as type filters.
 */
public class HashLinkTypedExceptionBreakpointType
  extends XBreakpointType<XBreakpoint<HashLinkExceptionBreakpointProperties>, HashLinkExceptionBreakpointProperties> {

  public HashLinkTypedExceptionBreakpointType() {
    super("hashlink-typed-exception", "HashLink Exception Breakpoints");
  }

  @Override
  public @NotNull String getDisplayText(XBreakpoint<HashLinkExceptionBreakpointProperties> breakpoint) {
    HashLinkExceptionBreakpointProperties properties = breakpoint.getProperties();
    String className = properties == null ? null : properties.className;
    return (className == null || className.isBlank())
      ? "Any HashLink exception (typed)"
      : "HashLink exception: " + className;
  }

  @Override
  public @Nullable HashLinkExceptionBreakpointProperties createProperties() {
    return new HashLinkExceptionBreakpointProperties();
  }

  @Override
  public boolean isAddBreakpointButtonVisible() {
    return true;
  }

  @Override
  public @Nullable XBreakpoint<HashLinkExceptionBreakpointProperties> addBreakpoint(Project project, JComponent parentComponent) {
    String input = Messages.showInputDialog(project,
                                            "Exception class (fully-qualified or simple name):",
                                            "Add HashLink Exception Breakpoint",
                                            Messages.getQuestionIcon());
    if (input == null || input.isBlank()) {
      return null;
    }
    String className = input.trim();
    return WriteAction.compute(() -> {
      XBreakpointManager manager = XDebuggerManager.getInstance(project).getBreakpointManager();
      HashLinkExceptionBreakpointProperties properties = new HashLinkExceptionBreakpointProperties();
      properties.className = className;
      return manager.addBreakpoint(this, properties);
    });
  }
}
