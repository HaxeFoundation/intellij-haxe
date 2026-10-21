package com.intellij.plugins.haxe.profiler.tracy.wire;

import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import static com.intellij.plugins.haxe.profiler.io.LittleEndian.*;

/**
 * The encoding shared by protocols 69, 74 and 76 (Tracy 0.11 to 0.13):
 * every delta is a plain 64-bit value, callstack samples put the time
 * before the thread, string payloads carry their plain length, and message
 * items end with their colour. The versions differ only in item numbering
 * (the table) and in whether the welcome still carries the queue-delay
 * calibration field, which v76 dropped.
 */
final class TracyClassicWireFormat implements TracyWireFormat {

  private static final int WELCOME_FIXED_SIZE = 8 * 8 + 1 + 1 + 12 + 4 + 64 + 1024;
  private static final int ON_DEMAND_FLAG = 1;

  private final TracyQueueTable table;
  private final boolean welcomeHasDelay;

  TracyClassicWireFormat(@NotNull TracyQueueTable table, boolean welcomeHasDelay) {
    this.table = table;
    this.welcomeHasDelay = welcomeHasDelay;
  }

  @Override
  public @NotNull TracyQueueTable table() {
    return table;
  }

  @Override
  public int welcomeSize() {
    return WELCOME_FIXED_SIZE + (welcomeHasDelay ? 8 : 0);
  }

  /**
   * The packed WelcomeMessage: timerMul f64, initBegin, initEnd, [delay],
   * resolution, epoch, exectime, pid, samplingPeriod (i64/u64 each), flags
   * u8, cpuArch u8, cpuManufacturer[12], cpuId u32, programName[64],
   * hostInfo[1024].
   */
  @Override
  public @NotNull TracyWelcome parseWelcome(byte @NotNull [] welcome, @NotNull TracyProtocolVersion version) {
    ByteBuffer buffer = ByteBuffer.wrap(welcome).order(ByteOrder.LITTLE_ENDIAN);
    double timerMul = buffer.getDouble();
    long initBegin = buffer.getLong();
    long initEnd = buffer.getLong();
    long delay = welcomeHasDelay ? buffer.getLong() : 0;
    long resolution = buffer.getLong();
    long epoch = buffer.getLong();
    long execTime = buffer.getLong();
    long pid = buffer.getLong();
    long samplingPeriod = buffer.getLong();
    int flags = buffer.get() & 0xFF;
    buffer.get();       // cpuArch
    buffer.position(buffer.position() + 12 + 4); // cpuManufacturer, cpuId

    byte[] name = new byte[64];
    buffer.get(name);
    int nameEnd = 0;
    while (nameEnd < name.length && name[nameEnd] != 0) nameEnd++;
    String programName = new String(name, 0, nameEnd, StandardCharsets.UTF_8);

    return new TracyWelcome(version, timerMul, initBegin, initEnd, delay, resolution, epoch, execTime, pid,
                            samplingPeriod, (flags & ON_DEMAND_FLAG) != 0, programName);
  }

  @Override
  public long zoneDelta(@NotNull TracyQueueType type, @NotNull DataInputStream in) throws IOException {
    return readLongLe(in);
  }

  /** Wire order: 64-bit delta, then the thread. */
  @Override
  public long callstackSampleDelta(@NotNull TracyQueueType type, @NotNull DataInputStream in) throws IOException {
    long delta = readLongLe(in);
    skipFully(in, 4);
    return delta;
  }

  @Override
  public int payloadLength(@NotNull TracyQueueType type, @NotNull DataInputStream in) throws IOException {
    return switch (type.payload()) {
      case NONE -> 0;
      case U8 -> in.readUnsignedByte();
      case U16 -> readU16Le(in);
      case U32 -> readIntLe(in);
    };
  }

  @Override
  public boolean messageFromProgram(@NotNull DataInputStream in) {
    return true;
  }
}
