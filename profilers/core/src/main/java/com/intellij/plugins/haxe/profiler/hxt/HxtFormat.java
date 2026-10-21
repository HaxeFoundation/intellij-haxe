package com.intellij.plugins.haxe.profiler.hxt;

import com.intellij.plugins.haxe.profiler.io.LittleEndianPayload;
import com.intellij.plugins.haxe.profiler.model.ProfilerFormatException;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import org.jetbrains.annotations.NotNull;

import static com.intellij.plugins.haxe.profiler.io.LittleEndian.*;

/**
 * The HXTS container shared by both capture shapes: the header, the record
 * framing ({@code u8 type, u32 payloadLength, payload}) and the deflated zone
 * chunks. {@link HxtSessionTranslator} documents the v1 records,
 * {@link HxtZoneWriter} the zone records.
 */
final class HxtFormat {

  static final byte[] MAGIC = {'H', 'X', 'T', 'S'};
  /** The record type byte plus its u32 payload length. */
  static final int RECORD_HEADER_BYTES = 1 + 4;
  /** A ZONES record's bounds ahead of its deflated zones: i32 count, i64 minStart, maxEnd and maxDuration. */
  static final int CHUNK_BOUNDS_BYTES = 4 + 8 + 8 + 8;

  /** The file header: {@code 'H','X','T','S', u16 version, u32 tickHz, f64 startStamp, u16 + utf8 target}. */
  record Header(int version, int tickHz, double startStamp, @NotNull String target) {

    /** The header's size in the file, where the first record starts. */
    long byteLength() {
      return MAGIC.length + 2 + 4 + 8 + 2 + target.getBytes(StandardCharsets.UTF_8).length;
    }
  }

  private HxtFormat() {
  }

  @NotNull
  static Header readHeader(@NotNull DataInputStream in) throws IOException {
    byte[] magic = in.readNBytes(MAGIC.length);
    if (!Arrays.equals(magic, MAGIC)) {
      throw new ProfilerFormatException("not an HXTS telemetry session");
    }
    int version = readU16Le(in);
    int tickHz = readIntLe(in);
    double startStamp = readDoubleLe(in);
    return new Header(version, tickHz, startStamp, readString16Le(in));
  }

  /** Returns the bytes written. */
  static int writeHeader(@NotNull OutputStream out, @NotNull Header header) throws IOException {
    LittleEndianPayload bytes = new LittleEndianPayload()
      .raw(MAGIC)
      .u16(header.version())
      .i32(header.tickHz())
      .f64(header.startStamp())
      .string16(header.target());
    bytes.writeTo(out);
    return bytes.size();
  }

  /** Writes one framed record; returns the bytes written. */
  static int writeRecord(@NotNull OutputStream out, int type, @NotNull LittleEndianPayload payload) throws IOException {
    LittleEndianPayload header = new LittleEndianPayload()
      .u8(type)
      .i32(payload.size());
    header.writeTo(out);
    payload.writeTo(out);
    return RECORD_HEADER_BYTES + payload.size();
  }

  /** One independent raw DEFLATE stream of {@code raw[0, length)}. */
  static byte @NotNull [] deflate(byte @NotNull [] raw, int length, int level) {
    Deflater deflater = new Deflater(level);
    deflater.setInput(raw, 0, length);
    deflater.finish();
    ByteArrayOutputStream compressed = new ByteArrayOutputStream();
    byte[] buffer = new byte[64 * 1024];
    while (!deflater.finished()) {
      compressed.write(buffer, 0, deflater.deflate(buffer));
    }
    deflater.end();
    return compressed.toByteArray();
  }

  /** Inflates {@code compressed[0, compressedLength)} into exactly {@code rawLength} bytes of {@code raw}. */
  static void inflate(byte @NotNull [] compressed, int compressedLength, byte @NotNull [] raw, int rawLength)
    throws IOException {
    Inflater inflater = new Inflater();
    inflater.setInput(compressed, 0, compressedLength);
    try {
      int total = 0;
      while (total < rawLength) {
        int read = inflater.inflate(raw, total, rawLength - total);
        if (read == 0) {
          throw new ProfilerFormatException("zone chunk decompressed short: " + total + " of " + rawLength);
        }
        total += read;
      }
    }
    catch (DataFormatException corrupted) {
      throw new ProfilerFormatException("zone chunk does not decompress: " + corrupted.getMessage());
    }
    finally {
      inflater.end();
    }
  }
}
