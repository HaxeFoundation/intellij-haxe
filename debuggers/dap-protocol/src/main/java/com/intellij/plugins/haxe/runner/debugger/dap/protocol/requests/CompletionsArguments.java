package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import lombok.Data;

/**
 * Arguments of the "completions" request. {@code column} is 1-based (per the
 * initialize) and points INTO {@code text} where the caret sits; frameId
 * scopes the completion to a stack frame when stopped.
 */
@Data
public class CompletionsArguments {
  private Integer frameId;
  private String text;
  private Integer column;
  private Integer line;
}
