package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import lombok.Data;

/**
 * Arguments of the "evaluate" request. The adapter evaluates VARIABLE PATHS
 * (identifier + .field / [index] accessors), not arbitrary expressions.
 */
@Data
public class EvaluateArguments {
  private String expression;
  private Integer frameId;
  private String context; // "watch" | "hover" | "repl" - all treated alike
}
