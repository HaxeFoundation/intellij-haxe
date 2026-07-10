package com.intellij.plugins.haxe.runner.debugger.dap.protocol;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Sent by the adapter when the debuggee process has exited.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ExitedEvent extends Event {
  public static final String EVENT = "exited";

  private ExitedEventBody body;

  public ExitedEvent() {
    super(EVENT);
  }
}
