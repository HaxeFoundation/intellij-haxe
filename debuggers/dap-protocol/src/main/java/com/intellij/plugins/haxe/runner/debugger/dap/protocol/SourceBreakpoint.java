package com.intellij.plugins.haxe.runner.debugger.dap.protocol;

import lombok.Data;

/**
 * A breakpoint location as requested by the client in "setBreakpoints".
 */
@Data
public class SourceBreakpoint {
  private int line;
  private Integer column;
  private String condition;
  private String hitCondition;
  private String logMessage;
}
