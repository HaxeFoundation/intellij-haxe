package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * The "completions" request: the ADAPTER completes the given text against the
 * live runtime (capability supportsCompletionsRequest). The debug-console
 * fallback for identifiers the Haxe PSI cannot know — browser globals reached
 * through incomplete externs, dynamically attached fields, and so on.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CompletionsRequest extends Request {
  public static final String COMMAND = "completions";

  private CompletionsArguments arguments;

  public CompletionsRequest() {
    setCommand(COMMAND);
  }

  /** {@code frameId} may be null (no paused frame: complete against globals). */
  public static CompletionsRequest of(Integer frameId, String text, int column) {
    CompletionsRequest request = new CompletionsRequest();
    CompletionsArguments arguments = new CompletionsArguments();
    arguments.setFrameId(frameId);
    arguments.setText(text);
    arguments.setColumn(column);
    request.setArguments(arguments);
    return request;
  }
}
