package com.intellij.plugins.haxe.runner.debugger.dap.protocol;

import lombok.Data;

/**
 * The capabilities the adapter reports in the "initialize" response body.
 */
@Data
public class Capabilities {
  private Boolean supportsConfigurationDoneRequest;
  private Boolean supportsVariableType;
}
