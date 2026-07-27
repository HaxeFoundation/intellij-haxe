package com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe.jsonrpc;

/**
 * A message sent by the hxcpp-debug-server: either the {@link JsonRpcResponse}
 * to an outgoing request, or a spontaneous {@link JsonRpcNotification}
 * (stop/thread events). The server never sends requests of its own.
 */
public sealed interface JsonRpcServerMessage permits JsonRpcResponse, JsonRpcNotification {
}
