package com.intellij.plugins.haxe.profiler.tracy.connect;

import com.intellij.plugins.haxe.profiler.tracy.wire.TracyProtocolVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("tracy receiver: broadcast listener")
public class TracyBroadcastListenerTest {

  @Test
  @DisplayName("parses the client's announcement")
  public void testParsesTheClientsAnnouncement() {
    byte[] message = announcement(3, 41187, 76, 11912, "Main.exe");

    TracyBroadcastListener.Announcement parsed = TracyBroadcastListener.parse(message, 0, message.length);

    assertNotNull(parsed);
    assertEquals(41187, parsed.listenPort());
    assertEquals(76, parsed.protocolVersion());
    assertEquals(11912, parsed.pid());
    assertEquals("Main.exe", parsed.programName());
  }

  @Test
  @DisplayName("other broadcast layouts and short datagrams parse to nothing")
  public void testOtherBroadcastLayoutsAndShortDatagramsParseToNothing() {
    byte[] older = announcement(2, 8086, 69, 1, "x");
    assertNull(TracyBroadcastListener.parse(older, 0, older.length));
    assertNull(TracyBroadcastListener.parse(new byte[10], 0, 10));
  }

  @Test
  @DisplayName("hears the version announced for its data port and ignores other ports")
  public void testHearsTheVersionAnnouncedForItsDataPortAndIgnoresOtherPorts() throws Exception {
    TracyBroadcastListener listener = TracyBroadcastListener.listen(5555);
    assumeTrue(listener != null, "broadcast port not bindable here");

    try (listener; DatagramSocket sender = new DatagramSocket()) {
      send(sender, announcement(3, 6666, 82, 7, "other"));
      send(sender, announcement(3, 5555, 76, 8, "ours"));
      TracyProtocolVersion heard = null;
      for (int i = 0; i < 50 && heard == null; i++) {
        Thread.sleep(20);
        heard = listener.heard();
      }
      assertEquals(TracyProtocolVersion.V76, heard);
    }
  }

  /** BroadcastMessage as the client packs it, trimmed after the program name's terminator. */
  private static byte[] announcement(int broadcastVersion, int listenPort, int protocol, long pid, String name) {
    byte[] nameUtf8 = name.getBytes(StandardCharsets.UTF_8);
    ByteBuffer buffer = ByteBuffer.allocate(20 + nameUtf8.length + 1).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putShort((short)broadcastVersion);
    buffer.putShort((short)listenPort);
    buffer.putInt(protocol);
    buffer.putLong(pid);
    buffer.putInt(12); // activeTime
    buffer.put(nameUtf8);
    buffer.put((byte)0);
    return buffer.array();
  }

  private static void send(DatagramSocket sender, byte[] message) throws IOException {
    InetAddress loopback = InetAddress.getLoopbackAddress();
    sender.send(new DatagramPacket(message, message.length, loopback, TracyBroadcastListener.BROADCAST_PORT));
  }
}
