package com.intellij.plugins.haxe.runner.debugger.dap.protocol.events;

import lombok.Data;

@Data
public class ContinuedEventBody {
  private int threadId;
  private Boolean allThreadsContinued;
}
