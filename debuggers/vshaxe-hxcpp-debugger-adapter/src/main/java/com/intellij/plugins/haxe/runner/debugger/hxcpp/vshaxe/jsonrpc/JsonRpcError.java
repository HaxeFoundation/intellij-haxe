package com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe.jsonrpc;

/**
 * Error body of a failed request. Codes used by the server:
 * 500 (internal), 422 (wrong request) — see ErrorCode in Protocol.hx.
 */
public record JsonRpcError(int code, String message) {
}
