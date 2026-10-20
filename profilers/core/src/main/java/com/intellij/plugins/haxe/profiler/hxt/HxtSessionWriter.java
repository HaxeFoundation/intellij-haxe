package com.intellij.plugins.haxe.profiler.hxt;

import com.intellij.plugins.haxe.profiler.io.LittleEndianPayload;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes an HXTS v1 sample session, the counterpart of
 * {@link HxtSessionTranslator}'s reader, for the IDE-side transcoders
 * (flash telemetry, V8 segments) whose sources are not v1 themselves. One
 * tick is one microsecond; the header goes out with the first frame and
 * every frame record is flushed, so a live view can follow the growing file.
 * Frame windows must arrive in order: each record's samples tile the
 * stretch from the previous frame's stamp to its own.
 */
public final class HxtSessionWriter {

  /** One tick = one microsecond: sample weights are exact clock time. */
  public static final int TICK_HZ = 1_000_000;
  /** The record's boundary is a collection flush, not a display frame; the reader emits no frame event for it. */
  public static final int FLAG_SEGMENT_WINDOW = 1;
  /** The record carries no heap reading; the reader emits no memory sample for it. */
  public static final int FLAG_NO_HEAP_READING = 2;

  private final OutputStream out;
  private final String target;
  /** The v1 name table across the whole session; per-record additions collect in {@code newNames}. */
  private final Map<String, Integer> nameIndexes = new HashMap<>();
  private final List<String> newNames = new ArrayList<>();
  private long bytesWritten;
  private boolean headerWritten;

  /**
   * A root-first stack and the microseconds it accounts for. A name entry
   * is {@code symbol} or {@code symbol(path/File.hx:123)}; the reader parses
   * a position suffix back into file and line.
   */
  public record WeightedStack(@NotNull List<String> rootFirstStack, long weightUs) {}

  public HxtSessionWriter(@NotNull OutputStream out, @NotNull String target) {
    this.out = out;
    this.target = target;
  }

  public long bytesWritten() {
    return bytesWritten;
  }

  /** A display-frame window with a heap reading; see {@link #writeFrame(double, long, long, long, List, int)}. */
  public void writeFrame(double stampSeconds, long gcUs, long usedBytes, long reservedBytes,
                         @NotNull List<WeightedStack> samples) throws IOException {
    writeFrame(stampSeconds, gcUs, usedBytes, reservedBytes, samples, 0);
  }

  /** One frame window ending at {@code stampSeconds}; {@code samples} tile it edge to edge in order. */
  public void writeFrame(double stampSeconds, long gcUs, long usedBytes, long reservedBytes,
                         @NotNull List<WeightedStack> samples, int flags) throws IOException {
    if (!headerWritten) {
      headerWritten = true;
      // transcoded sources rebase their clock to the session start
      HxtFormat.Header header = new HxtFormat.Header(HxtSessionTranslator.SAMPLES_VERSION, TICK_HZ, 0.0, target);
      bytesWritten += HxtFormat.writeHeader(out, header);
    }
    newNames.clear();
    List<Integer> sampleInts = sampleInts(samples);

    LittleEndianPayload payload = new LittleEndianPayload()
      .f64(stampSeconds)
      .i32(clampToInt(gcUs))
      .i32(0) // gcOverheadUs
      .i32(clampToInt(usedBytes))
      .i32(clampToInt(reservedBytes))
      .i32(newNames.size());
    for (String name : newNames) {
      payload.string16(name);
    }
    payload.i32(sampleInts.size());
    for (int value : sampleInts) {
      payload.i32(value);
    }
    if (flags != 0) {
      // the flags byte sits after the optional alloc counters, so both ship
      payload.i32(0) // allocatedBytes
        .i32(0) // freedBytes
        .u8(flags);
    }

    bytesWritten += HxtFormat.writeRecord(out, HxtSessionTranslator.FRAME_RECORD, payload);
    out.flush(); // the live view re-reads the file while it grows
  }

  /** The v1 sample groups {@code [depth, depth * nameIndex, weight]}, registering names new to the table. */
  private List<Integer> sampleInts(List<WeightedStack> samples) {
    List<Integer> sampleInts = new ArrayList<>();
    for (WeightedStack sample : samples) {
      sampleInts.add(sample.rootFirstStack().size());
      for (String frame : sample.rootFirstStack()) {
        sampleInts.add(nameIndex(frame));
      }
      sampleInts.add(clampToInt(sample.weightUs()));
    }
    return sampleInts;
  }

  private int nameIndex(String name) {
    Integer known = nameIndexes.get(name);
    if (known != null) return known;
    newNames.add(name);
    int index = nameIndexes.size() + 1; // the v1 table is 1-based
    nameIndexes.put(name, index);
    return index;
  }

  private static int clampToInt(long value) {
    return (int)Math.min(value, Integer.MAX_VALUE);
  }
}
