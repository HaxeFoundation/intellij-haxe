package com.intellij.plugins.haxe.runner.debugger.dap.protocol.events;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Sent by the adapter when the debuggee resumed on its own — a step that had no
 * user-code landing (e.g. stepping past a thread entry's last statement) is
 * downgraded to a continue, and this tells the client the debuggee is running.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ContinuedEvent extends Event {
  public static final String EVENT = "continued";

  private ContinuedEventBody body;

  public ContinuedEvent() {
    super(EVENT);
  }
}
