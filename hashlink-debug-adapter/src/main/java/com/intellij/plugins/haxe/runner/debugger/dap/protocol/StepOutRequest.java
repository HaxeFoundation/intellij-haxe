package com.intellij.plugins.haxe.runner.debugger.dap.protocol;

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
}
