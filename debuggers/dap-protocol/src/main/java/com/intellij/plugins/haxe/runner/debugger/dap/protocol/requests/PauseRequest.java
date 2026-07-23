package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PauseRequest extends Request {
  public static final String COMMAND = "pause";

  private PauseArguments arguments;

  public PauseRequest() {
    super(COMMAND);
  }

  public static PauseRequest of(int threadId) {
    PauseRequest request = new PauseRequest();
    PauseArguments arguments = new PauseArguments();
    arguments.setThreadId(threadId);
    request.setArguments(arguments);
    return request;
  }
}
