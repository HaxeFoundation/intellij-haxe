package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Custom (non-standard DAP) request understood by the EVAL debug adapter:
 * switch stepping between LINE semantics (off, the default — sub-expression
 * steps are coalesced so step verbs behave like every other debugger) and
 * EXPRESSION semantics (on — each step is ONE interpreter sub-step, and the
 * stack frames' column/endColumn identify the exact sub-expression about to
 * run, which the IDE highlights). Only the eval target can do this: it is an
 * AST interpreter with a position per expression node; compiled targets have
 * line-level debug info only. Live, no restart — a Variables-view gear
 * toggle, like {@link SetToStringRenderingRequest}.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SetExpressionSteppingRequest extends Request {
  public static final String COMMAND = "custom/setExpressionStepping";

  private SetExpressionSteppingArguments arguments;

  public SetExpressionSteppingRequest() {
    super(COMMAND);
  }

  public static SetExpressionSteppingRequest of(boolean enabled) {
    SetExpressionSteppingRequest request = new SetExpressionSteppingRequest();
    SetExpressionSteppingArguments arguments = new SetExpressionSteppingArguments();
    arguments.setEnabled(enabled);
    request.setArguments(arguments);
    return request;
  }
}
