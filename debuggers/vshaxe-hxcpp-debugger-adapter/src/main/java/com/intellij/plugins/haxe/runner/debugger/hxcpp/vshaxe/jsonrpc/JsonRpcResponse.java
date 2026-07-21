package com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe.jsonrpc;

import tools.jackson.databind.JsonNode;

/**
 * The server's reply to one request: {@code {id, result}} on success or
 * {@code {id, error:{code,message}}} on failure. Exactly one of result/error
 * is meaningful; result stays a raw tree because its shape depends on the
 * request method (the translation layer decodes it per call site).
 */
public record JsonRpcResponse(int id, JsonNode result, JsonRpcError error) implements JsonRpcServerMessage {
  public boolean isError() {
    return error != null;
  }
}
