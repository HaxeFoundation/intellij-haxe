package com.intellij.plugins.haxe.runner.debugger.dap.protocol;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Re-verifies a breakpoint that was answered provisionally (e.g. set before launch).
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BreakpointEvent extends Event {
  public static final String EVENT = "breakpoint";

  private BreakpointEventBody body;

  public BreakpointEvent() {
    super(EVENT);
  }
}
