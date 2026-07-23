package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class StepInRequest extends Request {
  public static final String COMMAND = "stepIn";

  private StepInArguments arguments;

  public StepInRequest() {
    super(COMMAND);
  }

  public static StepInRequest of(int threadId) {
    StepInRequest request = new StepInRequest();
    StepInArguments arguments = new StepInArguments();
    arguments.setThreadId(threadId);
    request.setArguments(arguments);
    return request;
  }

  /** Step into the SPECIFIC call site chosen from the step-in targets. */
  public static StepInRequest of(int threadId, int targetId) {
    StepInRequest request = of(threadId);
    request.getArguments().setTargetId(targetId);
    return request;
  }
}
