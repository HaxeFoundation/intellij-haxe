package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import lombok.Data;

@Data
public class StepOutArguments {
  private int threadId;
  private Boolean singleThread;
  private String granularity;
}
