package com.intellij.plugins.haxe.display.transport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/// A stand-in for a `haxe --wait` server that answers one canned reply per
/// exchange: it reads the request up to its null terminator, writes the reply
/// and closes the connection.
public final class FakeDisplayServer implements AutoCloseable {

  public static final char LOG_MARK = 1;
  public static final char ERROR_MARK = 2;

  private final ServerSocket socket;
  private final ExecutorService executor = Executors.newSingleThreadExecutor();

  public FakeDisplayServer() throws IOException {
    socket = new ServerSocket(0);
  }

  public int port() {
    return socket.getLocalPort();
  }

  /** Serves the next connection; the future yields the request bytes without the terminator. */
  public Future<byte[]> replyOnce(byte[] reply) {
    return executor.submit(() -> {
      try (Socket connection = socket.accept()) {
        byte[] request = readRequest(connection.getInputStream());
        OutputStream out = connection.getOutputStream();
        out.write(reply);
        out.flush();
        return request;
      }
    });
  }

  private static byte[] readRequest(InputStream in) throws IOException {
    ByteArrayOutputStream request = new ByteArrayOutputStream();
    int b;
    while ((b = in.read()) > 0) {
      request.write(b);
    }
    return request.toByteArray();
  }

  @Override
  public void close() throws IOException {
    socket.close();
    executor.shutdownNow();
  }
}
