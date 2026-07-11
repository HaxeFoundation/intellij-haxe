package com.intellij.plugins.haxe.runner.debugger.dap.protocol;

import lombok.Data;

/**
 * Arguments for the "stepIn" request.
 */
@Data
public class StepInArguments {
  private int threadId;
  private Boolean singleThread;
  private Integer targetId;
  private String granularity;
}
