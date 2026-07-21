package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import lombok.Data;

@Data
public class StepInTargetsArguments {
  private int frameId;
}
