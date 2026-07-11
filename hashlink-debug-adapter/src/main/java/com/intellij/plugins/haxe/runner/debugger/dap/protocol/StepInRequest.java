package com.intellij.plugins.haxe.runner.debugger.dap.protocol;

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
}
