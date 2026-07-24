package com.intellij.plugins.haxe.runner.debugger.dap.client;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import java.io.Closeable;
import java.io.IOException;

/**
 * The client-side surface a DAP-driven debug session consumes: request with
 * response, fire-and-forget request, and the event stream. {@link DapClient}
 * is the plain single-connection implementation; the js-debug session
 * multiplexer implements the same surface over SEVERAL child sessions
 * (page + workers), so the IDE debug process stays a single-session client.
 */
public interface DapEndpoint extends Closeable {

  /** Sends one request and blocks for its response (or times out). */
  Response sendRequest(Request request, long timeoutMillis) throws IOException, InterruptedException;

  /** Sends a request without awaiting its response (deferred-response dialects). */
  void sendRequestNoWait(Request request) throws IOException;

  /** The next adapter event, waiting up to the timeout; null when none arrived. */
  Event pollEvent(long timeoutMillis) throws InterruptedException;
}
