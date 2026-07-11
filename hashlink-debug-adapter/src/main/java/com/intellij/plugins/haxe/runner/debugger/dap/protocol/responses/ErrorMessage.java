package com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses;

import lombok.Data;

/**
 * A structured error message (DAP "Message" type), used in error response bodies.
 */
@Data
public class ErrorMessage {
  private int id;
  private String format;
  private Boolean showUser;
}
