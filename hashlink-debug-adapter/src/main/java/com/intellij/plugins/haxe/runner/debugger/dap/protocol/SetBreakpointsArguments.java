package com.intellij.plugins.haxe.runner.debugger.dap.protocol;

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
