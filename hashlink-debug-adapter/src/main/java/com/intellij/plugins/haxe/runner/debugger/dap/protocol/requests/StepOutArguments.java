package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import lombok.Data;

/**
 * Arguments for the "stepOut" request.
 */
@Data
public class StepOutArguments {
  private int threadId;
  private Boolean singleThread;
  private String granularity;
}
