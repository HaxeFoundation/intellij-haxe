package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SetVariableRequest extends Request {
  public static final String COMMAND = "setVariable";

  private SetVariableArguments arguments;

  public SetVariableRequest() {
    super(COMMAND);
  }
}
