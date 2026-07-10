package com.intellij.plugins.haxe.runner.debugger.dap.protocol;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class StackTraceRequest extends Request {
  public static final String COMMAND = "stackTrace";

  private StackTraceArguments arguments;

  public StackTraceRequest() {
    super(COMMAND);
  }
}
