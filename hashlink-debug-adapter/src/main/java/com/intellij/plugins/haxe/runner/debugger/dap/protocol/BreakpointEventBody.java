package com.intellij.plugins.haxe.runner.debugger.dap.protocol;

import lombok.Data;

/**
 * Body of the "breakpoint" event.
 */
@Data
public class BreakpointEventBody {
  private String reason;
  private Breakpoint breakpoint;
}
