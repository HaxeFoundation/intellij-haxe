package com.intellij.plugins.haxe.profiler.tracy.connect;

import com.intellij.plugins.haxe.profiler.tracy.wire.TracyProtocolVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A fake client that behaves like tracy's accept loop: it reads the
 * shibboleth and version, refuses (status 2, close, listen again) until the
 * version it speaks is offered, then answers with the welcome fixture.
 */
@DisplayName("tracy receiver: live capture")
@Timeout(20)
public class TracyLiveCaptureTest {

  @Test
  @DisplayName("offers the ladder until the client accepts and keeps the settled version")
  public void testOffersTheLadderUntilTheClientAcceptsAndKeepsTheSettledVersion() throws Exception {
    try (FakeClient client = new FakeClient(TracyProtocolVersion.V82);
         TracyLiveCapture capture = TracyLiveCapture.connect(client.port(), probeLadder(), () -> true)) {

      assertNotNull(capture);
      assertEquals(TracyProtocolVersion.V82, capture.welcome().protocolVersion());
      assertEquals("ProfPump-debug.exe", capture.welcome().programName());
      assertEquals(List.of(76, 74, 82), client.offered(), "the ladder order, stopping at the accepted one");
    }
  }

  @Test
  @DisplayName("an announced version is offered first")
  public void testAnAnnouncedVersionIsOfferedFirst() throws Exception {
    TracyVersionStrategy strategy = TracyVersionStrategy.detect(() -> TracyProtocolVersion.V69);
    try (FakeClient client = new FakeClient(TracyProtocolVersion.V69);
         TracyLiveCapture capture = TracyLiveCapture.connect(client.port(), strategy, () -> true)) {

      assertNotNull(capture);
      assertEquals(List.of(69), client.offered());
    }
  }

  @Test
  @DisplayName("a client refusing every version fails naming what was offered")
  public void testAClientRefusingEveryVersionFailsNamingWhatWasOffered() throws Exception {
    try (FakeClient client = new FakeClient(null)) {
      TracyProtocolUnsupportedException failure = assertThrows(TracyProtocolUnsupportedException.class,
        () -> TracyLiveCapture.connect(client.port(), probeLadder(), () -> true));

      assertEquals(TracyProtocolVersion.PROBE_ORDER, failure.refused());
      assertEquals(List.of(76, 74, 82, 69), client.offered());
    }
  }

  @Test
  @DisplayName("a pinned version is offered once and its refusal ends the capture")
  public void testAPinnedVersionIsOfferedOnceAndItsRefusalEndsTheCapture() throws Exception {
    try (FakeClient client = new FakeClient(TracyProtocolVersion.V76)) {
      TracyVersionStrategy strategy = TracyVersionStrategy.pinned(TracyProtocolVersion.V74);

      assertThrows(TracyProtocolUnsupportedException.class,
                   () -> TracyLiveCapture.connect(client.port(), strategy, () -> true));
      assertEquals(List.of(74), client.offered());
    }
  }

  /** Detection with nothing announced: the plain probe ladder. */
  private static TracyVersionStrategy probeLadder() {
    return TracyVersionStrategy.detect(() -> null);
  }

  private static final class FakeClient implements AutoCloseable {
    private final ServerSocket listener;
    private final TracyProtocolVersion speaks;
    private final List<Integer> offered = new CopyOnWriteArrayList<>();
    private final Thread thread;
    private final List<Socket> accepted = new ArrayList<>();

    /** {@code speaks} null = refuses everything. */
    FakeClient(TracyProtocolVersion speaks) throws IOException {
      this.speaks = speaks;
      listener = new ServerSocket(0);
      thread = new Thread(this::serve, "fake-tracy-client");
      thread.setDaemon(true);
      thread.start();
    }

    int port() {
      return listener.getLocalPort();
    }

    List<Integer> offered() {
      return offered;
    }

    private void serve() {
      while (!listener.isClosed()) {
        try {
          Socket socket = listener.accept();
          synchronized (accepted) {
            accepted.add(socket);
          }
          handshake(socket);
        }
        catch (IOException closed) {
          return;
        }
      }
    }

    private void handshake(Socket socket) throws IOException {
      InputStream in = socket.getInputStream();
      OutputStream out = socket.getOutputStream();
      byte[] shibbolethAndVersion = in.readNBytes(12);
      int version = ByteBuffer.wrap(shibbolethAndVersion, 8, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
      offered.add(version);
      if (speaks == null || version != speaks.wire()) {
        out.write(2); // HandshakeProtocolMismatch
        out.flush();
        socket.close();
        return;
      }
      out.write(1); // HandshakeWelcome
      out.write(welcomeFor(speaks));
      out.flush();
      // the connection stays open: the receiver owns it from here
    }

    /** The v74 fixture reshaped to the version's layout (v76+ has no delay field). */
    private static byte[] welcomeFor(TracyProtocolVersion version) throws IOException {
      byte[] v74;
      try (InputStream in = TracyLiveCaptureTest.class.getResourceAsStream("/tracy/welcome-v74.bin")) {
        v74 = in.readAllBytes();
      }
      if (version.format().welcomeSize() == v74.length) return v74;
      byte[] shorter = new byte[v74.length - 8];
      System.arraycopy(v74, 0, shorter, 0, 24);
      System.arraycopy(v74, 32, shorter, 24, v74.length - 32);
      return shorter;
    }

    @Override
    public void close() throws IOException {
      listener.close();
      synchronized (accepted) {
        for (Socket socket : accepted) socket.close();
      }
    }
  }
}
