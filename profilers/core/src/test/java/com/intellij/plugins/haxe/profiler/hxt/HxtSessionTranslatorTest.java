package com.intellij.plugins.haxe.profiler.hxt;

import com.intellij.plugins.haxe.profiler.model.ProfilerEvent;
import com.intellij.plugins.haxe.profiler.model.ProfilerFormatException;
import com.intellij.plugins.haxe.profiler.model.ProfilerMemorySample;
import com.intellij.plugins.haxe.profiler.model.ProfilerSnapshot;
import com.intellij.plugins.haxe.profiler.model.StackFrame;
import com.intellij.plugins.haxe.profiler.model.StackSample;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("hxcpp telemetry: session translator")
public class HxtSessionTranslatorTest {

  @Test
  @DisplayName("reconstructs sample times inside each frame window")
  public void testReconstructsSampleTimesInsideEachFrameWindow() throws IOException {
    SessionBuilder session = new SessionBuilder(1000, 10.0);
    // frame at 10.016: two samples, [Main.main > Game.update] 3 ticks then [Main.main] 1 tick
    session.beginFrame(10.016, 0, List.of("Main.main", "Game.update"));
    session.sample(new int[]{1, 2}, 3);
    session.sample(new int[]{1}, 1);
    session.endFrame();

    ProfilerSnapshot snapshot = HxtSessionTranslator.translate(session.stream());

    assertEquals(1000, snapshot.samplesPerSecond());
    assertEquals("hxcpp", snapshot.target());
    assertEquals(2, snapshot.samples().size());
    StackSample first = snapshot.samples().get(0);
    assertEquals(10.003, first.time(), 1e-9, "3 ticks after the window start");
    assertEquals(List.of("Main.main", "Game.update"),
                 first.frames().stream().map(frame -> frame.symbol()).toList());
    assertEquals(3, first.weight());
    assertEquals(10.004, snapshot.samples().get(1).time(), 1e-9);
  }

  @Test
  @DisplayName("name table accumulates across frames and stays one indexed")
  public void testNameTableAccumulatesAcrossFramesAndStaysOneIndexed() throws IOException {
    SessionBuilder session = new SessionBuilder(1000, 1.0);
    session.beginFrame(1.016, 0, List.of("Main.main"));
    session.sample(new int[]{1}, 1);
    session.endFrame();
    // the second frame ships only the NEW name; index 1 still resolves
    session.beginFrame(1.033, 0, List.of("Game.render"));
    session.sample(new int[]{1, 2}, 2);
    session.endFrame();

    ProfilerSnapshot snapshot = HxtSessionTranslator.translate(session.stream());

    StackSample second = snapshot.samples().get(1);
    assertEquals(List.of("Main.main", "Game.render"),
                 second.frames().stream().map(frame -> frame.symbol()).toList());
  }

  @Test
  @DisplayName("name entries with a source suffix parse into file and line")
  public void testNameEntriesWithASourceSuffixParseIntoFileAndLine() throws IOException {
    SessionBuilder session = new SessionBuilder(1000, 2.0);
    session.beginFrame(2.016, 0, List.of("Main.main(src/Main.hx:12)", "Game.update", "odd(name)"));
    session.sample(new int[]{1, 2, 3}, 1);
    session.endFrame();

    ProfilerSnapshot snapshot = HxtSessionTranslator.translate(session.stream());

    List<StackFrame> stack = snapshot.samples().get(0).frames();
    assertEquals(new StackFrame("Main.main", "src/Main.hx", 12), stack.get(0));
    assertEquals(new StackFrame("Game.update", null, StackFrame.NO_LINE), stack.get(1), "bare names stay bare");
    assertEquals(new StackFrame("odd(name)", null, StackFrame.NO_LINE), stack.get(2),
                 "a paren suffix without a position stays part of the symbol");
  }

  @Test
  @DisplayName("frame heap readings surface as memory samples")
  public void testFrameHeapReadingsSurfaceAsMemorySamples() throws IOException {
    SessionBuilder session = new SessionBuilder(1000, 10.0);
    session.beginFrame(10.016, 0, 4096, 8192, List.of("Main.main"));
    session.sample(new int[]{1}, 1);
    session.endFrame();
    session.beginFrame(10.032, 0, 6144, 8192, List.of());
    session.endFrame();

    ProfilerSnapshot snapshot = HxtSessionTranslator.translate(session.stream());

    assertEquals(List.of(new ProfilerMemorySample(10.016, 4096, 8192),
                         new ProfilerMemorySample(10.032, 6144, 8192)),
                 snapshot.memory());
  }

  @Test
  @DisplayName("frames become frame events and gc time becomes a gc event")
  public void testFramesBecomeFrameEventsAndGcTimeBecomesAGcEvent() throws IOException {
    SessionBuilder session = new SessionBuilder(1000, 5.0);
    session.beginFrame(5.016, 2500, List.of("Main.main"));
    session.sample(new int[]{1}, 1);
    session.endFrame();
    session.beginFrame(5.033, 0, List.of());
    session.endFrame();

    ProfilerSnapshot snapshot = HxtSessionTranslator.translate(session.stream());

    List<ProfilerEvent> frameEvents = snapshot.events().stream()
      .filter(event -> event.code() == ProfilerEvent.FRAME_CODE)
      .toList();
    assertEquals(2, frameEvents.size());
    assertEquals(5.016, frameEvents.get(0).time(), 1e-9);

    List<ProfilerEvent> gcEvents = snapshot.events().stream()
      .filter(event -> event.code() == ProfilerEvent.GC_TIME_CODE)
      .toList();
    assertEquals(1, gcEvents.size(), "only the frame with gc time carries a gc event");
    assertEquals("2500", gcEvents.get(0).data());
  }

  @Test
  @DisplayName("trailing allocation fields surface and records without them read as zero")
  public void testTrailingAllocationFieldsSurfaceAndRecordsWithoutThemReadAsZero() throws IOException {
    SessionBuilder session = new SessionBuilder(1000, 10.0);
    session.beginFrame(10.016, 0, 4096, 0, List.of("Main.main"));
    session.sample(new int[]{1}, 1);
    session.endFrameWithAllocations(2048, 512);
    session.beginFrame(10.032, 0, 4096, 0, List.of());
    session.endFrame();

    ProfilerSnapshot snapshot = HxtSessionTranslator.translate(session.stream());

    assertEquals(List.of(new ProfilerMemorySample(10.016, 4096, 0, 2048, 512),
                         new ProfilerMemorySample(10.032, 4096, 0)),
                 snapshot.memory());
  }

  @Test
  @DisplayName("a truncated final record keeps the complete frames")
  public void testATruncatedFinalRecordKeepsTheCompleteFrames() throws IOException {
    SessionBuilder session = new SessionBuilder(1000, 1.0);
    session.beginFrame(1.016, 0, List.of("Main.main"));
    session.sample(new int[]{1}, 1);
    session.endFrame();
    byte[] complete = session.bytes();
    // the app died mid-write: a record header promising more than exists
    byte[] truncated = new byte[complete.length + 5];
    System.arraycopy(complete, 0, truncated, 0, complete.length);
    truncated[complete.length] = 1;
    truncated[complete.length + 1] = (byte)200; // payload length 200, no payload follows

    ProfilerSnapshot snapshot = HxtSessionTranslator.translate(new ByteArrayInputStream(truncated));

    assertEquals(1, snapshot.samples().size(), "the complete frame survives");
  }

  @Test
  @DisplayName("unknown record types are skipped by their length")
  public void testUnknownRecordTypesAreSkippedByTheirLength() throws IOException {
    SessionBuilder session = new SessionBuilder(1000, 1.0);
    session.rawRecord(99, new byte[]{1, 2, 3, 4});
    session.beginFrame(1.016, 0, List.of("Main.main"));
    session.sample(new int[]{1}, 1);
    session.endFrame();

    ProfilerSnapshot snapshot = HxtSessionTranslator.translate(session.stream());

    assertEquals(1, snapshot.samples().size());
  }

  @Test
  @DisplayName("foreign bytes fail as not a session")
  public void testForeignBytesFailAsNotASession() {
    InputStream foreign = new ByteArrayInputStream("PROF whatever".getBytes(StandardCharsets.UTF_8));

    assertThrows(ProfilerFormatException.class, () -> HxtSessionTranslator.translate(foreign));
  }

  /** Writes HXTS bytes the way the injected collector does — the format's executable spec. */
  private static final class SessionBuilder {
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private ByteArrayOutputStream frame;

    SessionBuilder(int tickHz, double startStamp) {
      out.writeBytes(HxtFormat.MAGIC);
      writeU16(out, 1);
      writeInt(out, tickHz);
      writeDouble(out, startStamp);
      byte[] target = "hxcpp".getBytes(StandardCharsets.UTF_8);
      writeU16(out, target.length);
      out.writeBytes(target);
    }

    void beginFrame(double stamp, int gcTimeUs, List<String> newNames) {
      beginFrame(stamp, gcTimeUs, 0, 0, newNames);
    }

    void beginFrame(double stamp, int gcTimeUs, int usedBytes, int reservedBytes, List<String> newNames) {
      frame = new ByteArrayOutputStream();
      writeDouble(frame, stamp);
      writeInt(frame, gcTimeUs);
      writeInt(frame, 0);
      writeInt(frame, usedBytes);
      writeInt(frame, reservedBytes);
      writeInt(frame, newNames.size());
      for (String name : newNames) {
        byte[] utf8 = name.getBytes(StandardCharsets.UTF_8);
        writeU16(frame, utf8.length);
        frame.writeBytes(utf8);
      }
    }

    private final ByteArrayOutputStream samples = new ByteArrayOutputStream();
    private int sampleInts;

    void sample(int[] nameIndexes, int deltaTicks) {
      writeInt(samples, nameIndexes.length);
      for (int index : nameIndexes) writeInt(samples, index);
      writeInt(samples, deltaTicks);
      sampleInts += nameIndexes.length + 2;
    }

    void endFrame() {
      writeInt(frame, sampleInts);
      frame.writeBytes(samples.toByteArray());
      samples.reset();
      sampleInts = 0;
      rawRecord(HxtSessionTranslator.FRAME_RECORD, frame.toByteArray());
      frame = null;
    }

    /** The extended record shape: the optional allocation counters trail the sample ints. */
    void endFrameWithAllocations(int allocatedBytes, int freedBytes) {
      writeInt(frame, sampleInts);
      frame.writeBytes(samples.toByteArray());
      samples.reset();
      sampleInts = 0;
      writeInt(frame, allocatedBytes);
      writeInt(frame, freedBytes);
      rawRecord(HxtSessionTranslator.FRAME_RECORD, frame.toByteArray());
      frame = null;
    }

    void rawRecord(int type, byte[] payload) {
      out.write(type);
      writeInt(out, payload.length);
      out.writeBytes(payload);
    }

    byte[] bytes() {
      return out.toByteArray();
    }

    InputStream stream() {
      return new ByteArrayInputStream(bytes());
    }

    private static void writeU16(ByteArrayOutputStream target, int value) {
      target.write(value & 0xFF);
      target.write(value >> 8 & 0xFF);
    }

    private static void writeInt(ByteArrayOutputStream target, int value) {
      for (int i = 0; i < 4; i++) target.write(value >> (8 * i) & 0xFF);
    }

    private static void writeDouble(ByteArrayOutputStream target, double value) {
      long bits = Double.doubleToLongBits(value);
      for (int i = 0; i < 8; i++) target.write((int)(bits >> (8 * i) & 0xFF));
    }
  }
}
