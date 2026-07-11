package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import lombok.Data;

/**
 * Arguments for the "continue" request.
 */
@Data
public class ContinueArguments {
  private int threadId;
}
