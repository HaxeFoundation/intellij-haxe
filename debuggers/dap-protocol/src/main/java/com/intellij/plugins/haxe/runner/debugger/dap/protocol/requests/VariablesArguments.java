package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import lombok.Data;

@Data
public class VariablesArguments {
  private int variablesReference;
  private String filter;
  private Integer start;
  private Integer count;
}
