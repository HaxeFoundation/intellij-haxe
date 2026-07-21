package com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Breakpoint;

import java.util.List;
import lombok.Data;

@Data
public class SetBreakpointsResponseBody {
  private List<Breakpoint> breakpoints;
}
