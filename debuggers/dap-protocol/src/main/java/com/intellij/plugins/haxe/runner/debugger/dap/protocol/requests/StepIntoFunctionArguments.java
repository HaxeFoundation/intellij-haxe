package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import lombok.Data;

/**
 * Arguments for the custom "intellij/stepIntoFunction" request: the callee to
 * enter, named the way hxcpp generated code names its stack frames — dotted
 * class path ("my.pack.Target") plus bare function name ("update").
 */
@Data
public class StepIntoFunctionArguments {
  private int threadId;
  private String className;
  private String functionName;
  /**
   * Which invocation of the callee on the stopped line to enter (1-based),
   * for lines calling the same function more than once
   * ({@code cfg.test1(1)...test1(2)}). Optional: servers that predate the
   * field ignore it and enter the first invocation.
   */
  private int occurrence = 1;
}
