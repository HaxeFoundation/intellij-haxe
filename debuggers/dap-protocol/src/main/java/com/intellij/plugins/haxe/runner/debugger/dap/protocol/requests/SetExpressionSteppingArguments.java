package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import lombok.Data;

@Data
public class SetExpressionSteppingArguments {
  private boolean enabled;
}
