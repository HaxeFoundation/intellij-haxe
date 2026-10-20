package com.intellij.plugins.haxe.display.transport;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/// The connection to a dedicated haxe display process, kept for the
/// process's lifetime: the socket a `haxe --server-connect host:port` process
/// opened to the IDE, or the pipes of a `haxe --wait stdio` process, which
/// reads its requests on stdin and answers on stderr. Both directions are
/// frames: a 4-byte little-endian length, then the body. A request body is
/// the argument list joined by `\n`; an answer body is what a `--wait` server
/// would have written on its socket, so the same classification applies.
///
/// The server answers one request at a time, strictly in order, so requests
/// are serialised here. An answer that does not arrive in time leaves the
/// stream misaligned: the channel closes itself and stays broken, and its
/// owner starts a new process. A request that finds the channel busy for the
/// whole timeout fails without being sent.
public final class FramedDisplayChannel implements DisplayChannel, Closeable {

  private static final int HEADER_BYTES = 4;

  private final DataInputStream in;
  private final OutputStream out;
  /** Closing it ends the connection: the socket, or the stdio process. */
  private final Closeable connection;
  /** The socket, which bounds a read by its own timeout; null for pipes. */
  private final Socket socket;
  private final String description;
  // fair: a completion or navigation request queued behind a hover batch gets the channel after one hover
  private final ReentrantLock lock = new ReentrantLock(true);
  private volatile boolean broken;

  public FramedDisplayChannel(Socket socket) throws IOException {
    this(socket.getInputStream(), socket.getOutputStream(), socket, socket, "haxe display process on " + socket.getLocalPort());
  }

  private FramedDisplayChannel(InputStream answers, OutputStream requests, Closeable connection, Socket socket, String description) {
    this.in = new DataInputStream(answers);
    this.out = requests;
    this.connection = connection;
    this.socket = socket;
    this.description = description;
  }

  /**
   * A channel over the pipes of a {@code haxe --wait stdio} process: requests
   * go to its stdin, answers come from its stderr. Closing the channel closes
   * {@code process}, which must end the process: a blocked pipe read returns
   * only once the writing side is gone.
   */
  public static FramedDisplayChannel overStdio(InputStream stderr, OutputStream stdin, Closeable process, String description) {
    return new FramedDisplayChannel(stderr, stdin, process, null, description);
  }

  @Override
  public DisplayResponse request(List<String> args, int readTimeoutMs) throws DisplayRequestException {
    if (!acquire(readTimeoutMs)) throw new DisplayRequestException(description + " is busy");
    try {
      if (broken) throw new DisplayRequestException(description + " is closed");
      return exchange(args, readTimeoutMs);
    } catch (IOException e) {
      close();
      String reason = e.getMessage() != null ? e.getMessage() : "the connection ended";
      throw new DisplayRequestException(description + " failed: " + reason, e);
    } finally {
      lock.unlock();
    }
  }

  private DisplayResponse exchange(List<String> args, int readTimeoutMs) throws IOException {
    Runnable answered = boundRead(readTimeoutMs);
    try {
      writeFrame(out, String.join("\n", args).getBytes(StandardCharsets.UTF_8));
      return HaxeDisplayTransport.classify(readFrame(in));
    } finally {
      answered.run();
    }
  }

  /**
   * Bounds the next read: a socket by its timeout; a pipe, which has none, by
   * closing the channel once the answer is overdue. The returned action lifts
   * the bound after the answer arrived.
   */
  private Runnable boundRead(int timeoutMs) throws IOException {
    if (socket != null) {
      socket.setSoTimeout(timeoutMs);
      return () -> { };
    }
    Executor afterTimeout = CompletableFuture.delayedExecutor(timeoutMs, TimeUnit.MILLISECONDS);
    CompletableFuture<Void> overdue = CompletableFuture.runAsync(this::close, afterTimeout);
    return () -> overdue.cancel(false);
  }

  private boolean acquire(int timeoutMs) throws DisplayRequestException {
    try {
      return lock.tryLock(timeoutMs, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new DisplayRequestException(description + ": interrupted while waiting for the channel", e);
    }
  }

  /** Whether a failed exchange closed the channel; a broken channel never recovers. */
  @Override
  public boolean isBroken() {
    return broken;
  }

  @Override
  public void close() {
    broken = true;
    try {
      connection.close();
    } catch (IOException ignored) {
      // closing a connection that already failed has nothing left to report
    }
  }

  static void writeFrame(OutputStream out, byte[] body) throws IOException {
    out.write(ByteBuffer.allocate(HEADER_BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt(body.length).array());
    out.write(body);
    out.flush();
  }

  static byte[] readFrame(DataInputStream in) throws IOException {
    byte[] header = new byte[HEADER_BYTES];
    in.readFully(header);
    int length = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).getInt();
    if (length < 0) throw new IOException("negative frame length " + length);
    byte[] body = new byte[length];
    in.readFully(body);
    return body;
  }
}
