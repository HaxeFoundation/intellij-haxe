package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class InitializeRequest extends Request {
  public static final String COMMAND = "initialize";

  private InitializeRequestArguments arguments;

  public InitializeRequest() {
    super(COMMAND);
  }

  /**
   * The arguments every backend/adapter in this plugin sends: client
   * "intellij", native paths (vscode-firefox-debug REJECTS initialize without
   * pathFormat "path"), 1-based lines and columns. {@code supportsStartDebugging}
   * announces reverse-request support (the js-debug multi-session dance).
   */
  public static InitializeRequest standard(String adapterId, boolean supportsStartDebugging) {
    InitializeRequest request = new InitializeRequest();
    InitializeRequestArguments arguments = new InitializeRequestArguments();
    arguments.setClientID("intellij");
    arguments.setAdapterID(adapterId);
    arguments.setPathFormat("path");
    arguments.setLinesStartAt1(true);
    arguments.setColumnsStartAt1(true);
    if (supportsStartDebugging) {
      arguments.setSupportsStartDebuggingRequest(true);
    }
    request.setArguments(arguments);
    return request;
  }
}
