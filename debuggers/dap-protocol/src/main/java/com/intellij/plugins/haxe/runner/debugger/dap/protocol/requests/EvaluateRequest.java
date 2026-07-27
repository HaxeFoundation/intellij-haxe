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

  public static EvaluateRequest of(int frameId, String expression, String context) {
    EvaluateRequest request = new EvaluateRequest();
    EvaluateArguments arguments = new EvaluateArguments();
    arguments.setFrameId(frameId);
    arguments.setExpression(expression);
    arguments.setContext(context);
    request.setArguments(arguments);
    return request;
  }
}
