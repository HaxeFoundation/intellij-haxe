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
  private final DapConnection connection;
  private final Thread readerThread;
  private final AtomicInteger nextSeq = new AtomicInteger(1);
  private final ConcurrentMap<Integer, BlockingQueue<Response>> pendingResponses = new ConcurrentHashMap<>();
  private final BlockingQueue<Event> events = new LinkedBlockingQueue<>();

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
      connection.send(request);
      Response response = pending.poll(timeoutMillis, TimeUnit.MILLISECONDS);
      if (response == null) {
        throw new IOException("Timed out waiting for response to '" + request.getCommand() + "' (seq " + seq + ")");
      }
      return response;
    } finally {
      pendingResponses.remove(seq);
    }
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
      // connection is gone; outstanding sendRequest calls will time out
    }
  }

  @Override
  public void close() throws IOException {
    connection.close();
    try {
      readerThread.join(1000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
