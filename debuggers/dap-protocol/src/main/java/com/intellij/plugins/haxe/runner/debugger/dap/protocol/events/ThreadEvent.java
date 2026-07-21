package com.intellij.plugins.haxe.runner.debugger.dap.protocol.events;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Sent by the adapter when a debuggee thread starts or exits.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ThreadEvent extends Event {
  public static final String EVENT = "thread";
  public static final String REASON_STARTED = "started";
  public static final String REASON_EXITED = "exited";

  private ThreadEventBody body;

  public ThreadEvent() {
    super(EVENT);
  }
}
