package com.intellij.plugins.haxe.profiler.hxt;

import com.intellij.plugins.haxe.profiler.model.ProfilerFormatException;
import com.intellij.plugins.haxe.profiler.model.ProfilerSnapshot;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.intellij.plugins.haxe.profiler.io.LittleEndian.readIntLe;

/**
 * Reads an HXTS telemetry session: the format the injected hxcpp collector
 * streams over the socket and the IDE receiver persists verbatim, so wire
 * and disk are the same bytes. Little-endian throughout.
 *
 * <pre>
 * header:  'H','X','T','S', u16 version, u32 tickHz,
 *          f64 startStamp (seconds), u16 targetLen + utf8 target
 * record:  u8 type, u32 payloadLength, payload — unknown types are skipped
 * FRAME (type 1) payload:
 *          f64 stamp (seconds, taken at stash = the window's END),
 *          i32 gcTimeUs, i32 gcOverheadUs, i32 usedBytes, i32 reservedBytes,
 *          i32 nameCount, nameCount * (u16 len + utf8)   — appended to the
 *              session's accumulated name table (1-indexed, index 0 unused),
 *          i32 sampleIntCount, that many i32s: groups of
 *              [depth, depth * nameIndex (root-first), deltaTicks],
 *          OPTIONAL trailing u32 allocatedBytes, u32 freedBytes — the
 *              window's allocation traffic from collectors that track it;
 *              records without them read as 0,
 *          OPTIONAL trailing u8 flags — bit 0 marks a COLLECTION-SEGMENT
 *              window (a transcoder's flush boundary, not a display frame:
 *              no frame event), bit 1 a record without a heap reading (no
 *              memory sample)
 * </pre>
 *
 * A truncated final record (the app died mid-write) ends the session
 * cleanly with what was read.
 */
public final class HxtSessionTranslator {

  static final int SAMPLES_VERSION = 1;
  static final int FRAME_RECORD = 1;

  private HxtSessionTranslator() {
  }

  /**
   * Reads any HXTS file: a v1 header dispatches to the sampled-capture path,
   * v5 and v6 to the disk-served zone store ({@link HxtZoneStore}; zone
   * captures are too big to load whole, so they need the file rather than a
   * stream, and v5 differs only in carrying its small series pre-rebased).
   * v2 to v4 are zone captures in abandoned development layouts and must be
   * captured again.
   */
  @NotNull
  public static HxtCapture translateCapture(@NotNull Path file) throws IOException {
    int version;
    try (InputStream in = new BufferedInputStream(Files.newInputStream(file))) {
      version = HxtFormat.readHeader(new DataInputStream(in)).version();
    }
    return switch (version) {
      case SAMPLES_VERSION -> {
        try (InputStream in = new BufferedInputStream(Files.newInputStream(file))) {
          yield new HxtCapture.Samples(translate(in));
        }
      }
      case HxtZoneStore.OLDEST_VERSION, HxtZoneWriter.VERSION -> new HxtCapture.Zones(HxtZoneStore.open(file));
      case 2, 3, 4 -> throw new ProfilerFormatException("this zone capture uses an older in-progress layout - capture it again");
      default -> throw new ProfilerFormatException("unsupported HXTS version " + version);
    };
  }

  /** The v1 (sampled) view of a stream; zone captures need {@link #translateCapture} with the file. */
  @NotNull
  public static ProfilerSnapshot translate(@NotNull InputStream in) throws IOException {
    DataInputStream data = new DataInputStream(in);
    HxtFormat.Header header = HxtFormat.readHeader(data);
    if (header.version() != SAMPLES_VERSION) {
      throw new ProfilerFormatException("this HXTS file holds a zone capture, not samples");
    }
    HxtSampleAccumulator capture = new HxtSampleAccumulator(header.tickHz(), header.startStamp());
    while (true) {
      int type = data.read();
      if (type < 0) break;
      try {
        byte[] payload = data.readNBytes(readIntLe(data));
        if (type == FRAME_RECORD) capture.readFrame(payload);
      }
      catch (EOFException truncated) {
        break; // the app died mid-record - keep what is complete
      }
    }
    return capture.snapshot(header.target(), header.version());
  }
}
