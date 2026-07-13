package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import java.util.List;
import lombok.Data;

/**
 * Arguments for the "setExceptionBreakpoints" request: the ids of the exception
 * filters the client wants active (e.g. "all" to break on every thrown exception).
 */
@Data
public class SetExceptionBreakpointsArguments {
  private List<String> filters;
}
