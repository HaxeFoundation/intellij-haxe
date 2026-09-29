package com.intellij.plugins.haxe.profiler.hxt;

import com.intellij.plugins.haxe.profiler.model.ProfilerEvent;
import com.intellij.plugins.haxe.profiler.model.ProfilerFormatException;
import com.intellij.plugins.haxe.profiler.model.ProfilerMemorySample;
import com.intellij.plugins.haxe.profiler.model.ProfilerSnapshot;
import com.intellij.plugins.haxe.profiler.model.ProfilerThread;
import com.intellij.plugins.haxe.profiler.model.StackFrame;
import com.intellij.plugins.haxe.profiler.model.StackSample;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

import static com.intellij.plugins.haxe.profiler.io.LittleEndian.*;

/**
 * The sampled capture of a v1 session, built record by record from its FRAME
 * payloads (layout in {@link HxtSessionTranslator}): the session's growing
 * name table, the frames interned from it, and the samples, events and heap
 * readings read so far. Sample times are reconstructed inside each frame's
 * window: the previous frame's stamp plus the cumulative tick time.
 */
final class HxtSampleAccumulator {

  private final int tickHz;
  /** The session's name table; 1-indexed, index 0 unused. */
  private final List<String> names = new ArrayList<>(List.of(""));
  private final Map<Integer, StackFrame> frames = new HashMap<>();
  private final List<StackSample> samples = new ArrayList<>();
  private final List<ProfilerEvent> events = new ArrayList<>();
  private final List<ProfilerMemorySample> memory = new ArrayList<>();
  private double windowStart;

  HxtSampleAccumulator(int tickHz, double startStamp) throws ProfilerFormatException {
    if (tickHz <= 0) throw new ProfilerFormatException("invalid tick rate: " + tickHz);
    this.tickHz = tickHz;
    this.windowStart = startStamp;
  }

  /** Reads one FRAME payload; the next frame's window starts at this frame's stamp. */
  void readFrame(byte @NotNull [] payload) throws IOException {
    DataInputStream data = new DataInputStream(new ByteArrayInputStream(payload));
    double stamp = readDoubleLe(data);
    int gcTimeUs = readIntLe(data);
    readIntLe(data); // gcOverheadUs, not charted
    long usedBytes = Integer.toUnsignedLong(readIntLe(data));
    long reservedBytes = Integer.toUnsignedLong(readIntLe(data));

    int nameCount = readIntLe(data);
    for (int i = 0; i < nameCount; i++) {
      names.add(readString16Le(data));
    }
    readSamples(data, stamp);

    // optional trailing fields - records written before them read as 0
    long allocatedBytes = data.available() >= 8 ? Integer.toUnsignedLong(readIntLe(data)) : 0;
    long freedBytes = data.available() >= 4 ? Integer.toUnsignedLong(readIntLe(data)) : 0;
    int flags = data.available() >= 1 ? data.readUnsignedByte() : 0;
    if ((flags & HxtSessionWriter.FLAG_NO_HEAP_READING) == 0) {
      memory.add(new ProfilerMemorySample(stamp, usedBytes, reservedBytes, allocatedBytes, freedBytes));
    }
    if ((flags & HxtSessionWriter.FLAG_SEGMENT_WINDOW) == 0) {
      events.add(new ProfilerEvent(stamp, 0, ProfilerEvent.FRAME_CODE, ""));
    }
    if (gcTimeUs > 0) {
      events.add(new ProfilerEvent(stamp, 0, ProfilerEvent.GC_TIME_CODE, Integer.toString(gcTimeUs)));
    }
    windowStart = stamp;
  }

  /** The sample groups {@code [depth, depth * nameIndex (root-first), deltaTicks]} of one frame ending at {@code stamp}. */
  private void readSamples(DataInputStream data, double stamp) throws IOException {
    int sampleInts = readIntLe(data);
    double tickSeconds = 1.0 / tickHz;
    long cumulativeTicks = 0;
    int consumed = 0;
    while (consumed < sampleInts) {
      int depth = readIntLe(data);
      if (depth < 0 || depth > sampleInts - consumed - 2) {
        throw new ProfilerFormatException("corrupt sample group (depth " + depth + ")");
      }
      List<StackFrame> stack = new ArrayList<>(depth);
      for (int i = 0; i < depth; i++) {
        stack.add(frameFor(readIntLe(data)));
      }
      int deltaTicks = Math.max(readIntLe(data), 1);
      consumed += depth + 2;
      cumulativeTicks += deltaTicks;
      double time = Math.min(windowStart + cumulativeTicks * tickSeconds, stamp);
      samples.add(new StackSample(time, 0, List.copyOf(stack), deltaTicks, false));
    }
  }

  private StackFrame frameFor(int nameIndex) throws ProfilerFormatException {
    if (nameIndex <= 0 || nameIndex >= names.size()) {
      throw new ProfilerFormatException("sample references unknown name index " + nameIndex);
    }
    return frames.computeIfAbsent(nameIndex, index -> parseFrame(names.get(index)));
  }

  /**
   * A name entry is {@code symbol} or {@code symbol(path/File.hx:123)}:
   * collectors that know source positions append them the way the HL dump
   * spells its descriptions, the hxcpp collector sends bare names. A
   * malformed suffix stays part of the symbol rather than failing.
   */
  private static StackFrame parseFrame(String name) {
    if (name.isEmpty() || name.charAt(name.length() - 1) != ')') {
      return new StackFrame(name, null, StackFrame.NO_LINE);
    }
    int open = name.lastIndexOf('(');
    int separator = name.lastIndexOf(':');
    if (open <= 0 || separator <= open) {
      return new StackFrame(name, null, StackFrame.NO_LINE);
    }
    try {
      int line = Integer.parseInt(name.substring(separator + 1, name.length() - 1).trim());
      String file = name.substring(open + 1, separator).replace('\\', '/');
      return new StackFrame(name.substring(0, open), file, line);
    }
    catch (NumberFormatException notAPosition) {
      return new StackFrame(name, null, StackFrame.NO_LINE);
    }
  }

  /** The capture read so far; list copies, safe to hand to another thread. */
  @NotNull
  ProfilerSnapshot snapshot(@NotNull String target, int version) {
    List<ProfilerThread> threads = List.of(ProfilerThread.main());
    return new ProfilerSnapshot(target, version, tickHz, threads, List.copyOf(samples),
                                List.copyOf(events), List.copyOf(memory));
  }
}
