package com.intellij.plugins.haxe.runner.debugger.dap.protocol.events;

import java.util.List;
import lombok.Data;

/**
 * Body of the "stopped" event. {@code reason} is e.g. "breakpoint" or "exception".
 */
@Data
public class StoppedEventBody {
  private String reason;
  private Integer threadId;
  private Boolean allThreadsStopped;
  private List<Integer> hitBreakpointIds;
  private String description;
}
