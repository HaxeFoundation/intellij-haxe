package com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe.jsonrpc;

import java.io.IOException;
import lombok.Getter;

/** Thrown by {@link JsonRpcClient#call} when the server answers a request with an error. */
@Getter
public class JsonRpcErrorException extends IOException {
  private final String method;
  private final JsonRpcError error;

  public JsonRpcErrorException(String method, JsonRpcError error) {
    super("'" + method + "' failed: " + error.message() + " (code " + error.code() + ")");
    this.method = method;
    this.error = error;
  }
}
