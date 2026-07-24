package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Custom (non-standard DAP) request understood by the intellij-hxcpp-debug
 * server: smart step into the named function. Unlike the standard
 * stepInTargets/stepIn pair, the CLIENT names the callee — on hxcpp the
 * server has no line→calls knowledge (there is no bytecode to mine), so the
 * IDE resolves the calls on the stopped line through its PSI and sends the
 * chosen (className, functionName). The server races a temporary entry
 * breakpoint against a step-over and reports either landing as a step stop.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StepIntoFunctionRequest extends Request {
  public static final String COMMAND = "intellij/stepIntoFunction";

  private StepIntoFunctionArguments arguments;

  public StepIntoFunctionRequest() {
    super(COMMAND);
  }

  public static StepIntoFunctionRequest of(int threadId, String className, String functionName, int occurrence) {
    StepIntoFunctionRequest request = new StepIntoFunctionRequest();
    StepIntoFunctionArguments arguments = new StepIntoFunctionArguments();
    arguments.setThreadId(threadId);
    arguments.setClassName(className);
    arguments.setFunctionName(functionName);
    arguments.setOccurrence(occurrence);
    request.setArguments(arguments);
    return request;
  }
}
