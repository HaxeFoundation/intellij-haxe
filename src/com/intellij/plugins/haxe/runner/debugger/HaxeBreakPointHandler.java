package com.intellij.plugins.haxe.runner.debugger;

import com.intellij.xdebugger.breakpoints.XBreakpointHandler;
import com.intellij.xdebugger.breakpoints.XBreakpointProperties;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import org.jetbrains.annotations.NotNull;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeBreakPointHandler extends XBreakpointHandler<XLineBreakpoint<XBreakpointProperties>> {
  private final XBreakpointHandler<XLineBreakpoint<XBreakpointProperties>> myHandler;

  public HaxeBreakPointHandler(XBreakpointHandler<XLineBreakpoint<XBreakpointProperties>> handler) {
    super(HaxeBreakpointType.class);
    myHandler = handler;
  }

  @Override
  public void registerBreakpoint(@NotNull XLineBreakpoint<XBreakpointProperties> breakpoint) {
    myHandler.registerBreakpoint(breakpoint);
  }

  @Override
  public void unregisterBreakpoint(@NotNull XLineBreakpoint<XBreakpointProperties> breakpoint, boolean temporary) {
    myHandler.unregisterBreakpoint(breakpoint, temporary);
  }
}
