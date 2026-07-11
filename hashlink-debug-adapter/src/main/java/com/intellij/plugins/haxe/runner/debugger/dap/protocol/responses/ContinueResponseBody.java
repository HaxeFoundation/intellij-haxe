package com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses;

import lombok.Data;

/**
 * Body of the "continue" response.
 */
@Data
public class ContinueResponseBody {
  private Boolean allThreadsContinued;
}
