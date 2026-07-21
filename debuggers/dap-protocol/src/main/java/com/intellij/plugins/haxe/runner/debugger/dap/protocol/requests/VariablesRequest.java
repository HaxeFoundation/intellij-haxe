package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class VariablesRequest extends Request {
  public static final String COMMAND = "variables";

  private VariablesArguments arguments;

  public VariablesRequest() {
    super(COMMAND);
  }
}
