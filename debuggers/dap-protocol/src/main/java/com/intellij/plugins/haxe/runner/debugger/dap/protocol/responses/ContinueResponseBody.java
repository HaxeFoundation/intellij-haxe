package com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses;

import lombok.Data;

@Data
public class ContinueResponseBody {
  private Boolean allThreadsContinued;
}
