package com.intellij.plugins.haxe.hashlink;

import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XBreakpointType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The "Uncaught HashLink exception" breakpoint: like {@link HashLinkExceptionBreakpointType}
 * but the debugger stops only at a throw that no live {@code try/catch} will
 * handle. Enabling/disabling it drives the "uncaught" filter of
 * {@code setExceptionBreakpoints}; present by default but off.
 */
public class HashLinkUncaughtExceptionBreakpointType
  extends XBreakpointType<XBreakpoint<XBreakpointProperties>, XBreakpointProperties> {

  public HashLinkUncaughtExceptionBreakpointType() {
    super("hashlink-uncaught-exception", "HashLink Uncaught Exceptions");
  }

  @Override
  public @NotNull String getDisplayText(XBreakpoint<XBreakpointProperties> breakpoint) {
    return "Any uncaught HashLink exception";
  }

  @Override
  public @Nullable XBreakpointProperties createProperties() {
    return null;
  }

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
