package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import lombok.Data;

/**
 * Arguments of the "setVariable" request: set the named child of a
 * variablesReference to `value` (a literal or another variable path).
 */
@Data
public class SetVariableArguments {
  private int variablesReference;
  private String name;
  private String value;
}
