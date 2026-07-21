package com.intellij.plugins.haxe.runner.debugger.eval;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * JSON-RPC 2.0 connection to the eval VM's debug socket: correlates request
 * ids to responses, and routes id-less messages (breakpointStop /
 * exceptionStop / threadEvent) to the event listener. The VM is strict about
 * the envelope — a request without {@code "jsonrpc":"2.0"} is rejected with a
 * parse error, so the envelope is built here and nowhere else.
 *
 * <p>Stream-based (not socket-based) so unit tests can drive it with
 * in-memory pipes. One reader thread owns the input stream for the
 * connection's lifetime; writes are serialized on the output stream.
 */
public final class EvalConnection implements AutoCloseable {
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final InputStream in;
  private final OutputStream out;
  private final AtomicInteger nextId = new AtomicInteger(1);
  private final Map<Integer, CompletableFuture<JsonNode>> pending = new ConcurrentHashMap<>();
  // volatile: set once by the owner before/around start, read by the reader thread
  private volatile BiConsumer<String, JsonNode> eventListener = (method, params) -> { };
  private volatile Runnable onDisconnected = () -> { };
  private volatile boolean closed;
  private Thread reader;

  public EvalConnection(InputStream in, OutputStream out) {
    this.in = in;
    this.out = out;
  }

  /** Registers the sink for id-less notifications (method name, params). */
  public void setEventListener(BiConsumer<String, JsonNode> listener) {
    this.eventListener = listener;
  }

  /**
   * Registers a callback for the VM ending the connection (its process — the
   * interpreted program or the compilation being macro-debugged — finished).
   * Not invoked when WE close the connection.
   */
  public void setOnDisconnected(Runnable listener) {
    this.onDisconnected = listener;
  }

  /** Starts the reader thread; call once after the listener is registered. */
  public void start() {
    reader = new Thread(this::readLoop, "eval-debug-reader");
    reader.setDaemon(true);
    reader.start();
  }

  /**
   * Sends one request and blocks for its response, returning the
   * {@code result} node. A protocol-level error response surfaces as
   * {@link EvalProtocolException}; transport death or timeout as IOException.
   */
  public JsonNode request(String method, JsonNode params, long timeoutMs) throws IOException {
    int id = nextId.getAndIncrement();
    CompletableFuture<JsonNode> future = new CompletableFuture<>();
    pending.put(id, future);
    ObjectNode envelope = MAPPER.createObjectNode();
    envelope.put("jsonrpc", "2.0");
    envelope.put("id", id);
    envelope.put("method", method);
    envelope.set("params", params == null ? MAPPER.createObjectNode() : params);
    byte[] frame = EvalFraming.encodeRequest(MAPPER.writeValueAsString(envelope));
    try {
      synchronized (out) {
        out.write(frame);
        out.flush();
      }
      return future.get(timeoutMs, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while awaiting eval response to '" + method + "'", e);
    } catch (ExecutionException e) {
      if (e.getCause() instanceof EvalProtocolException protocol) {
        throw protocol;
      }
      if (e.getCause() instanceof EvalConnectionClosedException closedEnd) {
        throw closedEnd; // resume-shaped callers treat this as the program ending
      }
      throw new IOException("Eval request '" + method + "' failed", e.getCause());
    } catch (TimeoutException e) {
      throw new IOException("Eval request '" + method + "' got no response within " + timeoutMs + "ms");
    } finally {
      pending.remove(id);
    }
  }

  private void readLoop() {
    try {
      String payload;
      while ((payload = EvalFraming.readResponse(in)) != null) {
        dispatch(MAPPER.readTree(payload));
      }
      failPending(new EvalConnectionClosedException("Eval debug connection closed by the VM"));
      if (!closed) {
        onDisconnected.run();
      }
    } catch (Exception e) {
      if (!closed) {
        failPending(new EvalConnectionClosedException("Eval debug connection lost: " + e));
        onDisconnected.run();
      } else {
        failPending(new IOException("Eval debug connection closed"));
      }
    }
  }

  private void dispatch(JsonNode message) {
    JsonNode id = message.get("id");
    if (id == null || id.isNull()) {
      // an id-less message is either a notification or the VM's complaint
      // about a request it could not even parse (error with id:null) — the
      // latter cannot be correlated, so surface it through the event sink
      JsonNode error = message.get("error");
      if (error != null) {
        eventListener.accept("protocolError", error);
      } else {
        eventListener.accept(message.path("method").asString(""), message.get("params"));
      }
      return;
    }
    CompletableFuture<JsonNode> future = pending.remove(id.asInt());
    if (future == null) {
      return; // response to a request that already timed out
    }
    JsonNode error = message.get("error");
    if (error != null) {
      future.completeExceptionally(new EvalProtocolException(
        error.path("code").asInt(0), error.path("message").asString("(no message)")));
    } else {
      future.complete(message.get("result"));
    }
  }

  private void failPending(IOException reason) {
    for (CompletableFuture<JsonNode> future : pending.values()) {
      future.completeExceptionally(reason);
    }
    pending.clear();
  }

  @Override
  public void close() {
    closed = true;
    try {
      in.close();
    } catch (IOException ignored) {
    }
    try {
      out.close();
    } catch (IOException ignored) {
    }
    if (reader != null) {
      try {
        reader.join(2000);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
