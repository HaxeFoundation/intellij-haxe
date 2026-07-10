package com.intellij.plugins.haxe.runner.debugger.dap.protocol;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * A response to a request (type = "response").
 * {@code request_seq} echoes the {@code seq} of the request being answered.
 * The field name intentionally keeps the DAP wire name so no JSON mapping is needed.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Response extends ProtocolMessage {
  public static final String TYPE = "response";

  private int request_seq;
  private boolean success;
  private String command;
  private String message;

  public Response() {
    super(TYPE);
  }
}
