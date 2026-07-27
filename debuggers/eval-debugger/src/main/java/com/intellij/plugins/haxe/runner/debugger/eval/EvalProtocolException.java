package com.intellij.plugins.haxe.runner.debugger.eval;

import java.io.IOException;

/**
 * An error RESPONSE from the eval VM (JSON-RPC error object): the transport
 * is healthy, the VM just refused this particular request.
 */
public final class EvalProtocolException extends IOException {
  private final int code;

  public EvalProtocolException(int code, String message) {
    super("Eval VM error " + code + ": " + message);
    this.code = code;
  }

  public int getCode() {
    return code;
  }
}
