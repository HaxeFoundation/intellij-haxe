package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import java.util.List;
import lombok.Data;

/**
 * Arguments for the "setExceptionBreakpoints" request: the ids of the exception
 * filters the client wants active (e.g. "all" to break on every thrown exception).
 *
 * {@code filterTypes} is our extension for per-class exception breakpoints:
 * exception class names (FQN or simple) to stop on, matched against the thrown
 * value's class and its superclasses.
 */
@Data
public class SetExceptionBreakpointsArguments {
  private List<String> filters;
  private List<String> filterTypes;
}
