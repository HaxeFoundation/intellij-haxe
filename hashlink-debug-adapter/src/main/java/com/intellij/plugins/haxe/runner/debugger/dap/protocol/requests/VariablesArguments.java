package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import lombok.Data;

/**
 * Arguments for the "variables" request.
 */
@Data
public class VariablesArguments {
  private int variablesReference;
  private String filter;
  private Integer start;
  private Integer count;
}
