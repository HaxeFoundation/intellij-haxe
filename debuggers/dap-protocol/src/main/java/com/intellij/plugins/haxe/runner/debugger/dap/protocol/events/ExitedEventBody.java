package com.intellij.plugins.haxe.runner.debugger.dap.protocol.events;

import lombok.Data;

@Data
public class ExitedEventBody {
  private int exitCode;
}
