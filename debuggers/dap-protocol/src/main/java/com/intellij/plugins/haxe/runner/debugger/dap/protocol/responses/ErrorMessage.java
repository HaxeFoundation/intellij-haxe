package com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses;

import java.util.Map;
import lombok.Data;

/**
 * A structured error message (DAP "Message" type), used in error response bodies.
 * {@code id} is the stable machine-readable code the client branches on; {@code
 * variables} carries machine-readable details (e.g. the offending name).
 */
@Data
public class ErrorMessage {
  private int id;
  private String format;
  private Boolean showUser;
  private Map<String, String> variables;
}
