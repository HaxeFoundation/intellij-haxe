package com.intellij.plugins.haxe.profiler.hxt;

import com.intellij.plugins.haxe.profiler.io.LittleEndianPayload;
import com.intellij.plugins.haxe.profiler.model.ProfilerFormatException;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

import static com.intellij.plugins.haxe.profiler.io.LittleEndian.readIntLe;

/**
 * Re-deflates a finished capture's zone chunks at a higher level. A LIVE
 * capture writes cheap ({@link HxtZoneWriter#LIVE_LEVEL}) so the receiver
 * does not steal CPU from the app it is profiling; this pass brings the
 * file to the archive level once the app has exited. Every non-zone record
 * is copied verbatim except INFO, whose trailing level byte is updated to
 * match the rewritten chunks. The rewrite lands in a temp sibling and then
 * replaces the original.
 */
public final class HxtZoneRecompressor {

  /** The INFO fields between the program name and the level byte: pid, epoch, duration, unmatched ends, base. */
  private static final int INFO_FIELDS_BEFORE_LEVEL = 8 + 8 + 8 + 4 + 8;

  private HxtZoneRecompressor() {
  }

  public static void recompress(@NotNull Path file, int level) throws IOException {
    Path temp = file.resolveSibling(file.getFileName() + ".recompress");
    try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)));
         OutputStream out = new BufferedOutputStream(Files.newOutputStream(temp))) {
      HxtFormat.writeHeader(out, HxtFormat.readHeader(in));
      byte[] raw = new byte[0];
      while (true) {
        int type = in.read();
        if (type < 0) break;
        int length = readIntLe(in);
        byte[] payload = in.readNBytes(length);
        if (payload.length < length) throw new ProfilerFormatException("record truncated during recompression");
        byte[] rewritten = switch (type) {
          case HxtZoneWriter.INFO_RECORD -> withLevelByte(payload, level);
          case HxtZoneWriter.ZONES_RECORD -> {
            int rawLength = zoneCount(payload) * HxtZoneWriter.ZONE_BYTES;
            if (raw.length < rawLength) raw = new byte[rawLength];
            yield recompressedChunk(payload, raw, rawLength, level);
          }
          default -> payload;
        };
        HxtFormat.writeRecord(out, type, new LittleEndianPayload().raw(rewritten));
      }
    }
    catch (IOException e) {
      Files.deleteIfExists(temp);
      throw e;
    }
    replaceRetrying(temp, file);
  }

  /** A ZONES payload with its deflated zones re-deflated at {@code level}; the bounds ahead of them stay. */
  private static byte[] recompressedChunk(byte[] payload, byte[] raw, int rawLength, int level) throws IOException {
    int boundsLength = HxtFormat.CHUNK_BOUNDS_BYTES;
    byte[] compressed = Arrays.copyOfRange(payload, boundsLength, payload.length);
    HxtFormat.inflate(compressed, compressed.length, raw, rawLength);
    byte[] recompressed = HxtFormat.deflate(raw, rawLength, level);
    byte[] rewritten = Arrays.copyOf(payload, boundsLength + recompressed.length);
    System.arraycopy(recompressed, 0, rewritten, boundsLength, recompressed.length);
    return rewritten;
  }

  /** The zone count leading a ZONES payload's bounds (u32 little-endian). */
  private static int zoneCount(byte[] payload) {
    return payload[0] & 0xFF | (payload[1] & 0xFF) << 8 | (payload[2] & 0xFF) << 16 | (payload[3] & 0xFF) << 24;
  }

  /**
   * A live view's refresh tick may hold the original open for a moment, and
   * on Windows an open reader blocks the replace with a sharing violation;
   * a short bounded retry outlasts any single read. On give-up the temp is
   * removed and the original, complete and valid at its live level, stays.
   */
  private static void replaceRetrying(Path temp, Path file) throws IOException {
    IOException lastFailure = null;
    for (int attempt = 0; attempt < 10; attempt++) {
      try {
        Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        return;
      }
      catch (IOException locked) {
        lastFailure = locked;
        try {
          Thread.sleep(200);
        }
        catch (InterruptedException interrupted) {
          Thread.currentThread().interrupt();
          break;
        }
      }
    }
    Files.deleteIfExists(temp);
    throw lastFailure;
  }

  /**
   * The INFO payload carries an optional u8 deflate level after its fixed
   * fields; sets it (patching or appending) so the file names the level its
   * rewritten chunks actually carry. A payload too short for its own fixed
   * fields is left alone.
   */
  private static byte[] withLevelByte(byte[] payload, int level) {
    if (payload.length < 2) return payload;
    int nameLength = payload[0] & 0xFF | (payload[1] & 0xFF) << 8;
    int levelOffset = 2 + nameLength + INFO_FIELDS_BEFORE_LEVEL;
    if (payload.length < levelOffset) return payload;
    byte[] patched = Arrays.copyOf(payload, Math.max(payload.length, levelOffset + 1));
    patched[levelOffset] = (byte)level;
    return patched;
  }
}
