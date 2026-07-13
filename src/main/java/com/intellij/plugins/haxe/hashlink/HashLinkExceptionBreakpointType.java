package com.intellij.plugins.haxe.hashlink;

import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XBreakpointType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The "Any HashLink exception" breakpoint: a single, non-line breakpoint in the
 * Breakpoints dialog that, when enabled, makes the debugger stop wherever an
 * exception is thrown. Enabling/disabling it drives {@code setExceptionBreakpoints}
 * on the adapter (see {@code HashLinkDebugProcess}). Present by default but off,
 * so it costs nothing until the user turns it on.
 */
public class HashLinkExceptionBreakpointType
  extends XBreakpointType<XBreakpoint<XBreakpointProperties>, XBreakpointProperties> {

  public HashLinkExceptionBreakpointType() {
    super("hashlink-exception", "HashLink Exceptions");
  }

  @Override
  public @NotNull String getDisplayText(XBreakpoint<XBreakpointProperties> breakpoint) {
    return "Any HashLink exception";
  }

  @Override
  public @Nullable XBreakpointProperties createProperties() {
    return null;
  }

  // only the single "any exception" toggle — no user-added variants
  @Override
  public boolean isAddBreakpointButtonVisible() {
    return false;
  }

  @Override
  public XBreakpoint<XBreakpointProperties> createDefaultBreakpoint(@NotNull XBreakpointCreator<XBreakpointProperties> creator) {
    XBreakpoint<XBreakpointProperties> breakpoint = creator.createBreakpoint(null);
    breakpoint.setEnabled(false); // off by default; the user opts in
    return breakpoint;
  }
}
