package com.intellij.plugins.haxe.profiler.hxt;

import com.intellij.plugins.haxe.profiler.model.ProfilerFormatException;
import com.intellij.plugins.haxe.profiler.model.TimelineEvent;
import com.intellij.plugins.haxe.profiler.tracy.*;
import com.intellij.plugins.haxe.profiler.tracy.wire.TracyProtocolVersion;
import com.intellij.plugins.haxe.profiler.tracy.wire.TracyWelcome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("hxcpp tracy: zone store")
public class HxtZoneStoreTest {

  private static final TracySourceLocation BURN = new TracySourceLocation("ProfPump.burn", "ProfPump.hx", 2, 0);
  private static final TracySourceLocation MAIN = new TracySourceLocation("ProfPump.main", "ProfPump.hx", 9, 0xFF00FF);

  @TempDir
  Path directory;

  /** The small series a capture accumulates beside the zones; times already rebased. */
  private static TracySession smallSession() {
    TracyWelcome welcome = new TracyWelcome(TracyProtocolVersion.V74, 0.25, 1, 2, 3, 4, 1_756_200_000L, 5, 4242, 0, false, "game.exe");
    return new TracySession(
      welcome,
      List.of(),
      List.of(16_000_000L, 33_000_000L),
      Map.of("frame time", List.of(new TracySession.PlotPoint(100, 16.5))),
      Map.of("Small Object Heap", List.of(new TracySession.PlotPoint(150, 4096.0))),
      List.of(new TracySession.GcSweep(250, 290, 2048, 17)),
      List.of(new TimelineEvent(1, 320, "level loaded", 0xFF9900)),
      List.of(new TracySession.PlotPoint(500, 12.5)),
      List.of(new TracySession.PlotPoint(0, 87.5)),
      Map.of(1, "Main", 2, "worker"),
      5_000_000,
      3);
  }

  @Test
  @DisplayName("a streamed capture reopens from disk with rebased zones and every small record")
  public void testAStreamedCaptureReopensFromDiskWithRebasedZonesAndEverySmallRecord() throws IOException {
    Path file = directory.resolve("session.hxtsession");
    try (OutputStream out = Files.newOutputStream(file)) {
      HxtZoneWriter writer = new HxtZoneWriter(out, 1_756_200_000.0);
      // raw times with a base of 1000, close order: burn closes inside main
      writer.zone(1, 1, 2_000, 900_000, BURN);
      writer.zone(1, 0, 1_000, 5_000_000, MAIN);
      writer.zone(2, 0, 1_400, 1_800, BURN);
      writer.finished(1_000);
      writer.finish(smallSession());
      assertEquals(3, writer.zoneCount());
    }

    HxtCapture capture = HxtSessionTranslator.translateCapture(file);
    HxtZoneStore store = assertInstanceOf(HxtCapture.Zones.class, capture).store();

    assertEquals(3, store.zoneCount());
    List<TracyZone> zones = new ArrayList<>();
    List<Integer> depths = new ArrayList<>();
    store.scanZones(-1, 0, Long.MAX_VALUE, 0, (thread, depth, startNs, endNs, location) -> {
      zones.add(new TracyZone(thread, startNs, endNs, location));
      depths.add(depth);
    });
    assertEquals(List.of(new TracyZone(1, 1_000, 899_000, BURN),
                         new TracyZone(1, 0, 4_999_000, MAIN),
                         new TracyZone(2, 400, 800, BURN)),
                 zones, "close order kept, times rebased to the base");
    assertEquals(List.of(1, 0, 0), depths);

    TracySession small = smallSession();
    TracySession reopened = store.session();
    assertEquals(small.frameMarksNs(), reopened.frameMarksNs());
    assertEquals(small.plots(), reopened.plots());
    assertEquals(small.memoryCurves(), reopened.memoryCurves());
    assertEquals(small.gcSweeps(), reopened.gcSweeps());
    assertEquals(small.events(), reopened.events());
    assertEquals(small.cpuUsage(), reopened.cpuUsage());
    assertEquals(small.processCpu(), reopened.processCpu());
    assertEquals(small.threadNames(), reopened.threadNames());
    assertEquals(small.durationNs(), reopened.durationNs());
    assertEquals(small.unmatchedZoneEnds(), reopened.unmatchedZoneEnds());
    assertEquals("game.exe", reopened.welcome().programName());
    assertEquals(4242, reopened.welcome().pid());
    assertEquals(HxtZoneWriter.FINAL_LEVEL, store.compressionLevel(), "the writer records its level in INFO");

    assertEquals(List.of(new HxtZoneStore.ThreadEntry(1, "Main", 2),
                         new HxtZoneStore.ThreadEntry(2, "worker", 1)),
                 store.threads(), "busiest first, zone counts from the writer");
  }

  @Test
  @DisplayName("scans filter by thread, window and minimum duration")
  public void testScansFilterByThreadWindowAndMinimumDuration() throws IOException {
    Path file = directory.resolve("filters.hxtsession");
    try (OutputStream out = Files.newOutputStream(file)) {
      HxtZoneWriter writer = new HxtZoneWriter(out, 0);
      writer.zone(1, 0, 100, 200, BURN);        // 100 ns, early
      writer.zone(1, 0, 5_000, 95_000, MAIN);   // 90 us, late
      writer.zone(2, 0, 5_500, 5_600, BURN);    // other thread
      writer.finished(100);
      writer.finish(emptySmallSession());
    }
    HxtZoneStore store = assertInstanceOf(HxtCapture.Zones.class, HxtSessionTranslator.translateCapture(file)).store();

    List<TracyZone> thread1Late = new ArrayList<>();
    store.scanZones(1, 1_000, Long.MAX_VALUE, 0,
                    (thread, depth, startNs, endNs, location) -> thread1Late.add(new TracyZone(thread, startNs, endNs, location)));
    assertEquals(List.of(new TracyZone(1, 4_900, 94_900, MAIN)), thread1Late);

    List<TracyZone> longOnes = new ArrayList<>();
    store.scanZones(-1, 0, Long.MAX_VALUE, 1_000,
                    (thread, depth, startNs, endNs, location) -> longOnes.add(new TracyZone(thread, startNs, endNs, location)));
    assertEquals(List.of(new TracyZone(1, 4_900, 94_900, MAIN)), longOnes, "the short zones fall under the duration floor");
  }

  @Test
  @DisplayName("a capture larger than one chunk splits and reopens whole")
  public void testACaptureLargerThanOneChunkSplitsAndReopensWhole() throws IOException {
    Path file = directory.resolve("chunks.hxtsession");
    int total = 70_000; // crosses the 65 536 zone chunk boundary
    try (OutputStream out = Files.newOutputStream(file)) {
      HxtZoneWriter writer = new HxtZoneWriter(out, 0);
      for (int i = 0; i < total; i++) {
        writer.zone(1, 0, i * 10L, i * 10L + 5, BURN);
      }
      writer.finished(0);
      writer.finish(emptySmallSession());
    }
    HxtZoneStore store = assertInstanceOf(HxtCapture.Zones.class, HxtSessionTranslator.translateCapture(file)).store();

    long[] seen = {0};
    store.scanZones(-1, 0, Long.MAX_VALUE, 0, (thread, depth, startNs, endNs, location) -> seen[0]++);
    assertEquals(total, seen[0]);
    assertEquals(total, store.zoneCount());
    long rawZoneBytes = (long)total * HxtZoneWriter.ZONE_BYTES;
    assertTrue(Files.size(file) < rawZoneBytes, "delta + LZ4 must beat the raw zone encoding");
  }

  /** A LIVE reader catches the file between chunk flushes: no finish(), no INFO — flushed chunks must still serve. */
  @Test
  @DisplayName("a growing capture without its finish records reads with fallbacks")
  public void testAGrowingCaptureWithoutItsFinishRecordsReadsWithFallbacks() throws IOException {
    Path file = directory.resolve("growing.hxtsession");
    int total = 70_000; // one full chunk on disk, the partial second one lost
    long baseOffsetNs = 5_000_000;
    try (OutputStream out = Files.newOutputStream(file)) {
      HxtZoneWriter writer = new HxtZoneWriter(out, 0, HxtZoneWriter.LIVE_LEVEL);
      for (int i = 0; i < total; i++) {
        writer.zone(1, 0, baseOffsetNs + i * 10L, baseOffsetNs + i * 10L + 5, BURN);
      }
      assertTrue(writer.flushedZones() > 0 && writer.flushedZones() < total,
                 "the scenario needs a flushed chunk AND an unflushed tail");
    }
    HxtZoneStore store = HxtZoneStore.open(file);

    assertEquals(1, store.threads().size(), "the incremental THREAD record lists the thread before finish()");
    assertEquals("Thread 1", store.threads().get(0).name(), "nameless until finish() overrides");

    long[] firstStartNs = {Long.MIN_VALUE};
    store.scanZones(-1, 0, Long.MAX_VALUE, 0, (thread, depth, startNs, endNs, location) -> {
      if (firstStartNs[0] == Long.MIN_VALUE) firstStartNs[0] = startNs;
    });
    assertEquals(0, firstStartNs[0], "without INFO the chunks' own minimum stands in as the rebase base");
    assertTrue(store.session().durationNs() > 0, "the chunk bounds stand in for the duration");
  }

  @Test
  @DisplayName("a live-level capture recompresses to the final level with identical content")
  public void testALiveLevelCaptureRecompressesToTheFinalLevelWithIdenticalContent() throws IOException {
    Path file = directory.resolve("recompress.hxtsession");
    int total = 70_000; // crosses the chunk boundary
    try (OutputStream out = Files.newOutputStream(file)) {
      HxtZoneWriter writer = new HxtZoneWriter(out, 0, HxtZoneWriter.LIVE_LEVEL);
      for (int i = 0; i < total; i++) {
        writer.zone(1, 0, i * 10L, i * 10L + 5, BURN);
      }
      writer.finished(0);
      writer.finish(smallSession());
    }
    long liveSize = Files.size(file);
    HxtZoneStore liveStore = assertInstanceOf(HxtCapture.Zones.class, HxtSessionTranslator.translateCapture(file)).store();
    assertEquals(HxtZoneWriter.LIVE_LEVEL, liveStore.compressionLevel());

    HxtZoneRecompressor.recompress(file, HxtZoneWriter.FINAL_LEVEL);

    assertTrue(Files.size(file) < liveSize, "the final level must pack tighter than the live level");
    HxtZoneStore store = assertInstanceOf(HxtCapture.Zones.class, HxtSessionTranslator.translateCapture(file)).store();
    long[] seen = {0};
    store.scanZones(-1, 0, Long.MAX_VALUE, 0, (thread, depth, startNs, endNs, location) -> seen[0]++);
    assertEquals(total, seen[0]);
    assertEquals(smallSession().memoryCurves(), store.session().memoryCurves(),
                 "non-zone records survive the rewrite verbatim");
    assertEquals(HxtZoneWriter.FINAL_LEVEL, store.compressionLevel(),
                 "the rewrite updates the stored level to match its chunks");
  }

  @Test
  @DisplayName("a file without the level byte reads as unknown and gains it on recompress")
  public void testAFileWithoutTheLevelByteReadsAsUnknownAndGainsItOnRecompress() throws IOException {
    Path file = directory.resolve("levelless.hxtsession");
    try (OutputStream out = Files.newOutputStream(file)) {
      HxtZoneWriter writer = new HxtZoneWriter(out, 0, HxtZoneWriter.LIVE_LEVEL);
      writer.zone(1, 0, 0, 10, BURN);
      writer.finished(0);
      writer.finish(emptySmallSession());
    }
    // strip the trailing level byte and shrink the INFO length field to
    // reproduce a capture written before the field existed; INFO is the
    // last record and emptySmallSession's one-char program name makes its
    // payload a fixed 40 bytes (u16+1 name, 36 fixed, 1 level)
    byte[] bytes = Files.readAllBytes(file);
    bytes[bytes.length - 40 - 4] = 39;
    Files.write(file, Arrays.copyOf(bytes, bytes.length - 1));
    HxtZoneStore levelless = assertInstanceOf(HxtCapture.Zones.class, HxtSessionTranslator.translateCapture(file)).store();
    assertEquals(-1, levelless.compressionLevel(), "no level byte reads as unknown");

    HxtZoneRecompressor.recompress(file, HxtZoneWriter.FINAL_LEVEL);

    HxtZoneStore store = assertInstanceOf(HxtCapture.Zones.class, HxtSessionTranslator.translateCapture(file)).store();
    assertEquals(HxtZoneWriter.FINAL_LEVEL, store.compressionLevel(), "the rewrite appends the level byte");
    assertEquals(1, store.zoneCount());
  }

  @Test
  @DisplayName("live series batches append across records and rebase with the final base")
  public void testLiveSeriesBatchesAppendAcrossRecordsAndRebaseWithTheFinalBase() throws IOException {
    Path file = directory.resolve("batches.hxtsession");
    // batch columns: frames, plots, cpu, processCpu, memory, gc, events, threadNames - raw times, base 1000
    TracyEventReader.SeriesBatch first = new TracyEventReader.SeriesBatch(
      List.of(17_000L),
      Map.of("frame time", List.of(new TracySession.PlotPoint(1_100, 16.5))),
      List.of(),
      List.of(),
      Map.of("Small Object Heap", List.of(new TracySession.PlotPoint(1_150, 4096.0))),
      List.of(),
      List.of(),
      Map.of(1, "Early"));
    TracyEventReader.SeriesBatch second = new TracyEventReader.SeriesBatch(
      List.of(33_000L),
      Map.of("frame time", List.of(new TracySession.PlotPoint(2_100, 17.5))),
      List.of(new TracySession.PlotPoint(1_500, 12.5)),
      List.of(),
      Map.of("Small Object Heap", List.of(new TracySession.PlotPoint(2_150, 5120.0))),
      List.of(new TracySession.GcSweep(1_250, 1_290, 2048, 17)),
      List.of(new TimelineEvent(1, 1_320, "level loaded", 0xFF9900)),
      Map.of());
    try (OutputStream out = Files.newOutputStream(file)) {
      HxtZoneWriter writer = new HxtZoneWriter(out, 0, HxtZoneWriter.LIVE_LEVEL);
      writer.zone(1, 0, 1_000, 2_000, BURN);
      writer.series(first);
      writer.series(second);
      writer.finished(1_000);
      writer.finish(emptySmallSession());
    }
    HxtZoneStore store = HxtZoneStore.open(file);

    TracySession reopened = store.session();
    assertEquals(List.of(16_000L, 32_000L), reopened.frameMarksNs());
    assertEquals(List.of(new TracySession.PlotPoint(100, 16.5), new TracySession.PlotPoint(1_100, 17.5)),
                 reopened.plots().get("frame time"), "increments of one series merge in order");
    assertEquals(List.of(new TracySession.PlotPoint(150, 4096.0), new TracySession.PlotPoint(1_150, 5120.0)),
                 reopened.memoryCurves().get("Small Object Heap"));
    assertEquals(List.of(new TracySession.PlotPoint(500, 12.5)), reopened.cpuUsage());
    assertEquals(List.of(new TracySession.GcSweep(250, 290, 2048, 17)), reopened.gcSweeps());
    assertEquals(List.of(new TimelineEvent(1, 320, "level loaded", 0xFF9900)), reopened.events());
    assertEquals("Main", store.threads().get(0).name(), "finish()'s named THREAD record overrides the batch's");
  }

  /** A LIVE reader catches the file when only batches are down: series stamps stand in for the missing base. */
  @Test
  @DisplayName("streamed series serve before any chunk with their own fallback base")
  public void testStreamedSeriesServeBeforeAnyChunkWithTheirOwnFallbackBase() throws IOException {
    Path file = directory.resolve("seriesonly.hxtsession");
    // batch columns: frames, plots, cpu, processCpu, memory, gc, events, threadNames - raw times
    TracyEventReader.SeriesBatch streamed = new TracyEventReader.SeriesBatch(
      List.of(5_000_000L, 5_016_000L),
      Map.of(),
      List.of(new TracySession.PlotPoint(5_001_000, 12.5)),
      List.of(),
      Map.of(),
      List.of(),
      List.of(),
      Map.of());
    try (OutputStream out = Files.newOutputStream(file)) {
      HxtZoneWriter writer = new HxtZoneWriter(out, 0, HxtZoneWriter.LIVE_LEVEL);
      writer.series(streamed);
      // no finish - the reader caught the capture mid-stream
    }
    HxtZoneStore store = HxtZoneStore.open(file);

    assertEquals(List.of(0L, 16_000L), store.session().frameMarksNs(),
                 "the series' own minimum stands in as the rebase base");
    assertEquals(List.of(new TracySession.PlotPoint(1_000, 12.5)), store.session().cpuUsage());
    assertEquals(16_000, store.session().durationNs(), "the series bounds stand in for the duration");
  }

  @Test
  @DisplayName("unknown record types are skipped by their length")
  public void testUnknownRecordTypesAreSkippedByTheirLength() throws IOException {
    Path file = directory.resolve("future.hxtsession");
    try (OutputStream out = Files.newOutputStream(file)) {
      HxtZoneWriter writer = new HxtZoneWriter(out, 0);
      writer.zone(1, 0, 0, 10, BURN);
      writer.finished(0);
      writer.finish(emptySmallSession());
      // a record from the future, spliced in at the end
      out.write(99);
      out.write(new byte[]{3, 0, 0, 0, 1, 2, 3});
    }

    HxtZoneStore store = assertInstanceOf(HxtCapture.Zones.class, HxtSessionTranslator.translateCapture(file)).store();
    assertEquals(1, store.zoneCount());
  }

  @Test
  @DisplayName("the sampled view refuses a zone capture with a clear message")
  public void testTheSampledViewRefusesAZoneCaptureWithAClearMessage() throws IOException {
    Path file = directory.resolve("refused.hxtsession");
    try (OutputStream out = Files.newOutputStream(file)) {
      HxtZoneWriter writer = new HxtZoneWriter(out, 0);
      writer.finished(0);
      writer.finish(emptySmallSession());
    }

    try (InputStream in = Files.newInputStream(file)) {
      ProfilerFormatException failure = assertThrows(ProfilerFormatException.class,
                                                     () -> HxtSessionTranslator.translate(in));
      assertTrue(failure.getMessage().contains("zone capture"), failure.getMessage());
    }
  }

  private static TracySession emptySmallSession() {
    TracyWelcome welcome = new TracyWelcome(TracyProtocolVersion.V74, 1.0, 0, 0, 0, 0, 0, 0, 1, 0, false, "x");
    return new TracySession(welcome, List.of(), List.of(), Map.of(), Map.of(),
                            List.of(), List.of(), List.of(), List.of(), Map.of(1, "Main"), 0, 0);
  }
}
