package com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe.jsonrpc;

import tools.jackson.databind.JsonNode;

/**
 * A spontaneous server event: {@code {method, params}} with no id
 * (breakpointStop, exceptionStop, pauseStop, threadStart, ThreadExit).
 */
public record JsonRpcNotification(String method, JsonNode params) implements JsonRpcServerMessage {
}
