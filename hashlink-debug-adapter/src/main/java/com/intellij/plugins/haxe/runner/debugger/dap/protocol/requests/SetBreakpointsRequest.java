package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SetBreakpointsRequest extends Request {
  public static final String COMMAND = "setBreakpoints";

  private SetBreakpointsArguments arguments;

  public SetBreakpointsRequest() {
    super(COMMAND);
  }
}
