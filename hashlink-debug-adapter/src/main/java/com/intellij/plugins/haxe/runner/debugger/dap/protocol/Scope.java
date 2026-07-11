package com.intellij.plugins.haxe.runner.debugger.dap.protocol;

import lombok.Data;

/**
 * A named container of variables (e.g. "Locals") returned by "scopes".
 */
@Data
public class Scope {
  private String name;
  private int variablesReference;
  private Boolean expensive;
  private String presentationHint;
}
