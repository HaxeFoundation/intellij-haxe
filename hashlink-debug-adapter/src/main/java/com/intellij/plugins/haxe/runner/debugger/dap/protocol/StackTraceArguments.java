package com.intellij.plugins.haxe.runner.debugger.dap.protocol;

import lombok.Data;

/**
 * Arguments for the "stackTrace" request.
 */
@Data
public class StackTraceArguments {
  private int threadId;
  private Integer startFrame;
  private Integer levels;
}
