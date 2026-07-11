package com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.StackFrame;

import java.util.List;
import lombok.Data;

/**
 * Body of the "stackTrace" response.
 */
@Data
public class StackTraceResponseBody {
  private List<StackFrame> stackFrames;
  private Integer totalFrames;
}
