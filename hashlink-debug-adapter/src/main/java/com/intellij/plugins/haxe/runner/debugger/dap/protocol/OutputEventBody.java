package com.intellij.plugins.haxe.runner.debugger.dap.protocol;

import lombok.Data;

/**
 * Body of the "output" event. {@code category} is "stdout" or "stderr"
 * for forwarded debuggee output.
 */
@Data
public class OutputEventBody {
  private String output;
  private String category;
}
