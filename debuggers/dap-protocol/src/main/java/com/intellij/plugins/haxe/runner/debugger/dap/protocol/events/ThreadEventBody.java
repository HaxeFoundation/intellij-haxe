package com.intellij.plugins.haxe.runner.debugger.dap.protocol.events;

import lombok.Data;

/**
 * Body of the "thread" event. {@code reason} is "started" or "exited".
 */
@Data
public class ThreadEventBody {
  private String reason;
  private int threadId;
}
