package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import lombok.Data;

/**
 * Arguments for the "next" (step over) request.
 */
@Data
public class NextArguments {
  private int threadId;
  private Boolean singleThread;
  private String granularity;
}
