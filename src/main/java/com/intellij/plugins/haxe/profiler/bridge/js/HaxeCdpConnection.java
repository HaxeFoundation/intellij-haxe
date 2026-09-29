package com.intellij.plugins.haxe.profiler.bridge.js;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * A minimal Chrome DevTools Protocol client over the JDK's own WebSocket:
 * id-matched requests whose futures complete with the response's
 * {@code result}, and every other message (the events) handed to one
 * listener. A lost connection fails every pending request, so no caller
 * waits for a response that cannot arrive.
 */
final class HaxeCdpConnection implements WebSocket.Listener {

  private static final Logger LOG = Logger.getInstance(HaxeCdpConnection.class);

  private final ObjectMapper mapper = new ObjectMapper();
  private final AtomicInteger nextId = new AtomicInteger(1);
  private final Map<Integer, CompletableFuture<JsonNode>> pending = new ConcurrentHashMap<>();
  /** A message's text parts, collected until its last one arrives. */
  private final StringBuilder messageParts = new StringBuilder();
  private final Consumer<JsonNode> eventListener;
  private volatile boolean closing;
  private WebSocket webSocket;

  private HaxeCdpConnection(@NotNull Consumer<JsonNode> eventListener) {
    this.eventListener = eventListener;
  }

  /** Connects to a DevTools target's websocket url; blocks until the socket is open. */
  @NotNull
  static HaxeCdpConnection open(@NotNull String targetSocketUrl, @NotNull Consumer<JsonNode> eventListener) {
    HaxeCdpConnection connection = new HaxeCdpConnection(eventListener);
    connection.webSocket = HttpClient.newHttpClient()
      .newWebSocketBuilder()
      .buildAsync(URI.create(targetSocketUrl), connection)
      .join();
    return connection;
  }

  /** One CDP request; the future completes with the response's {@code result}. */
  @NotNull
  CompletableFuture<JsonNode> call(@NotNull String method, @Nullable JsonNode params) {
    int id = nextId.getAndIncrement();
    CompletableFuture<JsonNode> response = new CompletableFuture<>();
    pending.put(id, response);
    var message = mapper.createObjectNode().put("id", id).put("method", method);
    if (params != null) message.set("params", params);
    webSocket.sendText(message.toString(), true);
    return response;
  }

  /** The browser is gone: fails the pending requests and keeps the socket's own failure out of the log. */
  void abandon() {
    closing = true;
    failPendingCalls();
  }

  private void failPendingCalls() {
    for (CompletableFuture<JsonNode> response : pending.values()) {
      response.completeExceptionally(new IOException("browser connection lost"));
    }
    pending.clear();
  }

  @Override
  public CompletionStage<?> onText(WebSocket socket, CharSequence part, boolean last) {
    messageParts.append(part);
    if (last) {
      String message = messageParts.toString();
      messageParts.setLength(0);
      deliver(message);
    }
    socket.request(1);
    return null;
  }

  private void deliver(String message) {
    try {
      JsonNode parsed = mapper.readTree(message);
      if (!parsed.hasNonNull("id")) {
        eventListener.accept(parsed);
        return;
      }
      CompletableFuture<JsonNode> response = pending.remove(parsed.get("id").asInt());
      if (response == null) return;
      if (parsed.has("error")) {
        response.completeExceptionally(new IOException("CDP error: " + parsed.get("error")));
      }
      else {
        response.complete(parsed.path("result"));
      }
    }
    catch (JacksonException e) {
      LOG.warn("unreadable CDP message", e);
    }
  }

  @Override
  public CompletionStage<?> onClose(WebSocket socket, int statusCode, String reason) {
    failPendingCalls();
    return null;
  }

  @Override
  public void onError(WebSocket socket, Throwable error) {
    failPendingCalls();
    if (!closing) LOG.warn("js profiler connection failed", error);
  }
}
