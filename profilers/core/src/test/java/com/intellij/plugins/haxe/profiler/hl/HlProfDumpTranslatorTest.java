package com.intellij.plugins.haxe.profiler.hl;

import com.intellij.plugins.haxe.profiler.model.ProfilerEvent;
import com.intellij.plugins.haxe.profiler.model.ProfilerFormatException;
import com.intellij.plugins.haxe.profiler.model.ProfilerSnapshot;
import com.intellij.plugins.haxe.profiler.model.ProfilerThread;
import com.intellij.plugins.haxe.profiler.model.StackFrame;
import com.intellij.plugins.haxe.profiler.model.StackSample;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Synthetic dumps written per hashlink's {@code src/profile.c}: header,
 * (time, thread, msgId) records — negative msgId = stack sample whose frames
 * are first-occurrence (fileId, line, len, UTF-16 description) and afterwards
 * back-references (fileId | bit31, line) — then the -1.0 sentinel and the
 * thread-name trailer.
 */
@DisplayName("HashLink profiler: PROF dump translator")
public class HlProfDumpTranslatorTest {

  private static final int MAIN_TID = 100;
  private static final int WORKER_TID = 200;

  @Test
  @DisplayName("reads samples symbols and source positions")
  public void testReadsSamplesSymbolsAndSourcePositions() throws IOException {
    DumpBuilder dump = new DumpBuilder(10000);
    dump.sampleStart(0.5, MAIN_TID, 2, false);
    dump.newFrame(0, 10, "Main.loop(src\\Main.hx:10)");
    dump.newFrame(0, 42, "Main.main(src\\Main.hx:42)");
    dump.sentinelAndNames();

    ProfilerSnapshot snapshot = translate(dump);

    assertEquals(HlProfDumpTranslator.TARGET, snapshot.target());
    assertEquals(10000, snapshot.samplesPerSecond());
    StackSample sample = snapshot.samples().getFirst();
    assertEquals(0.5, sample.time());
    assertEquals(MAIN_TID, sample.threadId());
    assertEquals(1, sample.weight());
    // dump order is leaf-first; the model is root-first
    assertEquals(List.of(new StackFrame("Main.main", "src/Main.hx", 42),
                         new StackFrame("Main.loop", "src/Main.hx", 10)),
                 sample.frames());
  }

  @Test
  @DisplayName("resolves back references against interned elements")
  public void testResolvesBackReferencesAgainstInternedElements() throws IOException {
    DumpBuilder dump = new DumpBuilder(10000);
    dump.sampleStart(0.1, MAIN_TID, 1, false);
    dump.newFrame(3, 7, "Game.update(Game.hx:7)");
    dump.sampleStart(0.2, MAIN_TID, 1, false);
    dump.backReference(3, 7);
    dump.sentinelAndNames();

    ProfilerSnapshot snapshot = translate(dump);

    assertEquals(2, snapshot.samples().size());
    StackFrame frame = new StackFrame("Game.update", "Game.hx", 7);
    assertEquals(List.of(frame), snapshot.samples().get(0).frames());
    assertEquals(List.of(frame), snapshot.samples().get(1).frames());
  }

  @Test
  @DisplayName("drops unresolvable frames and keeps the rest of the stack")
  public void testDropsUnresolvableFramesAndKeepsTheRestOfTheStack() throws IOException {
    DumpBuilder dump = new DumpBuilder(10000);
    dump.sampleStart(0.1, MAIN_TID, 3, false);
    dump.newFrame(0, 5, "Top.run(Top.hx:5)");
    dump.skipFrame();
    dump.newFrame(0, 9, "Root.start(Root.hx:9)");
    dump.sentinelAndNames();

    ProfilerSnapshot snapshot = translate(dump);

    assertEquals(List.of(new StackFrame("Root.start", "Root.hx", 9),
                         new StackFrame("Top.run", "Top.hx", 5)),
                 snapshot.samples().getFirst().frames());
  }

  @Test
  @DisplayName("gc major samples carry the flag")
  public void testGcMajorSamplesCarryTheFlag() throws IOException {
    DumpBuilder dump = new DumpBuilder(10000);
    dump.sampleStart(0.1, MAIN_TID, 1, true);
    dump.newFrame(0, 3, "Main.alloc(Main.hx:3)");
    dump.sampleStart(0.2, MAIN_TID, 1, false);
    dump.backReference(0, 3);
    dump.sentinelAndNames();

    ProfilerSnapshot snapshot = translate(dump);

    assertTrue(snapshot.samples().get(0).inGc(), "bit 30 of the sample msgId marks a major GC");
    assertEquals(List.of(new StackFrame("Main.alloc", "Main.hx", 3)), snapshot.samples().get(0).frames());
    assertFalse(snapshot.samples().get(1).inGc(), "an unflagged sample stays unflagged");
  }

  @Test
  @DisplayName("merges one symbol sampled at several lines onto its earliest line")
  public void testMergesOneSymbolSampledAtSeveralLinesOntoItsEarliestLine() throws IOException {
    // without the merge a method sampled across its body splits into one
    // tree node per line downstream; the earliest line wins retroactively
    DumpBuilder dump = new DumpBuilder(10000);
    dump.sampleStart(0.1, MAIN_TID, 1, false);
    dump.newFrame(0, 20, "Main.work(Main.hx:20)");
    dump.sampleStart(0.2, MAIN_TID, 1, false);
    dump.newFrame(0, 12, "Main.work(Main.hx:12)");
    dump.sentinelAndNames();

    ProfilerSnapshot snapshot = translate(dump);

    StackFrame merged = new StackFrame("Main.work", "Main.hx", 12);
    assertEquals(List.of(merged), snapshot.samples().get(0).frames());
    assertEquals(List.of(merged), snapshot.samples().get(1).frames());
  }

  @Test
  @DisplayName("undoes closure and constructor name mangling")
  public void testUndoesClosureAndConstructorNameMangling() throws IOException {
    DumpBuilder dump = new DumpBuilder(10000);
    dump.sampleStart(0.1, MAIN_TID, 2, false);
    dump.newFrame(0, 8, "Main.$update.closure(Main.hx:8)");
    dump.newFrame(0, 1, "pack.Thing.__constructor__(pack/Thing.hx:1)");
    dump.sentinelAndNames();

    ProfilerSnapshot snapshot = translate(dump);

    assertEquals(List.of(new StackFrame("pack.Thing.new", "pack/Thing.hx", 1),
                         new StackFrame("Main.update.closure", "Main.hx", 8)),
                 snapshot.samples().getFirst().frames());
  }

  @Test
  @DisplayName("keeps custom events and the end of frame marker")
  public void testKeepsCustomEventsAndTheEndOfFrameMarker() throws IOException {
    DumpBuilder dump = new DumpBuilder(10000);
    dump.event(0.1, MAIN_TID, 0, "");
    dump.event(0.2, MAIN_TID, 7, "level loaded");
    dump.sentinelAndNames();

    ProfilerSnapshot snapshot = translate(dump);

    assertEquals(List.of(new ProfilerEvent(0.1, MAIN_TID, 0, ""),
                         new ProfilerEvent(0.2, MAIN_TID, 7, "level loaded")),
                 snapshot.events());
  }

  @Test
  @DisplayName("names threads from the trailer with main as the first seen default")
  public void testNamesThreadsFromTheTrailerWithMainAsTheFirstSeenDefault() throws IOException {
    DumpBuilder dump = new DumpBuilder(10000);
    dump.sampleStart(0.1, MAIN_TID, 1, false);
    dump.newFrame(0, 1, "Main.main(Main.hx:1)");
    dump.sampleStart(0.2, WORKER_TID, 1, false);
    dump.backReference(0, 1);
    dump.sampleStart(0.3, 300, 1, false);
    dump.backReference(0, 1);
    dump.sentinel();
    dump.names(new int[]{WORKER_TID}, new String[]{"loader"});

    ProfilerSnapshot snapshot = translate(dump);

    assertEquals(List.of(new ProfilerThread(MAIN_TID, "Main"),
                         new ProfilerThread(WORKER_TID, "loader"),
                         new ProfilerThread(300, "Thread 300")),
                 snapshot.threads());
  }

  @Test
  @DisplayName("a dump truncated after the records still loads")
  public void testADumpTruncatedAfterTheRecordsStillLoads() throws IOException {
    DumpBuilder dump = new DumpBuilder(10000);
    dump.sampleStart(0.1, MAIN_TID, 1, false);
    dump.newFrame(0, 1, "Main.main(Main.hx:1)");
    // no sentinel, no trailer - the VM was interrupted between dumps

    ProfilerSnapshot snapshot = translate(dump);

    assertEquals(1, snapshot.samples().size());
    assertEquals(List.of(new ProfilerThread(MAIN_TID, "Main")), snapshot.threads());
  }

  @Test
  @DisplayName("a dump truncated inside a record is an error")
  public void testADumpTruncatedInsideARecordIsAnError() {
    DumpBuilder dump = new DumpBuilder(10000);
    dump.sampleStart(0.1, MAIN_TID, 2, false);
    dump.newFrame(0, 1, "Main.main(Main.hx:1)");
    // second frame missing entirely

    assertThrows(EOFException.class, () -> translate(dump));
  }

  @Test
  @DisplayName("a foreign file is rejected as not a dump")
  public void testAForeignFileIsRejectedAsNotADump() {
    byte[] foreign = "MZ some other binary".getBytes(StandardCharsets.US_ASCII);

    ProfilerFormatException rejection =
      assertThrows(ProfilerFormatException.class,
                   () -> HlProfDumpTranslator.translate(new ByteArrayInputStream(foreign)));
    assertTrue(rejection.getMessage().contains("PROF"), "the message names the missing magic: " + rejection.getMessage());
  }

  @Test
  @DisplayName("a back reference to an unknown element is an error")
  public void testABackReferenceToAnUnknownElementIsAnError() {
    DumpBuilder dump = new DumpBuilder(10000);
    dump.sampleStart(0.1, MAIN_TID, 1, false);
    dump.backReference(5, 99);

    assertThrows(ProfilerFormatException.class, () -> translate(dump));
  }

  private static ProfilerSnapshot translate(DumpBuilder dump) throws IOException {
    return HlProfDumpTranslator.translate(new ByteArrayInputStream(dump.bytes()));
  }

  /** Little-endian PROF writer mirroring profile.c's dump function. */
  private static final class DumpBuilder {
    private final ByteArrayOutputStream out = new ByteArrayOutputStream();

    DumpBuilder(int samplesPerSecond) {
      out.writeBytes("PROF".getBytes(StandardCharsets.US_ASCII));
      writeInt(115); // runtime version stamp
      writeInt(samplesPerSecond);
    }

    void sampleStart(double time, int threadId, int frameCount, boolean inGcMajor) {
      writeDouble(time);
      writeInt(threadId);
      writeInt(0x80000000 | (inGcMajor ? 0x40000000 : 0) | frameCount);
    }

    void newFrame(int fileId, int line, String description) {
      writeInt(fileId);
      writeInt(line);
      writeInt(description.length());
      out.writeBytes(description.getBytes(StandardCharsets.UTF_16LE));
    }

    void backReference(int fileId, int line) {
      writeInt(0x80000000 | fileId);
      writeInt(line);
    }

    void skipFrame() {
      writeInt(-1);
    }

    void event(double time, int threadId, int code, String data) {
      writeDouble(time);
      writeInt(threadId);
      writeInt(code);
      byte[] bytes = data.getBytes(StandardCharsets.UTF_16LE);
      writeInt(bytes.length);
      out.writeBytes(bytes);
    }

    void sentinel() {
      writeDouble(-1.0);
    }

    void names(int[] threadIds, String[] names) {
      writeInt(threadIds.length);
      for (int i = 0; i < threadIds.length; i++) {
        writeInt(threadIds[i]);
        byte[] bytes = names[i].getBytes(StandardCharsets.UTF_8);
        writeInt(bytes.length);
        out.writeBytes(bytes);
      }
    }

    void sentinelAndNames() {
      sentinel();
      names(new int[0], new String[0]);
    }

    byte[] bytes() {
      return out.toByteArray();
    }

    private void writeInt(int value) {
      out.write(value & 0xFF);
      out.write(value >> 8 & 0xFF);
      out.write(value >> 16 & 0xFF);
      out.write(value >> 24 & 0xFF);
    }

    private void writeDouble(double value) {
      long bits = Double.doubleToLongBits(value);
      writeInt((int)bits);
      writeInt((int)(bits >> 32));
    }
  }
}
