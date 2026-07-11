package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class EvaluateRequest extends Request {
  public static final String COMMAND = "evaluate";

  private EvaluateArguments arguments;

  public EvaluateRequest() {
    super(COMMAND);
  }
}
