package com.intellij.plugins.haxe.profiler.tracy.connect;

import com.intellij.plugins.haxe.profiler.tracy.wire.TracyProtocolVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Hears the client announce itself. Unless built with TRACY_NO_BROADCAST
 * (hxcpp is not), a tracy client sends a BroadcastMessage about once a
 * second on UDP port 8086 - the port is compile-time, TRACY_PORT moves only
 * the data port - naming its protocol version, data port and pid. The
 * message for the capture's data port settles the version to offer before
 * the first handshake; a missed or blocked broadcast just leaves the probe
 * ladder to find it. The socket binds with address reuse so a Tracy GUI on the same
 * machine (it listens on 8086 too) does not block the bind.
 */
public final class TracyBroadcastListener implements AutoCloseable {

  public static final int BROADCAST_PORT = 8086;
  /** The broadcast layout this listener parses (BroadcastMessage since Tracy 0.9). */
  static final int BROADCAST_VERSION = 3;
  private static final int HEADER_SIZE = 2 + 2 + 4 + 8 + 4;
  private static final int MAX_MESSAGE_SIZE = HEADER_SIZE + 64;

  /** One client's announcement: its data port, protocol version and process. */
  public record Announcement(int listenPort, int protocolVersion, long pid, @NotNull String programName) {
  }

  private final DatagramSocket socket;
  private final int dataPort;
  private final AtomicReference<TracyProtocolVersion> heard = new AtomicReference<>();
  private final Thread thread;

  private TracyBroadcastListener(DatagramSocket socket, int dataPort) {
    this.socket = socket;
    this.dataPort = dataPort;
    thread = new Thread(this::receive, "haxe-tracy-broadcast");
    thread.setDaemon(true);
  }

  /**
   * Starts listening for the client that will serve {@code dataPort}; null
   * when the broadcast port cannot be bound (the capture then relies on the
   * probe ladder alone).
   */
  @Nullable
  public static TracyBroadcastListener listen(int dataPort) {
    try {
      DatagramSocket socket = new DatagramSocket(null);
      socket.setReuseAddress(true);
      socket.bind(new InetSocketAddress(BROADCAST_PORT));
      TracyBroadcastListener listener = new TracyBroadcastListener(socket, dataPort);
      listener.thread.start();
      return listener;
    }
    catch (IOException cannotBind) {
      return null;
    }
  }

  /** The version the client announced for the capture's data port; null until heard. */
  @Nullable
  public TracyProtocolVersion heard() {
    return heard.get();
  }

  private void receive() {
    byte[] buffer = new byte[MAX_MESSAGE_SIZE];
    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
    while (!socket.isClosed()) {
      try {
        socket.receive(packet);
      }
      catch (IOException closed) {
        return;
      }
      Announcement announcement = parse(packet.getData(), packet.getOffset(), packet.getLength());
      if (announcement == null || announcement.listenPort() != dataPort) continue;
      TracyProtocolVersion version = TracyProtocolVersion.of(announcement.protocolVersion());
      if (version != null) heard.set(version);
    }
  }

  /**
   * BroadcastMessage, packed little-endian: u16 broadcastVersion, u16
   * listenPort, u32 protocolVersion, u64 pid, i32 activeTime, then the
   * zero-terminated program name (the sender trims the message after it).
   * Null for other broadcast layouts or a short datagram.
   */
  @Nullable
  static Announcement parse(byte @NotNull [] data, int offset, int length) {
    if (length < HEADER_SIZE) return null;
    ByteBuffer buffer = ByteBuffer.wrap(data, offset, length).order(ByteOrder.LITTLE_ENDIAN);
    int broadcastVersion = buffer.getShort() & 0xFFFF;
    if (broadcastVersion != BROADCAST_VERSION) return null;
    int listenPort = buffer.getShort() & 0xFFFF;
    int protocolVersion = buffer.getInt();
    long pid = buffer.getLong();
    buffer.getInt(); // activeTime - seconds since the client started, unused
    int nameStart = buffer.position();
    int nameEnd = nameStart;
    while (nameEnd < offset + length && data[nameEnd] != 0) nameEnd++;
    String programName = new String(data, nameStart, nameEnd - nameStart, StandardCharsets.UTF_8);
    return new Announcement(listenPort, protocolVersion, pid, programName);
  }

  @Override
  public void close() {
    socket.close();
    try {
      thread.join(1_000);
    }
    catch (InterruptedException interrupted) {
      Thread.currentThread().interrupt();
    }
  }
}
