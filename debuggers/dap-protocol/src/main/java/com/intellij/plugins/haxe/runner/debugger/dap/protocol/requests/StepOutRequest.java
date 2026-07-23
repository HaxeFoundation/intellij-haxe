package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class StepOutRequest extends Request {
  public static final String COMMAND = "stepOut";

  private StepOutArguments arguments;

  public StepOutRequest() {
    super(COMMAND);
  }

  public static StepOutRequest of(int threadId) {
    StepOutRequest request = new StepOutRequest();
    StepOutArguments arguments = new StepOutArguments();
    arguments.setThreadId(threadId);
    request.setArguments(arguments);
    return request;
  }
}
