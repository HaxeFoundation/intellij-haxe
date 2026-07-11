package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Source;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.SourceBreakpoint;

import java.util.List;
import lombok.Data;

/**
 * Arguments for the "setBreakpoints" request.
 */
@Data
public class SetBreakpointsArguments {
  private Source source;
  private List<SourceBreakpoint> breakpoints;
  private List<Integer> lines;
  private Boolean sourceModified;
}
