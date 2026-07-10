package com.intellij.plugins.haxe.runner.debugger.dap.protocol;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * An adapter-initiated event (type = "event").
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Event extends ProtocolMessage {
  public static final String TYPE = "event";

  private String event;

  public Event() {
    super(TYPE);
  }

  protected Event(String event) {
    this();
    this.event = event;
  }
}
