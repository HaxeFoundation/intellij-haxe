package com.intellij.plugins.haxe.profiler.hl;

import com.intellij.plugins.haxe.profiler.model.ProfilerEvent;
import com.intellij.plugins.haxe.profiler.model.ProfilerFormatException;
import com.intellij.plugins.haxe.profiler.model.ProfilerSnapshot;
import com.intellij.plugins.haxe.profiler.model.ProfilerThread;
import com.intellij.plugins.haxe.profiler.model.StackFrame;
import com.intellij.plugins.haxe.profiler.model.StackSample;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads a HashLink {@code hlprofile.dump} (the {@code PROF} binary the VM's
 * sampling profiler writes) into the neutral snapshot model. Little-endian
 * throughout.
 * <p>
 * Layout: {@code "PROF"} magic, int32 runtime version, int32 samples/sec,
 * then records of (double time, int32 threadId, int32 msgId) until a
 * {@code -1.0} time sentinel or EOF. A negative msgId is a stack sample:
 * bits 0-29 carry the frame count, bit 30 marks a sample taken inside a
 * major GC. Each frame is an int32 fileId — {@code -1} means an
 * unresolvable frame (nothing follows, the frame is dropped); bit 31 set
 * means a back-reference (int32 line follows, resolved against the
 * already-read element with that fileId and line); otherwise int32 line,
 * int32 length and that many UTF-16 units of the symbol description follow.
 * The writer flags each symbol in place after its first write, so every
 * later occurrence arrives as a back-reference. A non-negative msgId is a
 * custom event: int32 byte size plus UTF-16 data, code 0 being the
 * end-of-frame marker. A trailer holds (int32 count, then int32 tid +
 * length-prefixed UTF-8 name) thread names; the first-seen thread is "Main"
 * when unnamed.
 */
public final class HlProfDumpTranslator {

  public static final String TARGET = "hashlink";

  /** Frame count mask of a sample msgId; bit 30 = sample taken inside a major GC. */
  private static final int SAMPLE_FRAME_COUNT_MASK = 0x3FFFFFFF;
  private static final int SAMPLE_IN_GC_MAJOR = 0x40000000;
  private static final int BACK_REFERENCE_FLAG = 0x80000000;

  private HlProfDumpTranslator() {
  }

  @NotNull
  public static ProfilerSnapshot translate(@NotNull InputStream in) throws IOException {
    Reader reader = new Reader(in);
    byte[] magic = reader.readBytes(4);
    boolean isProf = magic[0] == 'P' && magic[1] == 'R' && magic[2] == 'O' && magic[3] == 'F';
    if (!isProf) {
      throw new ProfilerFormatException("Not a HashLink profiler dump (missing PROF magic)");
    }
    int version = reader.readInt();
    int samplesPerSecond = reader.readInt();

    Parse parse = new Parse();
    readRecords(reader, parse);
    readThreadNames(reader, parse);
    return snapshot(version, samplesPerSecond, parse);
  }

  /** Mutable state of one translation run. */
  private static final class Parse {
    // per-fileId interning maps, mirroring the writer's in-place flagging
    final List<FileElements> fileElements = new ArrayList<>();
    final Map<Integer, String> threadNames = new LinkedHashMap<>();
    final List<RawSample> rawSamples = new ArrayList<>();
    final List<ProfilerEvent> events = new ArrayList<>();
  }

  private static final class FileElements {
    final Map<Integer, Element> byLine = new LinkedHashMap<>();
    final Map<String, Element> bySymbol = new LinkedHashMap<>();
  }

  /**
   * Interned symbol, mutable because merging is retroactive: when the same
   * symbol shows up at several source lines the earliest line wins for ALL
   * samples, including ones already read (the reference reader does the
   * same). Frozen into {@link StackFrame}s only after the whole read.
   */
  private static final class Element {
    String symbol;
    @Nullable String file;
    int line = StackFrame.NO_LINE;
  }

  /** Frames LEAF-FIRST as the dump stores them; reversed when frozen. */
  private record RawSample(double time, int threadId, List<Element> frames, boolean inGcMajor) {
  }

  private static void readRecords(Reader reader, Parse parse) throws IOException {
    while (true) {
      Double time = reader.readDoubleOrEndOfInput();
      if (time == null || time == -1.0) return;
      int threadId = reader.readInt();
      parse.threadNames.putIfAbsent(threadId, null);
      int msgId = reader.readInt();
      if (msgId < 0) {
        readSample(reader, parse, time, threadId, msgId);
      }
      else {
        readEvent(reader, parse, time, threadId, msgId);
      }
    }
  }

  private static void readSample(Reader reader, Parse parse, double time, int threadId, int msgId) throws IOException {
    int frameCount = msgId & SAMPLE_FRAME_COUNT_MASK;
    List<Element> frames = new ArrayList<>(frameCount);
    for (int i = 0; i < frameCount; i++) {
      Element element = readFrame(reader, parse);
      if (element != null) {
        frames.add(element);
      }
    }
    boolean inGcMajor = (msgId & SAMPLE_IN_GC_MAJOR) != 0;
    parse.rawSamples.add(new RawSample(time, threadId, frames, inGcMajor));
  }

  /** One frame of a sample; null for the writer's unresolvable-frame marker. */
  @Nullable
  private static Element readFrame(Reader reader, Parse parse) throws IOException {
    int fileId = reader.readInt();
    if (fileId == -1) return null;
    int line = reader.readInt();
    if ((fileId & BACK_REFERENCE_FLAG) != 0) {
      return resolveBackReference(parse, fileId & ~BACK_REFERENCE_FLAG, line);
    }
    String description = reader.readUtf16(reader.readInt());
    return internElement(parse, fileId, line, description);
  }

  @NotNull
  private static Element resolveBackReference(Parse parse, int fileId, int line) throws ProfilerFormatException {
    FileElements elements = fileId < parse.fileElements.size() ? parse.fileElements.get(fileId) : null;
    Element element = elements == null ? null : elements.byLine.get(line);
    if (element == null) {
      throw new ProfilerFormatException("Back-reference to unknown element (file " + fileId + ", line " + line + ")");
    }
    return element;
  }

  /**
   * Interns a newly described element. Same symbol at different lines merges
   * into one element carrying the smallest line — without this a method
   * sampled across its body would split into one node per line downstream.
   * The line KEY still maps to the merged element so back-references resolve.
   */
  @NotNull
  private static Element internElement(Parse parse, int fileId, int line, String description) {
    while (parse.fileElements.size() <= fileId) {
      parse.fileElements.add(new FileElements());
    }
    FileElements elements = parse.fileElements.get(fileId);

    Element element = parseDescription(description);
    Element existing = elements.bySymbol.get(element.symbol);
    if (existing != null) {
      if (element.line != StackFrame.NO_LINE && (existing.line == StackFrame.NO_LINE || element.line < existing.line)) {
        existing.line = element.line;
      }
      element = existing;
    }
    else {
      elements.bySymbol.put(element.symbol, element);
    }
    elements.byLine.put(line, element);
    return element;
  }

  /**
   * The symbol description is {@code pack.Class.method(path\File.hx:123)} —
   * the source position rides inside the text, the record's own line int is
   * only the interning key. Compiler name mangling is undone: {@code $}
   * closure markers are dropped and {@code .__constructor__} reads as the
   * {@code .new} the user wrote.
   */
  @NotNull
  private static Element parseDescription(String description) {
    Element element = new Element();
    element.symbol = description;
    if (description.isEmpty() || description.charAt(description.length() - 1) != ')') return element;
    int open = description.lastIndexOf('(');
    int separator = description.lastIndexOf(':');
    if (open <= 0 || separator <= open) return element;

    element.file = description.substring(open + 1, separator).replace('\\', '/');
    try {
      element.line = Integer.parseInt(description.substring(separator + 1, description.length() - 1).trim());
    }
    catch (NumberFormatException ignored) {
      // a ')' suffix that is not a position; keep the full description as the symbol
      element.file = null;
      return element;
    }
    String symbol = description.substring(0, open).replace("$", "");
    if (symbol.endsWith(".__constructor__")) {
      symbol = symbol.substring(0, symbol.length() - "__constructor__".length()) + "new";
    }
    element.symbol = symbol;
    return element;
  }

  private static void readEvent(Reader reader, Parse parse, double time, int threadId, int msgId) throws IOException {
    int byteSize = reader.readInt();
    byte[] bytes = reader.readBytes(byteSize);
    String data = new String(bytes, StandardCharsets.UTF_16LE);
    parse.events.add(new ProfilerEvent(time, threadId, msgId, data));
  }

  /** The trailer is optional: a dump truncated after the records still loads, threads keep default names. */
  private static void readThreadNames(Reader reader, Parse parse) throws IOException {
    Integer count = reader.readIntOrEndOfInput();
    if (count == null) return;
    for (int i = 0; i < count; i++) {
      int threadId = reader.readInt();
      String name = new String(reader.readBytes(reader.readInt()), StandardCharsets.UTF_8);
      if (parse.threadNames.containsKey(threadId)) {
        parse.threadNames.put(threadId, name);
      }
    }
  }

  @NotNull
  private static ProfilerSnapshot snapshot(int version, int samplesPerSecond, Parse parse) {
    List<ProfilerThread> threads = new ArrayList<>();
    boolean first = true;
    for (Map.Entry<Integer, String> entry : parse.threadNames.entrySet()) {
      String defaultName = first ? ProfilerThread.MAIN_NAME : ProfilerThread.unnamed(entry.getKey());
      threads.add(new ProfilerThread(entry.getKey(), entry.getValue() != null ? entry.getValue() : defaultName));
      first = false;
    }

    Map<Element, StackFrame> frozen = new IdentityHashMap<>();
    List<StackSample> samples = new ArrayList<>(parse.rawSamples.size());
    for (RawSample raw : parse.rawSamples) {
      samples.add(freeze(raw, frozen));
    }
    // the PROF dump carries no heap readings
    List<ProfilerEvent> events = List.copyOf(parse.events);
    return new ProfilerSnapshot(TARGET, version, samplesPerSecond, List.copyOf(threads), List.copyOf(samples), events, List.of());
  }

  /** Reverses the dump's leaf-first order into the model's root-first one. */
  @NotNull
  private static StackSample freeze(RawSample raw, Map<Element, StackFrame> frozen) {
    List<StackFrame> frames = new ArrayList<>(raw.frames().size());
    for (int i = raw.frames().size() - 1; i >= 0; i--) {
      Element element = raw.frames().get(i);
      StackFrame frame = frozen.computeIfAbsent(element, e -> new StackFrame(e.symbol, e.file, e.line));
      frames.add(frame);
    }
    return new StackSample(raw.time(), raw.threadId(), List.copyOf(frames), 1, raw.inGcMajor());
  }

  /** Little-endian primitive reads; end-of-input is only legal at a record boundary. */
  private static final class Reader {
    private final InputStream in;
    private final ByteBuffer scratch = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);

    Reader(InputStream in) {
      this.in = in;
    }

    @NotNull
    byte[] readBytes(int count) throws IOException {
      if (count < 0) {
        throw new ProfilerFormatException("Corrupt dump: negative length " + count);
      }
      byte[] bytes = in.readNBytes(count);
      if (bytes.length != count) {
        throw new EOFException("Truncated dump: expected " + count + " bytes, got " + bytes.length);
      }
      return bytes;
    }

    int readInt() throws IOException {
      fill(4);
      return scratch.getInt(0);
    }

    /** Null at clean end of input; a PARTIAL int is truncation and throws. */
    @Nullable
    Integer readIntOrEndOfInput() throws IOException {
      int read = in.readNBytes(scratch.array(), 0, 4);
      if (read == 0) return null;
      if (read != 4) throw new EOFException("Truncated dump: partial trailer");
      return scratch.getInt(0);
    }

    /** Null at clean end of input; a PARTIAL double is truncation and throws. */
    @Nullable
    Double readDoubleOrEndOfInput() throws IOException {
      int read = in.readNBytes(scratch.array(), 0, 8);
      if (read == 0) return null;
      if (read != 8) throw new EOFException("Truncated dump: partial record");
      return scratch.getDouble(0);
    }

    @NotNull
    String readUtf16(int units) throws IOException {
      return new String(readBytes(units * 2), StandardCharsets.UTF_16LE);
    }

    private void fill(int count) throws IOException {
      int read = in.readNBytes(scratch.array(), 0, count);
      if (read != count) {
        throw new EOFException("Truncated dump: expected " + count + " bytes, got " + read);
      }
    }
  }
}
