package com.intellij.plugins.haxe.runner.debugger.dap.protocol;

import lombok.Data;

@Data
public class Capabilities {
  private Boolean supportsConfigurationDoneRequest;
  private Boolean supportsVariableType;
  private Boolean supportsEvaluateForHovers;
  private Boolean supportsSetVariable;
  private Boolean supportsConditionalBreakpoints;
  private Boolean supportsStepInTargetsRequest;
  private Boolean supportsCompletionsRequest;
}
