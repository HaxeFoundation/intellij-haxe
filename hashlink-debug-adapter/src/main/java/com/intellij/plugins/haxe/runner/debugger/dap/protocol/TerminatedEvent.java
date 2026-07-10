package com.intellij.plugins.haxe.runner.debugger.dap.protocol;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Sent by the adapter when the debug session has ended.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TerminatedEvent extends Event {
  public static final String EVENT = "terminated";

  public TerminatedEvent() {
    super(EVENT);
  }
}
