package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import lombok.Data;

/**
 * Arguments for the "pause" request.
 */
@Data
public class PauseArguments {
  private int threadId;
}
