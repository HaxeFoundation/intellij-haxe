package com.intellij.plugins.haxe.runner.debugger.dap.protocol;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Any response with success = false. The {@code command} field echoes the
 * failed request's command, so this type covers error replies to every request.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ErrorResponse extends Response {
  private ErrorResponseBody body;
}
