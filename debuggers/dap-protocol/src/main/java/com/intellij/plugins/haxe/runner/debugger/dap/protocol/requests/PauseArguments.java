package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import lombok.Data;

@Data
public class PauseArguments {
  private int threadId;
}
