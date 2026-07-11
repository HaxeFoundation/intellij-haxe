package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import lombok.Data;

/**
 * Arguments for the "initialize" request.
 * Wrapper types are used for optional DAP fields so unset values are omitted from JSON.
 */
@Data
public class InitializeRequestArguments {
  private String adapterID;
  private String clientID;
  private String clientName;
  private String locale;
  private Boolean linesStartAt1;
  private Boolean columnsStartAt1;
  private String pathFormat;
  private Boolean supportsVariableType;
  private Boolean supportsRunInTerminalRequest;
}
