package com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe.jsonrpc;

/**
 * A client-to-server request: {@code {id, method, params}}. The id is
 * assigned by {@link JsonRpcClient}; params may be any Jackson-serializable
 * value (a null params encodes as {@code {}} — the server's handlers always
 * expect a params object).
 */
public record JsonRpcRequest(int id, String method, Object params) {
}
