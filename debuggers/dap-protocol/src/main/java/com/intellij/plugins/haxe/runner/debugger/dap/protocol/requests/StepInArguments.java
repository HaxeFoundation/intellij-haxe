package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import lombok.Data;

@Data
public class StepInArguments {
  private int threadId;
  private Boolean singleThread;
  private Integer targetId;
  private String granularity;
}
