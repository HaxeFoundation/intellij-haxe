package com.intellij.plugins.haxe.runner.debugger.dap.protocol.events;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Sent by the adapter after the "initialize" response to signal it is ready
 * to accept configuration requests.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class InitializedEvent extends Event {
  public static final String EVENT = "initialized";

  public InitializedEvent() {
    super(EVENT);
  }
}
