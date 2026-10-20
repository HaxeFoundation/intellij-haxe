package com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses;

import lombok.Data;

/**
 * Body of the "exceptionInfo" response: what the debuggee is stopped on.
 * {@code breakMode} is "unhandled" (an uncatchable throw) or "always" (a
 * stop regardless of try/catch: a thrown exception, or a critical error).
 */
@Data
public class ExceptionInfoResponseBody {
  private String exceptionId;
  private String description;
  private String breakMode;
}
