package com.intellij.plugins.haxe.runner.debugger.hxcpp.vshaxe.jsonrpc;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import tools.jackson.databind.JsonNode;

/**
 * A synchronous-with-timeout jsonrpc client on top of {@link JsonRpcConnection}.
 *
 * A background reader thread demultiplexes incoming messages: responses are
 * matched to their request by id, notifications go to a queue that callers
 * drain with {@link #pollNotification}. Owns the client-side id counter.
 */
public class JsonRpcClient implements Closeable {
  /** Poison pill offered to every pending request when the reader exits. */
  private static final JsonRpcResponse CONNECTION_CLOSED = new JsonRpcResponse(-1, null, null);

  private final JsonRpcConnection connection;
  private final Thread readerThread;
  private final AtomicInteger nextId = new AtomicInteger(1);
  private final ConcurrentMap<Integer, BlockingQueue<JsonRpcResponse>> pendingResponses = new ConcurrentHashMap<>();
  private final BlockingQueue<JsonRpcNotification> notifications = new LinkedBlockingQueue<>();
  private volatile boolean closed = false;
  private volatile boolean readerFinished = false;
  private volatile Throwable readerDeathCause;

  public JsonRpcClient(JsonRpcConnection connection) {
    this.connection = connection;
    readerThread = new Thread(this::readLoop, "hxcpp-jsonrpc-reader");
    readerThread.setDaemon(true);
    readerThread.start();
  }

  /**
   * Assigns the next id to a request for {@code method}, sends it, and blocks
   * until the matching response arrives or the timeout elapses.
   */
  public JsonRpcResponse sendRequest(String method, Object params, long timeoutMillis)
    throws IOException, InterruptedException {
    int id = nextId.getAndIncrement();
    BlockingQueue<JsonRpcResponse> pending = new ArrayBlockingQueue<>(1);
    pendingResponses.put(id, pending);
    try {
      // registered BEFORE this check: a reader exiting in between either
      // trips the flag here or poisons our queue (its sweep runs after the
      // flag is set, so it sees the entry) — no window where we'd wait out
      // the full timeout against a connection that can never answer
      if (readerFinished) {
        throw connectionClosed(method);
      }
      connection.send(new JsonRpcRequest(id, method, params));
      JsonRpcResponse response = pending.poll(timeoutMillis, TimeUnit.MILLISECONDS);
      if (response == null) {
        throw new IOException("Timed out waiting for response to '" + method + "' (id " + id + ")");
      }
      if (response == CONNECTION_CLOSED) {
        throw connectionClosed(method);
      }
      return response;
    } finally {
      pendingResponses.remove(id);
    }
  }

  private IOException connectionClosed(String method) {
    Throwable cause = readerDeathCause;
    String how = closed ? "closed by this client"
                        : cause != null ? "lost (" + cause + ")"
                                        : "closed by the debuggee";
    return new IOException("The debugger connection was " + how
                           + " before '" + method + "' got its response", cause);
  }

  /**
   * Like {@link #sendRequest} but unwraps the response: returns the result
   * tree on success, throws {@link JsonRpcErrorException} on a server error.
   */
  public JsonNode call(String method, Object params, long timeoutMillis)
    throws IOException, InterruptedException {
    JsonRpcResponse response = sendRequest(method, params, timeoutMillis);
    if (response.isError()) {
      throw new JsonRpcErrorException(method, response.error());
    }
    return response.result();
  }

  /** Returns the next notification, waiting up to the timeout; null when none arrived. */
  public JsonRpcNotification pollNotification(long timeoutMillis) throws InterruptedException {
    return notifications.poll(timeoutMillis, TimeUnit.MILLISECONDS);
  }

  /**
   * True once the reader saw EOF or died — the connection carries no further
   * messages (the debuggee exited or the socket broke). Lets an event pump
   * distinguish "no notification yet" from "there will never be another".
   */
  public boolean isConnectionFinished() {
    return readerFinished;
  }

  private void readLoop() {
    try {
      while (true) {
        JsonRpcServerMessage message = connection.receive();
        if (message == null) {
          return;
        }
        // exhaustive: JsonRpcServerMessage is sealed
        switch (message) {
          case JsonRpcResponse response -> {
            BlockingQueue<JsonRpcResponse> pending = pendingResponses.get(response.id());
            if (pending != null) {
              pending.offer(response);
            }
          }
          case JsonRpcNotification notification -> notifications.offer(notification);
        }
      }
    } catch (IOException | RuntimeException e) {
      // A framing/decode failure must not silently kill the demultiplexer —
      // after this thread dies every later request times out with no hint why.
      // Only a deliberate close() is an expected way for the read to end.
      readerDeathCause = e;
      if (!closed) {
        System.err.println("JsonRpcClient reader died: " + e);
        e.printStackTrace();
      }
    } finally {
      // fail-fast for waiters: the flag is set FIRST, then every pending
      // request is poisoned (see the ordering note in sendRequest)
      readerFinished = true;
      for (BlockingQueue<JsonRpcResponse> pending : pendingResponses.values()) {
        pending.offer(CONNECTION_CLOSED);
      }
    }
  }

  @Override
  public void close() throws IOException {
    closed = true;
    connection.close();
  }
}
