package com.intellij.plugins.haxe.runner.debugger.dap.protocol;

import java.util.List;
import lombok.Data;

/**
 * Body of the "setBreakpoints" response.
 */
@Data
public class SetBreakpointsResponseBody {
  private List<Breakpoint> breakpoints;
}
