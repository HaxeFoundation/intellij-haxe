package com.intellij.plugins.haxe.runner.debugger.dap.protocol;

import lombok.Data;

/**
 * Base type of all DAP messages.
 * {@code type} is one of "request", "response" or "event".
 */
@Data
public abstract class ProtocolMessage {
  private int seq;
  private String type;

  protected ProtocolMessage(String type) {
    this.type = type;
  }
}
