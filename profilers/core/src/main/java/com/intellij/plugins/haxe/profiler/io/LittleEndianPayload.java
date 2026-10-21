package com.intellij.plugins.haxe.profiler.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.jetbrains.annotations.NotNull;

/** Little-endian assembly of one record's bytes, the write side of {@link LittleEndian}. */
public final class LittleEndianPayload {

  private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

  public LittleEndianPayload u8(int value) {
    bytes.write(value & 0xFF);
    return this;
  }

  public LittleEndianPayload u16(int value) {
    bytes.write(value & 0xFF);
    bytes.write(value >> 8 & 0xFF);
    return this;
  }

  public LittleEndianPayload i32(int value) {
    for (int shift = 0; shift < 32; shift += 8) {
      bytes.write(value >> shift & 0xFF);
    }
    return this;
  }

  public LittleEndianPayload i64(long value) {
    for (int shift = 0; shift < 64; shift += 8) {
      bytes.write((int)(value >> shift & 0xFF));
    }
    return this;
  }

  public LittleEndianPayload f64(double value) {
    return i64(Double.doubleToLongBits(value));
  }

  /** A u16 length followed by the string's UTF-8 bytes. */
  public LittleEndianPayload string16(@NotNull String value) {
    byte[] utf8 = value.getBytes(StandardCharsets.UTF_8);
    u16(utf8.length);
    bytes.writeBytes(utf8);
    return this;
  }

  public LittleEndianPayload raw(byte @NotNull [] value) {
    bytes.writeBytes(value);
    return this;
  }

  public int size() {
    return bytes.size();
  }

  public void writeTo(@NotNull OutputStream out) throws IOException {
    bytes.writeTo(out);
  }
}
