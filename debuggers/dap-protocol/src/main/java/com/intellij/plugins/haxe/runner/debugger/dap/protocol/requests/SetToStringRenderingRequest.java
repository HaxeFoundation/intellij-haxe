package com.intellij.plugins.haxe.runner.debugger.dap.protocol.requests;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Custom (non-standard DAP) request understood by the Haxe debug backends:
 * switch object VALUE LABELS between the class name (off, the default) and
 * the result of the object's own {@code toString()} (on). Sent at session
 * start when the project setting is enabled, and again whenever the user
 * flips the Variables-view gear toggle — the setting is live, no restart.
 *
 * toString is USER CODE run by the debugger; the off-default guards against
 * side effects, and on hxcpp a self-recursing toString is fatal to the
 * debuggee (stack overflow is uncatchable there), which is why this is an
 * explicit opt-in rather than always-on like the vshaxe server.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SetToStringRenderingRequest extends Request {
  public static final String COMMAND = "custom/setToStringRendering";

  private SetToStringRenderingArguments arguments;

  public SetToStringRenderingRequest() {
    super(COMMAND);
  }

  public static SetToStringRenderingRequest of(boolean enabled) {
    SetToStringRenderingRequest request = new SetToStringRenderingRequest();
    SetToStringRenderingArguments arguments = new SetToStringRenderingArguments();
    arguments.setEnabled(enabled);
    request.setArguments(arguments);
    return request;
  }
}
