package com.intellij.plugins.haxe.profiler.flash;

import com.intellij.plugins.haxe.profiler.hxt.HxtSessionTranslator;
import com.intellij.plugins.haxe.profiler.model.ProfilerEvent;
import com.intellij.plugins.haxe.profiler.model.ProfilerSnapshot;
import com.intellij.plugins.haxe.profiler.model.StackFrame;
import com.intellij.plugins.haxe.profiler.model.StackSample;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * The fixture is a live AIR 26 telemetry capture of a small animating probe
 * (debug-launch adl, SamplerEnabled) — 3710 AMF3 messages over ~7 seconds:
 * 206 display frames with render spans, sampler stacks, memory and GC.
 */
@DisplayName("Flash profiler: telemetry transcoder")
public class FlashTelemetryTranscoderTest {

  @Test
  @DisplayName("a captured telemetry stream becomes a translatable v1 session")
  public void testACapturedTelemetryStreamBecomesATranslatableV1Session() throws IOException {
    ByteArrayOutputStream session = new ByteArrayOutputStream();
    long written = FlashTelemetryTranscoder.transcode(fixture(), session);
    ProfilerSnapshot snapshot = HxtSessionTranslator.translate(new ByteArrayInputStream(session.toByteArray()));

    assertEquals(session.size(), written, "the returned count is the bytes actually written");
    assertEquals("flash", snapshot.target());
    assertEquals(1_000_000, snapshot.samplesPerSecond(), "one tick per microsecond keeps segment weights exact");

    long frames = snapshot.events().stream().filter(e -> e.code() == ProfilerEvent.FRAME_CODE).count();
    assertTrue(frames >= 200, "one frame per .enter boundary; the capture holds 206 display frames, got " + frames);

    Set<String> symbols = symbolsOf(snapshot);
    assertTrue(symbols.contains("TelemetryProbe.onFrame"), "sampler stacks carry the app's own methods: " + symbols);
    assertTrue(symbols.contains("[render]"), "render spans become pseudo-frame segments");
    assertTrue(symbols.contains("[idle]"), "gaps between busy segments tile as idle");
    assertFalse(symbols.contains("(unknown)"),
                "name maps LAG the samples referencing them - windows wait for the batch, so every index resolves");
  }

  /**
   * The many-methods fixture (a 2000-method probe) is where the name-map
   * lag bites: ~65% of samples reference indices whose map message has not
   * arrived yet, and the table ships as hundreds of chunked messages.
   */
  @Test
  @DisplayName("a large lagging name table still resolves the stacks")
  public void testALargeLaggingNameTableStillResolvesTheStacks() throws IOException {
    ProfilerSnapshot snapshot = transcoded(fixture("/flash/telemetry-air26-manymethods.bin"));

    Set<String> symbols = symbolsOf(snapshot);
    assertTrue(symbols.contains("BigProbe.onFrame"), "the frame handler resolves: " + preview(symbols));
    assertTrue(symbols.contains("Gen31.methodNumber18"), "late-announced generated methods resolve");

    long unknownSamples = snapshot.samples().stream()
      .filter(sample -> sample.frames().stream().anyMatch(frame -> frame.symbol().equals("(unknown)")))
      .count();
    // the capture was cut mid-run, so the final name batch never shipped -
    // only those last-moment indices may stay unresolved
    assertTrue(unknownSamples <= snapshot.samples().size() / 50,
               unknownSamples + " of " + snapshot.samples().size() + " samples lack a name");
  }

  @Test
  @DisplayName("segment weights tile the session span without inflation")
  public void testSegmentWeightsTileTheSessionSpanWithoutInflation() throws IOException {
    ProfilerSnapshot snapshot = transcoded(fixture());

    List<StackSample> samples = snapshot.samples();
    boolean timesOrdered = true;
    for (int i = 1; i < samples.size(); i++) {
      timesOrdered &= samples.get(i).time() >= samples.get(i - 1).time();
    }
    assertTrue(timesOrdered, "reconstructed sample times never step backwards");

    long weightUs = samples.stream().mapToLong(StackSample::weight).sum();
    double lastStamp = snapshot.events().get(snapshot.events().size() - 1).time();
    long sessionUs = (long)(lastStamp * 1_000_000);
    assertTrue(weightUs > sessionUs * 0.9 && weightUs < sessionUs * 1.1,
               "segments tile the clock: " + weightUs + "us of weight across a " + sessionUs + "us session");
  }

  @Test
  @DisplayName("memory and GC arrive from the mem and gc message families")
  public void testMemoryAndGcArriveFromTheMemAndGcMessageFamilies() throws IOException {
    ProfilerSnapshot snapshot = transcoded(fixture());

    long memoryUsed = snapshot.memory().get(snapshot.memory().size() - 1).usedBytes();
    assertTrue(memoryUsed > 1_000_000, ".mem.managed.used kilobytes scale to bytes, got " + memoryUsed);

    boolean gcSeen = snapshot.events().stream().anyMatch(e -> e.code() == ProfilerEvent.GC_TIME_CODE);
    assertTrue(gcSeen, "the capture holds mark/sweep/reap spans");
  }

  @Test
  @DisplayName("a torn stream keeps the frames completed before the tear")
  public void testATornStreamKeepsTheFramesCompletedBeforeTheTear() throws IOException {
    byte[] whole = fixture().readAllBytes();
    InputStream torn = new ByteArrayInputStream(whole, 0, whole.length / 2);

    ProfilerSnapshot snapshot = transcoded(torn);

    long frames = snapshot.events().stream().filter(e -> e.code() == ProfilerEvent.FRAME_CODE).count();
    assertTrue(frames >= 50, "half the capture still yields its completed frames, got " + frames);
  }

  /** (raw telemetry method name, normalized symbol). */
  static final List<Arguments> METHOD_NAMES = List.of(
    arguments("TelemetryProbe$/onFrame", "TelemetryProbe.onFrame"),
    arguments("runtime::ContentPlayer/loadInitialContent", "runtime.ContentPlayer.loadInitialContent"),
    arguments("Array/http://adobe.com/AS3/2006/builtin::push", "Array.push"),
    arguments("global/runtime::ADLEntry", "global.runtime.ADLEntry"),
    arguments("flash.display::Sprite", "flash.display.Sprite"));

  @ParameterizedTest(name = "{0}")
  @FieldSource("METHOD_NAMES")
  @DisplayName("method names normalize to dotted symbols")
  public void testMethodNamesNormalizeToDottedSymbols(String raw, String normalized) {
    assertEquals(normalized, FlashTelemetryTranscoder.normalizeMethodName(raw));
  }

  private static ProfilerSnapshot transcoded(InputStream telemetry) throws IOException {
    ByteArrayOutputStream session = new ByteArrayOutputStream();
    FlashTelemetryTranscoder.transcode(telemetry, session);
    return HxtSessionTranslator.translate(new ByteArrayInputStream(session.toByteArray()));
  }

  private static Set<String> symbolsOf(ProfilerSnapshot snapshot) {
    return snapshot.samples().stream()
      .flatMap(sample -> sample.frames().stream())
      .map(StackFrame::symbol)
      .collect(Collectors.toSet());
  }

  private static String preview(Set<String> symbols) {
    return symbols.stream().limit(30).collect(Collectors.joining(", "));
  }

  private static InputStream fixture() {
    return fixture("/flash/telemetry-air26.bin");
  }

  private static InputStream fixture(String resource) {
    return FlashTelemetryTranscoderTest.class.getResourceAsStream(resource);
  }
}
