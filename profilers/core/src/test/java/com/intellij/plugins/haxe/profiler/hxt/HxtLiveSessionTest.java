package com.intellij.plugins.haxe.profiler.hxt;

import com.intellij.plugins.haxe.profiler.model.ProfilerSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("hxcpp telemetry: live session reader")
public class HxtLiveSessionTest {

  @TempDir
  Path directory;

  @Test
  @DisplayName("polls consume appended records and a partial tail waits for the next poll")
  public void testPollsConsumeAppendedRecordsAndAPartialTailWaitsForTheNextPoll() throws IOException {
    Path file = directory.resolve("live.hxtsession");
    Files.write(file, header(1000, 10.0));
    HxtLiveSession live = HxtLiveSession.open(file);
    assertFalse(live.poll(), "a header-only file has nothing to consume");

    byte[] firstFrame = frame(10.016, "Main.main", 3);
    Files.write(file, firstFrame, StandardOpenOption.APPEND);
    assertTrue(live.poll());
    assertEquals(1, live.snapshot().samples().size());

    // the second frame arrives torn: everything but its last 4 bytes
    byte[] secondFrame = frame(10.032, "Game.render", 2);
    Files.write(file, Arrays.copyOf(secondFrame, secondFrame.length - 4), StandardOpenOption.APPEND);
    assertFalse(live.poll(), "a partial record must wait, not desync");
    assertEquals(1, live.snapshot().samples().size());

    byte[] tail = new byte[4];
    System.arraycopy(secondFrame, secondFrame.length - 4, tail, 0, 4);
    Files.write(file, tail, StandardOpenOption.APPEND);
    assertTrue(live.poll());

    ProfilerSnapshot streamed = live.snapshot();
    ProfilerSnapshot whole = HxtSessionTranslator.translate(new ByteArrayInputStream(Files.readAllBytes(file)));
    assertEquals(whole.samples(), streamed.samples(), "incremental polls must equal one full translate");
    assertEquals(whole.memory(), streamed.memory());
    assertEquals(whole.events(), streamed.events());
  }

  // --- minimal v1 writers (one single-sample frame per record) ---

  private static byte[] header(int tickHz, double startStamp) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    out.writeBytes(new byte[]{'H', 'X', 'T', 'S'});
    writeU16(out, 1);
    writeInt(out, tickHz);
    writeDouble(out, startStamp);
    byte[] target = "hxcpp".getBytes(StandardCharsets.UTF_8);
    writeU16(out, target.length);
    out.writeBytes(target);
    return out.toByteArray();
  }

  private static byte[] frame(double stamp, String name, int deltaTicks) {
    ByteArrayOutputStream payload = new ByteArrayOutputStream();
    writeDouble(payload, stamp);
    writeInt(payload, 0); // gcTimeUs
    writeInt(payload, 0); // gcOverheadUs
    writeInt(payload, 4096);
    writeInt(payload, 0);
    writeInt(payload, 1); // one new name
    byte[] utf8 = name.getBytes(StandardCharsets.UTF_8);
    writeU16(payload, utf8.length);
    payload.writeBytes(utf8);
    writeInt(payload, 3); // one sample group: depth 1
    writeInt(payload, 1);
    writeInt(payload, nameIndex(name));
    writeInt(payload, deltaTicks);

    ByteArrayOutputStream record = new ByteArrayOutputStream();
    record.write(HxtSessionTranslator.FRAME_RECORD);
    writeInt(record, payload.size());
    record.writeBytes(payload.toByteArray());
    return record.toByteArray();
  }

  /** The accumulated table is 1-indexed; each test frame introduces exactly one new name in call order. */
  private static int nameIndex(String name) {
    return name.equals("Main.main") ? 1 : 2;
  }

  private static void writeU16(ByteArrayOutputStream out, int value) {
    out.write(value & 0xFF);
    out.write(value >> 8 & 0xFF);
  }

  private static void writeInt(ByteArrayOutputStream out, int value) {
    for (int i = 0; i < 4; i++) out.write(value >> (8 * i) & 0xFF);
  }

  private static void writeDouble(ByteArrayOutputStream out, double value) {
    long bits = Double.doubleToLongBits(value);
    for (int i = 0; i < 8; i++) out.write((int)(bits >> (8 * i) & 0xFF));
  }
}
