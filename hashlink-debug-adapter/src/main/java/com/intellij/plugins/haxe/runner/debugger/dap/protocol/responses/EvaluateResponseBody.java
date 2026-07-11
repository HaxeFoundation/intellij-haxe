package com.intellij.plugins.haxe.runner.debugger.dap.protocol.responses;

import lombok.Data;

/**
 * Body of the "evaluate" response: rendered value, type label, and a
 * variablesReference when the result is expandable (0 = leaf).
 */
@Data
public class EvaluateResponseBody {
  private String result;
  private String type;
  private int variablesReference;
}
