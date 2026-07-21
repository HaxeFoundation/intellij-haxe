package com.intellij.plugins.haxe.runner.debugger.dap.protocol;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Request extends ProtocolMessage {
  public static final String TYPE = "request";

  private String command;

  public Request() {
    super(TYPE);
  }

  protected Request(String command) {
    this();
    this.command = command;
  }
}
