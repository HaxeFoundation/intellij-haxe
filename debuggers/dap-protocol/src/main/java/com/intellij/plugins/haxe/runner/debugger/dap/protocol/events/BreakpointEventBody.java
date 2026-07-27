package com.intellij.plugins.haxe.runner.debugger.dap.protocol.events;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Breakpoint;

import lombok.Data;

@Data
public class BreakpointEventBody {
  private String reason;
  private Breakpoint breakpoint;
}
