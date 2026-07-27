package com.intellij.plugins.haxe.runner.debugger.dap.protocol;

import lombok.Data;

/**
 * The adapter's view of a breakpoint, returned in the "setBreakpoints" response.
 */
@Data
public class Breakpoint {
  private boolean verified;
  private Integer id;
  private String message;
  private Source source;
  private Integer line;
  private Integer column;
}
