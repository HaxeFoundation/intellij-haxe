package com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses;

import lombok.Data;

/**
 * Body of the "setVariable" response: the new rendered value, its type, and a
 * variablesReference when the new value is expandable (0 = leaf).
 */
@Data
public class SetVariableResponseBody {
  private String value;
  private String type;
  private int variablesReference;
}
