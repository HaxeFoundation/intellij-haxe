package com.intellij.plugins.haxe.runner.debugger.dap.protocol;

import lombok.Data;

/**
 * Body of the "exited" event.
 */
@Data
public class ExitedEventBody {
  private int exitCode;
}
