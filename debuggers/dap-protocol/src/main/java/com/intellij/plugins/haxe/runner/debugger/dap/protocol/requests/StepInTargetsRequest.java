package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class StepInTargetsRequest extends Request {
  public static final String COMMAND = "stepInTargets";

  private StepInTargetsArguments arguments;

  public StepInTargetsRequest() {
    super(COMMAND);
  }

  public static StepInTargetsRequest of(int frameId) {
    StepInTargetsRequest request = new StepInTargetsRequest();
    StepInTargetsArguments arguments = new StepInTargetsArguments();
    arguments.setFrameId(frameId);
    request.setArguments(arguments);
    return request;
  }
}
