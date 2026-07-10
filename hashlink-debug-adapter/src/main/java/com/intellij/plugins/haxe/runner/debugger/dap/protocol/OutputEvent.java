package com.intellij.plugins.haxe.runner.debugger.dap.protocol;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Carries debuggee stdout/stderr output forwarded by the adapter.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OutputEvent extends Event {
  public static final String EVENT = "output";

  private OutputEventBody body;

  public OutputEvent() {
    super(EVENT);
  }
}
