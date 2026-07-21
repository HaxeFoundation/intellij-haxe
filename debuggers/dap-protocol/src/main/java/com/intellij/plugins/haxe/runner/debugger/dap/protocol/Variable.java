package com.intellij.plugins.haxe.runner.debugger.dap.protocol;

import lombok.Data;

/**
 * One variable in a "variables" response. {@code variablesReference} > 0 marks an
 * expandable value (object/array); 0 means a leaf.
 */
@Data
public class Variable {
  private String name;
  private String value;
  private int variablesReference;
  private String type;
  private Integer namedVariables;
  private Integer indexedVariables;
  /** Our classification for the client's icon; UNSPECIFIED when the wire omits it. */
  private VariableKind kind = VariableKind.UNSPECIFIED;
}
