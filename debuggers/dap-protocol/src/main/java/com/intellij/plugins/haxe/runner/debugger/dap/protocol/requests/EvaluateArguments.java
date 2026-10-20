package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import lombok.Data;

/**
 * Arguments of the "evaluate" request: an expression in the language of the
 * frame it is evaluated in.
 */
@Data
public class EvaluateArguments {
  private String expression;
  private Integer frameId;
  private String context; // the DAP hint where the expression comes from: "watch" | "repl" | "hover"
}
