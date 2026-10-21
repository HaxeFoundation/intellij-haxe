package com.intellij.plugins.haxe.profiler.tracy.wire;

import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.IOException;

import static com.intellij.plugins.haxe.profiler.io.LittleEndian.*;

/**
 * Protocol 82's encoding (Tracy 0.14): the client packs static zone begins,
 * zone ends and sampled callstacks whose delta is non-negative into 16-bit
 * items ({@code delta < 2^16}) or 32-bit items ({@code delta - 2^16}), and
 * stores the 64-bit fallback as {@code delta - (2^16 + 2^32)}; a negative
 * 64-bit delta is sent as-is. The alloc-srcloc begins hxcpp emits, and every
 * other delta, stay plain. Callstack samples put the thread before the
 * time. The u16 single/second string transfers store {@code length - 256}
 * (8-bit items carry the shorter strings). Message items gain a metadata
 * byte whose low nibble is the source: 0 the program, 1 the tracy client's
 * own status lines. The welcome is v76's (no delay field).
 */
final class TracyPackedWireFormat implements TracyWireFormat {

  private static final long TIME_OFFSET_16BIT = 1L << 16;
  private static final long TIME_OFFSET_32BIT = (1L << 16) + (1L << 32);
  private static final int STRING_LENGTH_OFFSET_8BIT = 1 << 8;
  private static final int MESSAGE_SOURCE_MASK = 0x0F;

  /** The table, welcome layout and plain reads are the classic format's. */
  private final TracyClassicWireFormat classic;

  TracyPackedWireFormat(@NotNull TracyQueueTable table) {
    classic = new TracyClassicWireFormat(table, false);
  }

  @Override
  public @NotNull TracyQueueTable table() {
    return classic.table();
  }

  @Override
  public int welcomeSize() {
    return classic.welcomeSize();
  }

  @Override
  public @NotNull TracyWelcome parseWelcome(byte @NotNull [] welcome, @NotNull TracyProtocolVersion version) {
    return classic.parseWelcome(welcome, version);
  }

  @Override
  public long zoneDelta(@NotNull TracyQueueType type, @NotNull DataInputStream in) throws IOException {
    return switch (type) {
      case ZoneBegin16, ZoneBeginCallstack16, ZoneEnd16 -> readU16Le(in);
      case ZoneBegin32, ZoneBeginCallstack32, ZoneEnd32 -> readDelta32(in);
      // the alloc-srcloc begins are the one zone item the client never packs
      case ZoneBeginAllocSrcLoc, ZoneBeginAllocSrcLocCallstack -> readLongLe(in);
      default -> readDelta64(in);
    };
  }

  /** Wire order: thread, then the packed delta. */
  @Override
  public long callstackSampleDelta(@NotNull TracyQueueType type, @NotNull DataInputStream in) throws IOException {
    skipFully(in, 4);
    return switch (type) {
      case CallstackSample16, CallstackSampleContextSwitch16 -> readU16Le(in);
      case CallstackSample32, CallstackSampleContextSwitch32 -> readDelta32(in);
      default -> readDelta64(in);
    };
  }

  @Override
  public int payloadLength(@NotNull TracyQueueType type, @NotNull DataInputStream in) throws IOException {
    int length = classic.payloadLength(type, in);
    boolean offset = type == TracyQueueType.SingleStringData || type == TracyQueueType.SecondStringData;
    return offset ? length + STRING_LENGTH_OFFSET_8BIT : length;
  }

  @Override
  public boolean messageFromProgram(@NotNull DataInputStream in) throws IOException {
    return (in.readUnsignedByte() & MESSAGE_SOURCE_MASK) == 0;
  }

  private static long readDelta32(@NotNull DataInputStream in) throws IOException {
    return Integer.toUnsignedLong(readIntLe(in)) + TIME_OFFSET_16BIT;
  }

  private static long readDelta64(@NotNull DataInputStream in) throws IOException {
    long delta = readLongLe(in);
    return delta >= 0 ? delta + TIME_OFFSET_32BIT : delta;
  }
}
