package com.intellij.plugins.haxe.runner.debugger.dap.protocol;

import lombok.Data;

/**
 * Body of an error response (success = false).
 */
@Data
public class ErrorResponseBody {
  private ErrorMessage error;
}
