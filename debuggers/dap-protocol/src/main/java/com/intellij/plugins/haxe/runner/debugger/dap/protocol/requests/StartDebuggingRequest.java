package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * The {@code startDebugging} REVERSE request (adapter -> client, DAP 1.59+):
 * the adapter asks the client to open a child debug session with the given
 * configuration. vscode-js-debug uses this to hand over the actual browser
 * target — the child's configuration carries a {@code __pendingTargetId} the
 * server matches when the client connects a new session with it.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class StartDebuggingRequest extends Request {
  public static final String COMMAND = "startDebugging";

  private Arguments arguments;

  public StartDebuggingRequest() {
    setCommand(COMMAND);
  }

  @Data
  public static class Arguments {
    /** The child session's launch/attach configuration (adapter-specific keys). */
    private Map<String, Object> configuration;
    /** "launch" or "attach". */
    private String request;
  }
}
