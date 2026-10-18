package com.intellij.plugins.haxe.display.transport;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/// One connection a `haxe --server-connect host:port` process opened to the
/// IDE and keeps for its lifetime. Both directions are frames: a 4-byte
/// little-endian length, then the body. A request body is the argument list
/// joined by `\n`; an answer body is what a `--wait` server would have
/// written on its socket, so the same classification applies.
///
/// The server answers one request at a time, strictly in order, so requests
/// are serialised here. An answer that does not arrive in time leaves the
/// stream misaligned: the channel closes itself and stays broken, and its
/// owner starts a new process. A request that finds the channel busy for the
/// whole timeout fails without being sent.
public final class FramedDisplayChannel implements DisplayChannel, Closeable {

  private static final int HEADER_BYTES = 4;

  private final Socket socket;
  private final DataInputStream in;
  private final OutputStream out;
  private final String description;
  // fair: a completion or navigation request queued behind a hover batch gets the channel after one hover
  private final ReentrantLock lock = new ReentrantLock(true);
  private volatile boolean broken;

  public FramedDisplayChannel(Socket socket) throws IOException {
    this.socket = socket;
    this.in = new DataInputStream(socket.getInputStream());
    this.out = socket.getOutputStream();
    this.description = "haxe display process on " + socket.getLocalPort();
  }

  @Override
  public DisplayResponse request(List<String> args, int readTimeoutMs) throws DisplayRequestException {
    if (!acquire(readTimeoutMs)) throw new DisplayRequestException(description + " is busy");
    try {
      if (broken) throw new DisplayRequestException(description + " is closed");
      socket.setSoTimeout(readTimeoutMs);
      writeFrame(out, String.join("\n", args).getBytes(StandardCharsets.UTF_8));
      return HaxeDisplayTransport.classify(readFrame(in));
    } catch (IOException e) {
      close();
      throw new DisplayRequestException(description + " failed: " + e.getMessage(), e);
    } finally {
      lock.unlock();
    }
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
  public String describe() {
    return description;
  }

  @Override
  public void close() {
    broken = true;
    try {
      socket.close();
    } catch (IOException ignored) {
      // closing a socket that already failed has nothing left to report
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
