package com.intellij.plugins.haxe.profiler.tracy.connect;

import com.intellij.plugins.haxe.profiler.tracy.TracyEventReader;
import com.intellij.plugins.haxe.profiler.tracy.TracySession;
import com.intellij.plugins.haxe.profiler.tracy.wire.TracyLz4Stream;
import com.intellij.plugins.haxe.profiler.tracy.wire.TracyProtocolVersion;
import com.intellij.plugins.haxe.profiler.tracy.wire.TracyWelcome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/**
 * One live tracy capture: connects OUT to the client (it listens on the
 * port the launcher assigned via TRACY_PORT), settles the protocol version
 * through the handshake - offering the strategy's candidates one connect
 * at a time, since the client answers a wrong version with a refusal, closes
 * and listens again - asks plot/thread names over the query channel as they
 * first appear, and reads until the client is drained. Shutdown is a
 * two-step handshake and the client NEVER closes the socket itself: it
 * announces exit with a Terminate item, and on receiving the receiver's
 * Disconnect query it flushes what remains, sends a FINAL Terminate and then
 * waits for the server to close. So a Terminate with no disconnect requested
 * yet triggers {@link #requestDisconnect()}, and one arriving after it means
 * drained: the read stops, and closing the receiver's socket is what lets
 * the client's process exit. A capture can also end by the stream TEARING (the
 * process killed, or dead without the handshake): everything received
 * before the tear is kept. Queries are a handful of name lookups, far below
 * the client's query budget, so no flow-control bookkeeping is needed.
 */
public final class TracyLiveCapture implements AutoCloseable {

  // ServerQuery wire values, the same in every supported protocol version
  private static final int QUERY_STRING = 1;
  private static final int QUERY_THREAD_STRING = 2;
  private static final int QUERY_PLOT_NAME = 4;
  private static final int QUERY_DISCONNECT = 9;

  private static final int CONNECT_RETRY_MS = 100;
  /** A client that accepted the socket answers the handshake within its own 2 s read window; longer means it hung. */
  private static final int HANDSHAKE_TIMEOUT_MS = 5_000;
  /** How long a post-disconnect stream may stay silent before the capture ends with what arrived. */
  private static final int DRAIN_QUIET_TIMEOUT_MS = 5_000;

  private final Socket socket;
  private final OutputStream queries;
  private final TracyWelcome welcome;
  private final AtomicBoolean disconnectSent = new AtomicBoolean();

  private TracyLiveCapture(Socket socket, TracyWelcome welcome) throws IOException {
    this.socket = socket;
    this.queries = socket.getOutputStream();
    this.welcome = welcome;
  }

  /**
   * Connects with retries while {@code keepTrying} allows - the client's
   * listener comes up somewhere inside the process's startup - and completes
   * the handshake with the first version the client accepts. Null when the
   * window closed without a connection (the process died first); throws
   * {@link TracyProtocolUnsupportedException} once the client refused every
   * version the strategy had.
   */
  @Nullable
  public static TracyLiveCapture connect(int port, @NotNull TracyVersionStrategy strategy,
                                         @NotNull BooleanSupplier keepTrying) throws IOException {
    List<TracyProtocolVersion> refused = new ArrayList<>();
    while (keepTrying.getAsBoolean()) {
      TracyProtocolVersion candidate = strategy.next(refused);
      if (candidate == null) throw new TracyProtocolUnsupportedException(refused);
      Socket socket = open(port);
      if (socket == null) {
        if (!pause()) return null;
        continue;
      }
      try {
        socket.setSoTimeout(HANDSHAKE_TIMEOUT_MS);
        TracyWelcome welcome = TracyHandshake.perform(socket.getInputStream(), socket.getOutputStream(), candidate);
        socket.setSoTimeout(0);
        return new TracyLiveCapture(socket, welcome);
      }
      catch (TracyProtocolMismatchException mismatch) {
        refused.add(candidate);
        closeQuietly(socket);
      }
      catch (IOException handshakeFailed) {
        // "not available" (another server attached) or a dropped socket: the same offer is retried while the window is open
        closeQuietly(socket);
        if (!pause()) return null;
      }
    }
    return null;
  }

  /** The client's welcome, protocol version included. */
  @NotNull
  public TracyWelcome welcome() {
    return welcome;
  }

  /**
   * Reads the whole session over the handshaken connection; returns when
   * the client's stream ends. Zones go to the sink as they close and stay
   * out of the returned session, so a minutes-long capture spools to disk
   * instead of filling the heap.
   */
  @NotNull
  public TracySession capture(TracyEventReader.@Nullable ZoneSink zoneSink) throws IOException {
    try (socket) {
      TracyEventReader.Hooks hooks = new TracyEventReader.Hooks() {
        @Override
        public void plotSeen(long namePointer) {
          query(QUERY_PLOT_NAME, namePointer);
        }

        @Override
        public void threadSeen(int threadId) {
          query(QUERY_THREAD_STRING, threadId);
        }

        @Override
        public void memPoolSeen(long namePointer) {
          query(QUERY_STRING, namePointer);
        }

        @Override
        public boolean terminateSeen() {
          boolean drained = disconnectSent.get();
          requestDisconnect();
          return drained;
        }
      };
      TracyLz4Stream decompressed = new TracyLz4Stream(socket.getInputStream());
      // salvaging: a killed client tears the stream mid-item - the capture
      // keeps everything received up to that point (tracy's client exit
      // wedges before its shutdown handshake when system tracing is
      // active, so killing the process is a NORMAL way to end a capture)
      return TracyEventReader.readSalvaging(decompressed, welcome, hooks, zoneSink);
    }
  }

  /**
   * Acknowledges shutdown so the client flushes and closes; safe from any
   * thread and called at most once - later calls and send failures are
   * no-ops (the stream end is what actually finishes the capture).
   */
  public void requestDisconnect() {
    if (!disconnectSent.compareAndSet(false, true)) return;
    query(QUERY_DISCONNECT, 0);
    try {
      // the client flushes and then WAITS for this side to close. An
      // exit-path shutdown (the app quit on its own; TRACY_NO_EXIT keeps the
      // process alive for the drain) does not repeat its Terminate after the
      // flush, so a quiet stream must end the capture instead of waiting for
      // a marker that never comes. Closing the socket is also what finally
      // lets that lingering process exit.
      socket.setSoTimeout(DRAIN_QUIET_TIMEOUT_MS);
    }
    catch (SocketException gone) {
      // a dead socket surfaces as the reader's stream end regardless
    }
  }

  /**
   * Abandons a handshaken connection the caller could not start reading
   * (the session file failed to open): the client streams into a closed
   * socket and moves on, instead of waiting forever for a server that never
   * drains it. {@link #capture} closes the socket itself.
   */
  @Override
  public void close() {
    closeQuietly(socket);
  }

  /** ServerQueryPacket: u8 type, u64 ptr, u32 extra (always 0 here), little-endian. */
  private void query(int type, long pointer) {
    ByteBuffer packet = ByteBuffer.allocate(1 + 8 + 4)
      .order(ByteOrder.LITTLE_ENDIAN)
      .put((byte)type)
      .putLong(pointer);
    try {
      synchronized (queries) {
        queries.write(packet.array());
        queries.flush();
      }
    }
    catch (IOException gone) {
      // a torn connection surfaces as the reader's stream end; queries are best-effort
    }
  }

  /** A connected socket, or null while the client's listener is not up yet. */
  @Nullable
  private static Socket open(int port) {
    Socket socket = new Socket();
    try {
      socket.connect(new InetSocketAddress("127.0.0.1", port), 500);
      return socket;
    }
    catch (IOException notListeningYet) {
      closeQuietly(socket);
      return null;
    }
  }

  /** One retry pause; false when interrupted (the capture is being abandoned). */
  private static boolean pause() {
    try {
      Thread.sleep(CONNECT_RETRY_MS);
      return true;
    }
    catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  private static void closeQuietly(@NotNull Socket socket) {
    try {
      socket.close();
    }
    catch (IOException ignored) {
    }
  }
}
