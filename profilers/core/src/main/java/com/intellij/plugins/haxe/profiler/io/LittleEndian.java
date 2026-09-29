package com.intellij.plugins.haxe.profiler.io;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.jetbrains.annotations.NotNull;

/**
 * Little-endian primitive reads over a {@link DataInputStream}, whose own
 * reads are big-endian. Every capture format the profilers read is
 * little-endian. A stream ending inside a value throws
 * {@link java.io.EOFException}.
 */
public final class LittleEndian {

  private LittleEndian() {
  }

  public static int readU16Le(@NotNull DataInputStream in) throws IOException {
    return Short.toUnsignedInt(Short.reverseBytes(in.readShort()));
  }

  public static int readIntLe(@NotNull DataInputStream in) throws IOException {
    return Integer.reverseBytes(in.readInt());
  }

  public static long readLongLe(@NotNull DataInputStream in) throws IOException {
    return Long.reverseBytes(in.readLong());
  }

  public static long readU48Le(@NotNull DataInputStream in) throws IOException {
    long low = Integer.toUnsignedLong(readIntLe(in));
    return low | (long)readU16Le(in) << 32;
  }

  public static double readDoubleLe(@NotNull DataInputStream in) throws IOException {
    return Double.longBitsToDouble(readLongLe(in));
  }

  /** {@code length} bytes of UTF-8. */
  @NotNull
  public static String readUtf8(@NotNull DataInputStream in, int length) throws IOException {
    byte[] utf8 = new byte[length];
    in.readFully(utf8);
    return new String(utf8, StandardCharsets.UTF_8);
  }

  /** A u16 length followed by that many bytes of UTF-8. */
  @NotNull
  public static String readString16Le(@NotNull DataInputStream in) throws IOException {
    return readUtf8(in, readU16Le(in));
  }

  public static void skipFully(@NotNull DataInputStream in, int count) throws IOException {
    in.skipNBytes(count);
  }
}
