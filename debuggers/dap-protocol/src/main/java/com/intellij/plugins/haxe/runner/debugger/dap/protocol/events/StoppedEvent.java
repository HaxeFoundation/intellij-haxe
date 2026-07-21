package com.intellij.plugins.haxe.runner.debugger.dap.protocol.events;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Sent by the adapter when the debuggee stops (breakpoint hit, exception, ...).
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StoppedEvent extends Event {
  public static final String EVENT = "stopped";

  private StoppedEventBody body;

  public StoppedEvent() {
    super(EVENT);
  }
}
