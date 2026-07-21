package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import lombok.Data;

@Data
public class StackTraceArguments {
  private int threadId;
  private Integer startFrame;
  private Integer levels;
}
