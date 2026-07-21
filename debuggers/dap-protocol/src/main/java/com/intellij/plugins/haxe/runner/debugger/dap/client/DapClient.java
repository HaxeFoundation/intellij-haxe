package com.intellij.plugins.haxe.runner.debugger.dap.client;

import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Event;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.ProtocolMessage;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Request;
import com.intellij.plugins.haxe.runner.debugger.dap.protocol.Response;
import com.intellij.plugins.haxe.runner.debugger.dap.transport.DapConnection;
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A synchronous-with-timeout DAP client on top of {@link DapConnection}.
 *
 * A background reader thread demultiplexes incoming messages: responses are
 * matched to their request by {@code request_seq}, events go to a queue that
 * callers drain with {@link #pollEvent}. Owns the client-side seq counter.
 */
public class DapClient implements Closeable {
  /** Poison pill offered to every pending request when the reader exits. */
  private static final Response CONNECTION_CLOSED = new Response();

  private final DapConnection connection;
  private final Thread readerThread;
  private final AtomicInteger nextSeq = new AtomicInteger(1);
  private final ConcurrentMap<Integer, BlockingQueue<Response>> pendingResponses = new ConcurrentHashMap<>();
  private final BlockingQueue<Event> events = new LinkedBlockingQueue<>();
  private volatile boolean closed = false;
  private volatile boolean readerFinished = false;
  private volatile Throwable readerDeathCause;

  public static DapClient connect(String host, int port, int connectTimeoutMillis) throws IOException {
    return new DapClient(DapConnection.connect(host, port, connectTimeoutMillis));
  }

  public DapClient(DapConnection connection) {
    this.connection = connection;
    readerThread = new Thread(this::readLoop, "dap-client-reader");
    readerThread.setDaemon(true);
    readerThread.start();
  }

  /**
   * Assigns the next seq to the request, sends it, and blocks until the
   * matching response arrives or the timeout elapses.
   */
  public Response sendRequest(Request request, long timeoutMillis) throws IOException, InterruptedException {
    int seq = nextSeq.getAndIncrement();
    request.setSeq(seq);
    BlockingQueue<Response> pending = new ArrayBlockingQueue<>(1);
    pendingResponses.put(seq, pending);
    try {
      // registered BEFORE this check: a reader exiting in between either
      // trips the flag here or poisons our queue (its sweep runs after the
      // flag is set, so it sees the entry) — no window where we'd wait out
      // the full timeout against a connection that can never answer
      if (readerFinished) {
        throw connectionClosed(request);
      }
      connection.send(request);
      Response response = pending.poll(timeoutMillis, TimeUnit.MILLISECONDS);
      if (response == null) {
        throw new IOException("Timed out waiting for response to '" + request.getCommand() + "' (seq " + seq + ")");
      }
      if (response == CONNECTION_CLOSED) {
        throw connectionClosed(request);
      }
      return response;
    } finally {
      pendingResponses.remove(seq);
    }
  }

  private IOException connectionClosed(Request request) {
    Throwable cause = readerDeathCause;
    String how = closed ? "closed by this client"
                        : cause != null ? "lost (" + cause + ")"
                                        : "closed by the peer";
    return new IOException("The DAP connection was " + how
                           + " before '" + request.getCommand() + "' got its response", cause);
  }

  /**
   * True once the reader saw EOF or died — the connection carries no further
   * messages. Lets an event pump distinguish "no event yet" from "there will
   * never be another".
   */
  public boolean isConnectionFinished() {
    return readerFinished;
  }

  /** Returns the next event, waiting up to the timeout; null when none arrived. */
  public Event pollEvent(long timeoutMillis) throws InterruptedException {
    return events.poll(timeoutMillis, TimeUnit.MILLISECONDS);
  }

  private void readLoop() {
    try {
      while (true) {
        ProtocolMessage message = connection.receive();
        if (message == null) {
          return;
        }
        if (message instanceof Response response) {
          BlockingQueue<Response> pending = pendingResponses.get(response.getRequest_seq());
          if (pending != null) {
            pending.offer(response);
          }
        }
        else if (message instanceof Event event) {
          events.offer(event);
        }
      }
    } catch (IOException | RuntimeException e) {
      // A framing/decode failure must not silently kill the demultiplexer —
      // after this thread dies every later request times out with no hint why.
      // Only a deliberate close() is an expected way for the read to end.
      readerDeathCause = e;
      if (!closed) {
        System.err.println("DapClient reader died: " + e);
        e.printStackTrace();
      }
    } finally {
      // fail-fast for waiters: the flag is set FIRST, then every pending
      // request is poisoned (see the ordering note in sendRequest)
      readerFinished = true;
      for (BlockingQueue<Response> pending : pendingResponses.values()) {
        pending.offer(CONNECTION_CLOSED);
      }
    }
  }

  @Override
  public void close() throws IOException {
    closed = true;
    connection.close();
    try {
      readerThread.join(1000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
