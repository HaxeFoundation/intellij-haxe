package com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe.protocol;

/**
 * Method and notification names of the hxcpp-debug-server jsonrpc protocol
 * (Protocol.hx in vshaxe/hxcpp-debugger) — the subset we use. Protocol.hx
 * also declares setBreakpoint, removeBreakpoint, switchFrame, completions
 * and setExceptionOptions, but Server.hx has NO handler for those (an
 * unknown method gets a null-result success — see docs/README.md gotcha #7),
 * so they are deliberately absent here.
 */
public final class HxcppProtocol {
  // requests
  public static final String PAUSE = "pause";
  public static final String CONTINUE = "continue";
  public static final String STEP_IN = "stepIn";
  public static final String NEXT = "next";
  public static final String STEP_OUT = "stepOut";
  public static final String STACK_TRACE = "stackTrace";
  public static final String SET_BREAKPOINTS = "setBreakpoints";
  public static final String GET_SCOPES = "getScopes";
  public static final String GET_VARIABLES = "getVariables";
  public static final String SET_VARIABLE = "setVariable";
  public static final String THREADS = "threads";
  public static final String EVALUATE = "evaluate";

  // notifications ("ThreadExit" capitalisation is the protocol's, not a typo)
  public static final String NOTIFY_BREAKPOINT_STOP = "breakpointStop";
  public static final String NOTIFY_EXCEPTION_STOP = "exceptionStop";
  public static final String NOTIFY_PAUSE_STOP = "pauseStop";
  public static final String NOTIFY_THREAD_START = "threadStart";
  public static final String NOTIFY_THREAD_EXIT = "ThreadExit";

  private HxcppProtocol() {
  }
}
