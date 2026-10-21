package com.intellij.plugins.haxe.profiler.tracy;

import com.intellij.plugins.haxe.profiler.model.ProfilerFormatException;
import com.intellij.plugins.haxe.profiler.model.TimelineEvent;
import com.intellij.plugins.haxe.profiler.tracy.wire.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("tracy receiver: event reader")
public class TracyEventReaderTest {

  private static final TracyWelcome TICKS_ARE_NS = ticksAreNs(TracyProtocolVersion.V74);
  private static final TracyWelcome TICKS_ARE_NS_V82 = ticksAreNs(TracyProtocolVersion.V82);
  private static final TracyEventReader.Hooks NO_HOOKS = new TracyEventReader.Hooks() {
  };

  @Test
  @DisplayName("replays the live captured session into nested zones")
  public void testReplaysTheLiveCapturedSessionIntoNestedZones() throws IOException {
    TracyWelcome welcome;
    try (InputStream in = resource("/tracy/welcome-v74.bin")) {
      welcome = TracyProtocolVersion.V74.format().parseWelcome(in.readAllBytes(), TracyProtocolVersion.V74);
    }
    TracySession session;
    try (InputStream raw = resource("/tracy/session-v74.raw")) {
      session = TracyEventReader.read(new TracyLz4Stream(raw), welcome);
    }

    assertEquals(791, session.zones().size(), "787 closed + 4 auto-closed at the capture cut");
    assertEquals(0, session.unmatchedZoneEnds());
    assertTrue(session.zones().stream().allMatch(zone -> zone.endNs() >= zone.startNs()));
    assertTrue(session.zones().stream().anyMatch(zone -> zone.location().function().contains("ProfPump")),
               "the sample's own functions must appear");
    assertTrue(session.zones().stream().anyMatch(zone -> zone.location().file().endsWith(".hx")),
               "haxe positions ride along");
    // wall clock was ~2 s but the INSTRUMENTED span is the burn workload (~70 ms)
    assertTrue(session.durationNs() > 10_000_000L && session.durationNs() < 60_000_000_000L,
               "the sample's instrumented span: " + session.durationNs() + " ns");
    assertEquals(1, session.cpuUsage().size(), "the one SysTimeReport in the capture");

    TracyZone outer = session.zones().stream()
      .max(Comparator.comparingLong(TracyZone::durationNs))
      .orElseThrow();
    boolean nests = session.zones().stream()
      .anyMatch(zone -> zone != outer && zone.startNs() >= outer.startNs() && zone.endNs() <= outer.endNs());
    assertTrue(nests, "instrumented calls must nest inside their caller's zone");
  }

  @Test
  @DisplayName("a thread context switch resets the time reference")
  public void testAThreadContextSwitchResetsTheTimeReference() throws IOException {
    ItemBuilder items = new ItemBuilder();
    items.threadContext(1);
    items.sourceLocation("Main.main", "Main.hx", 10);
    items.zoneBeginAlloc(100);
    items.zoneEnd(50);
    items.threadContext(2);
    items.sourceLocation("Worker.run", "Worker.hx", 20);
    items.zoneBeginAlloc(10); // a RESET reference: 10, not 160
    items.zoneEnd(5);

    TracySession session = TracyEventReader.read(items.stream(), TICKS_ARE_NS);

    assertEquals(2, session.zones().size());
    TracyZone worker = session.zones().get(0);
    assertEquals(2, worker.threadId());
    assertEquals(0, worker.startNs(), "session times rebase to the earliest instant");
    assertEquals(5, worker.endNs());
    TracyZone main = session.zones().get(1);
    assertEquals(90, main.startNs());
    assertEquals(140, main.endNs());
  }

  @Test
  @DisplayName("frame marks are absolute while zones stay on the delta stream")
  public void testFrameMarksAreAbsoluteWhileZonesStayOnTheDeltaStream() throws IOException {
    ItemBuilder items = new ItemBuilder();
    items.threadContext(1);
    items.sourceLocation("Main.main", "Main.hx", 1);
    items.zoneBeginAlloc(1000);
    items.frameMark(1200);
    items.zoneEnd(500); // delta from the zone begin, unaffected by the mark

    TracySession session = TracyEventReader.read(items.stream(), TICKS_ARE_NS);

    assertEquals(List.of(200L), session.frameMarksNs());
    assertEquals(500, session.zones().getFirst().endNs());
  }

  @Test
  @DisplayName("unmatched ends are counted and unclosed zones close at the last instant")
  public void testUnmatchedEndsAreCountedAndUnclosedZonesCloseAtTheLastInstant() throws IOException {
    ItemBuilder items = new ItemBuilder();
    items.threadContext(1);
    items.zoneEnd(50); // connection opened mid-zone
    items.sourceLocation("Main.main", "Main.hx", 1);
    items.zoneBeginAlloc(50);
    items.frameMark(400); // moves the last-seen instant past the open zone

    TracySession session = TracyEventReader.read(items.stream(), TICKS_ARE_NS);

    assertEquals(1, session.unmatchedZoneEnds());
    assertEquals(1, session.zones().size());
    assertEquals(350, session.zones().getFirst().endNs(), "closed at the last instant seen");
  }

  @Test
  @DisplayName("a zone begin without its source location payload fails")
  public void testAZoneBeginWithoutItsSourceLocationPayloadFails() {
    ItemBuilder items = new ItemBuilder();
    items.threadContext(1);
    items.zoneBeginAlloc(100);

    assertThrows(ProfilerFormatException.class, () -> TracyEventReader.read(items.stream(), TICKS_ARE_NS));
  }

  @Test
  @DisplayName("plots ride the thread delta stream keyed by their name pointer")
  public void testPlotsRideTheThreadDeltaStreamKeyedByTheirNamePointer() throws IOException {
    ItemBuilder items = new ItemBuilder();
    items.threadContext(1);
    items.plotDouble(0xCAFE, 100, 42.5);
    items.plotDouble(0xCAFE, 50, 43.5);

    TracySession session = TracyEventReader.read(items.stream(), TICKS_ARE_NS);

    List<TracySession.PlotPoint> points = session.plots().get("plot@cafe");
    assertEquals(2, points.size());
    assertEquals(0, points.get(0).timeNs());
    assertEquals(42.5, points.get(0).value());
    assertEquals(50, points.get(1).timeNs(), "the second point is 50 ticks after the first");
  }

  @Test
  @DisplayName("named memory pools accumulate live bytes curves on the serial stream")
  public void testNamedMemoryPoolsAccumulateLiveBytesCurvesOnTheSerialStream() throws IOException {
    ItemBuilder items = new ItemBuilder();
    items.threadContext(1);
    items.memName(0xBEEF);
    items.memAlloc(100, 0x1000, 4096);
    items.memName(0xBEEF);
    items.memAlloc(200_000, 0x2000, 1024);
    items.memName(0xBEEF);
    items.memFree(100_000, 0x1000);
    items.stringData(0xBEEF, "Small Object Heap");

    TracySession session = TracyEventReader.read(items.stream(), TICKS_ARE_NS);

    List<TracySession.PlotPoint> curve = session.memoryCurves().get("Small Object Heap");
    assertEquals(List.of(new TracySession.PlotPoint(0, 4096.0),
                         new TracySession.PlotPoint(200_000, 5120.0),
                         new TracySession.PlotPoint(300_000, 1024.0)),
                 curve, "alloc, alloc, free - rebased to the first event");
  }

  @Test
  @DisplayName("messages become timeline events with absolute times and their preceding text")
  public void testMessagesBecomeTimelineEventsWithAbsoluteTimesAndTheirPrecedingText() throws IOException {
    ItemBuilder items = new ItemBuilder();
    items.threadContext(1);
    items.sourceLocation("Main.main", "Main.hx", 1);
    items.zoneBeginAlloc(1000);
    items.singleString("level loaded");
    items.messageColor(1200, 0xFF, 0x99, 0x00); // absolute ticks, like frame marks
    items.zoneEnd(500); // delta from the zone begin, unaffected by the message

    TracySession session = TracyEventReader.read(items.stream(), TICKS_ARE_NS);

    assertEquals(List.of(new TimelineEvent(1, 200, "level loaded", 0xFF9900)), session.events());
    assertEquals(500, session.zones().getFirst().endNs(), "the zone delta stream ignores the message");
  }

  @Test
  @DisplayName("a sink receives the small series as raw batches and the session stays lean")
  public void testASinkReceivesTheSmallSeriesAsRawBatchesAndTheSessionStaysLean() throws IOException {
    ItemBuilder items = new ItemBuilder();
    items.threadContext(1);
    items.sourceLocation("Main.main", "Main.hx", 1);
    items.zoneBeginAlloc(1_000);
    items.frameMark(1_200);
    items.singleString("level loaded");
    items.messageColor(1_300, 0xFF, 0x99, 0x00);
    items.memName(0xBEEF);
    items.memAlloc(100, 0x1000, 4096);
    items.stringData(0xBEEF, "Small Object Heap");
    items.plotDouble(0xCAFE, 50, 42.5); // its name never answers - the final batch labels it by pointer
    items.zoneEnd(500);
    List<TracyEventReader.SeriesBatch> batches = new ArrayList<>();
    TracyEventReader.ZoneSink sink = new TracyEventReader.ZoneSink() {
      @Override
      public void zone(int threadId, int depth, long startNs, long endNs, TracySourceLocation location) {
      }

      @Override
      public void series(TracyEventReader.SeriesBatch batch) {
        batches.add(batch);
      }
    };

    TracySession session = TracyEventReader.read(items.stream(), TICKS_ARE_NS, NO_HOOKS, sink);

    // too few items to trip the timed flush - everything arrives in the final tail batch
    assertEquals(1, batches.size());
    TracyEventReader.SeriesBatch tail = batches.get(0);
    assertEquals(List.of(1_200L), tail.frameMarksNs(), "raw stamps, no rebase");
    assertEquals(List.of(new TracySession.PlotPoint(100, 4096.0)), tail.memoryCurves().get("Small Object Heap"));
    assertEquals(List.of(new TracySession.PlotPoint(1_050, 42.5)), tail.plots().get("plot@cafe"),
                 "an unanswered name falls back to its pointer label");
    assertEquals(List.of(new TimelineEvent(1, 1_300, "level loaded", 0xFF9900)), tail.events());

    boolean seriesLeftInSession = !session.frameMarksNs().isEmpty() || !session.plots().isEmpty()
                                  || !session.memoryCurves().isEmpty() || !session.events().isEmpty();
    assertFalse(seriesLeftInSession, "the series went to the sink - the session mirrors the zones contract");
  }

  @Test
  @DisplayName("a torn stream salvages everything decoded before the tear")
  public void testATornStreamSalvagesEverythingDecodedBeforeTheTear() throws IOException {
    ItemBuilder items = new ItemBuilder();
    items.threadContext(1);
    items.sourceLocation("Main.main", "Main.hx", 10);
    items.zoneBeginAlloc(100);
    items.zoneEnd(50);
    items.sourceLocation("Main.helper", "Main.hx", 20);
    items.zoneBeginAlloc(10);
    byte[] whole = items.bytes();
    // the tear lands inside the final zone begin's time field
    InputStream torn = new ByteArrayInputStream(whole, 0, whole.length - 4);

    TracySession session = TracyEventReader.readSalvaging(torn, TICKS_ARE_NS, NO_HOOKS, null);

    assertEquals(1, session.zones().size(), "the closed zone before the tear survives");
    assertEquals(0, session.zones().getFirst().startNs());
    assertEquals(50, session.zones().getFirst().endNs());
  }

  @Test
  @DisplayName("the strict replay forms still fail on a torn stream")
  public void testTheStrictReplayFormsStillFailOnATornStream() {
    ItemBuilder items = new ItemBuilder();
    items.threadContext(1);
    items.sourceLocation("Main.main", "Main.hx", 10);
    items.zoneBeginAlloc(100);
    byte[] whole = items.bytes();
    InputStream torn = new ByteArrayInputStream(whole, 0, whole.length - 4);

    assertThrows(IOException.class, () -> TracyEventReader.read(torn, TICKS_ARE_NS));
  }

  @Test
  @DisplayName("own context-switch intervals fold into the process CPU curve")
  public void testOwnContextSwitchIntervalsFoldIntoTheProcessCpuCurve() throws IOException {
    ItemBuilder items = new ItemBuilder();
    items.tidToPid(7, 1); // the welcome's pid is 1 - thread 7 is ours
    items.contextSwitch(1_000, 0, 7, 0);
    items.threadWakeup(1_000_000, 42); // advances the shared ctx reference
    items.callstackSample(1_000_000, 7); // so does a sampled callstack
    items.contextSwitch(23_000_000, 7, 99, 0); // out 25 ms after the in

    TracySession session = TracyEventReader.read(items.stream(), TICKS_ARE_NS);

    assertEquals(List.of(new TracySession.PlotPoint(0, 25.0)), session.processCpu(),
                 "25 ms of the 100 ms bucket; wakeup and sample deltas count into the out time");
  }

  @Test
  @DisplayName("an interval crossing the bucket edge splits between the buckets")
  public void testAnIntervalCrossingTheBucketEdgeSplitsBetweenTheBuckets() throws IOException {
    ItemBuilder items = new ItemBuilder();
    items.tidToPid(7, 1);
    items.contextSwitch(90_000_000, 0, 7, 0);
    items.contextSwitch(30_000_000, 7, 0, 0); // out at 120 ms - 10 ms in the first bucket, 20 in the second

    TracySession session = TracyEventReader.read(items.stream(), TICKS_ARE_NS);

    assertEquals(List.of(new TracySession.PlotPoint(0, 10.0),
                         new TracySession.PlotPoint(10_000_000, 20.0)),
                 session.processCpu(), "bucket starts rebased to the first busy edge");
  }

  @Test
  @DisplayName("only closed intervals of own threads count toward process CPU")
  public void testOnlyClosedIntervalsOfOwnThreadsCountTowardProcessCpu() throws IOException {
    ItemBuilder items = new ItemBuilder();
    items.tidToPid(7, 1);
    items.contextSwitch(1_000, 0, 42, 0); // another process's thread
    items.contextSwitch(10_000_000, 42, 7, 1); // ours schedules in but never out
    items.contextSwitch(5_000_000, 55, 66, 0); // closes the foreign interval on core 0

    TracySession session = TracyEventReader.read(items.stream(), TICKS_ARE_NS);

    assertEquals(List.of(), session.processCpu());
  }

  @Test
  @DisplayName("a free burst between allocs becomes one GC sweep with its reclaim")
  public void testAFreeBurstBetweenAllocsBecomesOneGcSweepWithItsReclaim() throws IOException {
    ItemBuilder items = new ItemBuilder();
    items.threadContext(1);
    for (int i = 0; i < 5; i++) {
      items.memName(0xBEEF);
      items.memAlloc(100, 0x1000 + i, 256);
    }
    for (int i = 0; i < 4; i++) {
      items.memName(0xBEEF);
      items.memFree(50, 0x1000 + i);
    }
    items.memName(0xBEEF);
    items.memAlloc(100, 0x2000, 64); // the mutator resumes - the sweep is over
    items.memName(0xBEEF);
    items.memFree(10, 0x2000); // a lone free stays below the sweep threshold

    TracySession session = TracyEventReader.read(items.stream(), TICKS_ARE_NS);

    // allocs at 100..500, frees at 550/600/650/700, all rebased to the first alloc
    assertEquals(List.of(new TracySession.GcSweep(450, 600, 4 * 256, 4)), session.gcSweeps());
  }

  @Test
  @DisplayName("same bucket samples coalesce and unknown frees are ignored")
  public void testSameBucketSamplesCoalesceAndUnknownFreesAreIgnored() throws IOException {
    ItemBuilder items = new ItemBuilder();
    items.threadContext(1);
    items.memName(0xBEEF);
    items.memAlloc(100, 0x1000, 100);
    items.memName(0xBEEF);
    items.memAlloc(10, 0x2000, 50); // same 65 us bucket - replaces the previous point
    items.memName(0xBEEF);
    items.memFree(10, 0x9999); // allocated before the capture attached - no effect

    TracySession session = TracyEventReader.read(items.stream(), TICKS_ARE_NS);

    List<TracySession.PlotPoint> curve = session.memoryCurves().get("pool@beef");
    assertEquals(List.of(new TracySession.PlotPoint(10, 150.0)), curve);
  }

  @Test
  @DisplayName("replays the live captured v76 session from hxcpp master")
  public void testReplaysTheLiveCapturedV76SessionFromHxcppMaster() throws IOException {
    TracySession session = replayFixture(TracyProtocolVersion.V76);

    assertEquals(TracyProtocolVersion.V76, session.welcome().protocolVersion());
    assertEquals("Main.exe", session.welcome().programName());
    // the sample nests frame -> branch -> 5 leaves: 300 frames of 7 zones, plus main and its startup
    assertEquals(300 * 7 + 4, session.zones().size());
    assertEquals(0, session.unmatchedZoneEnds());
    assertEquals(300, session.frameMarksNs().size());
    assertTrue(session.zones().stream().anyMatch(zone -> zone.location().function().equals("Main.leaf")),
               "hxcpp's alloc-srcloc zones name the haxe function");
    assertTrue(session.events().size() >= 3, "one message per hundred frames: " + session.events());
  }

  @Test
  @DisplayName("replays the recorded v69 session of the 0.11 client")
  public void testReplaysTheRecordedV69SessionOfThe011Client() throws IOException {
    TracySession session = replayFixture(TracyProtocolVersion.V69);

    assertEquals(TracyProtocolVersion.V69, session.welcome().protocolVersion());
    // the fixture program nests frame -> branch -> 5 leaves, 300 frames of 7 zones
    assertEquals(300 * 7, session.zones().size());
    assertEquals(0, session.unmatchedZoneEnds());
    assertEquals(300, session.frameMarksNs().size());
    assertEquals(3, session.events().size(), "one message per hundred frames");
    boolean plotPresent = session.plots().containsKey("sink")
                          || session.plots().keySet().stream().anyMatch(name -> name.startsWith("plot@"));
    assertTrue(plotPresent, "the plot rides along: " + session.plots().keySet());
    assertEquals(1, session.memoryCurves().size(), "the named pool's live-bytes curve");
  }

  @Test
  @DisplayName("replays the recorded v82 session of the 0.14 client")
  public void testReplaysTheRecordedV82SessionOfThe014Client() throws IOException {
    TracySession session = replayFixture(TracyProtocolVersion.V82);

    assertEquals(TracyProtocolVersion.V82, session.welcome().protocolVersion());
    // the same fixture program as v69: 300 frames of 7 alloc-srcloc zones, the ends arriving packed
    assertEquals(300 * 7, session.zones().size());
    assertEquals(0, session.unmatchedZoneEnds());
    assertTrue(session.zones().stream().allMatch(zone -> zone.endNs() >= zone.startNs()));
    assertEquals(300, session.frameMarksNs().size());
    // the client's own status message rides the same item with a non-program source and is dropped
    assertEquals(3, session.events().size(), "the program's messages, with their metadata byte and 8-bit strings");
    assertEquals("frame 0", session.events().get(0).text());
    assertEquals(1, session.memoryCurves().size());
  }

  @Test
  @DisplayName("v82 packs zone ends into 16 and 32 bit deltas with offsets")
  public void testV82PacksZoneEndsInto16And32BitDeltasWithOffsets() throws IOException {
    ItemBuilder items = new ItemBuilder(TracyProtocolVersion.V82);
    items.threadContext(1);
    items.sourceLocation("Main.a", "Main.hx", 1);
    items.zoneBeginAlloc(100); // alloc-srcloc begins stay plain 64-bit deltas
    items.zoneEnd16(50);
    items.sourceLocation("Main.b", "Main.hx", 2);
    items.zoneBeginAlloc(10);
    items.zoneEnd32(70_000); // 32-bit item: stored minus 2^16
    items.sourceLocation("Main.c", "Main.hx", 3);
    items.zoneBeginAlloc(10);
    items.zoneEnd64(5_000_000_000L); // 64-bit item: stored minus (2^16 + 2^32)
    items.sourceLocation("Main.d", "Main.hx", 4);
    items.zoneBeginAlloc(10);
    items.zoneEnd64(-5); // a negative delta is sent as-is

    TracySession session = TracyEventReader.read(items.stream(), TICKS_ARE_NS_V82);

    assertEquals(4, session.zones().size());
    assertEquals(0, session.unmatchedZoneEnds());
    assertEquals(50, session.zones().get(0).durationNs());
    assertEquals(70_000, session.zones().get(1).durationNs());
    assertEquals(5_000_000_000L, session.zones().get(2).durationNs());
    assertEquals(-5, session.zones().get(3).durationNs());
  }

  @Test
  @DisplayName("v82 static zone begins arrive packed too")
  public void testV82StaticZoneBeginsArrivePackedToo() throws IOException {
    ItemBuilder items = new ItemBuilder(TracyProtocolVersion.V82);
    items.threadContext(1);
    items.zoneBeginStatic16(100, 0xABC);
    items.zoneEnd16(10);
    items.zoneBeginStatic32(70_000, 0xABC);
    items.zoneEnd16(10);
    items.zoneBeginStatic64(5_000_000_000L, 0xABC);
    items.zoneEnd16(10);

    TracySession session = TracyEventReader.read(items.stream(), TICKS_ARE_NS_V82);

    // starts at 100, 70110 and 5000070120 ticks, rebased to the first
    assertEquals(List.of(0L, 70_010L, 5_000_070_020L), session.zones().stream().map(TracyZone::startNs).toList());
  }

  @Test
  @DisplayName("v82 messages carry a metadata byte and short strings an 8 bit length")
  public void testV82MessagesCarryAMetadataByteAndShortStringsAn8BitLength() throws IOException {
    ItemBuilder items = new ItemBuilder(TracyProtocolVersion.V82);
    items.threadContext(1);
    items.singleString8("hi");
    items.messageColor82(1200, 0xFF, 0x99, 0x00);
    items.singleString16Offset("x".repeat(300)); // u16 length stored minus 256
    items.message82(1300);

    TracySession session = TracyEventReader.read(items.stream(), TICKS_ARE_NS_V82);

    assertEquals(2, session.events().size());
    assertEquals("hi", session.events().get(0).text());
    assertEquals(0xFF9900, session.events().get(0).color());
    assertEquals(300, session.events().get(1).text().length());
    assertEquals(100, session.events().get(1).timeNs() - session.events().get(0).timeNs());
  }

  @Test
  @DisplayName("v82 callstack samples put the thread first and pack their delta")
  public void testV82CallstackSamplesPutTheThreadFirstAndPackTheirDelta() throws IOException {
    ItemBuilder items = new ItemBuilder(TracyProtocolVersion.V82);
    items.frameMark(0); // pins the session's zero
    items.tidToPid(1, 1);
    items.callstackSample16(1000, 1);
    items.callstackSample32(70_000, 1);
    items.contextSwitch(5, 0, 1, 0); // own thread 1 lands on core 0 at 1000 + 70000 + 5
    items.contextSwitch(200_000_000, 1, 2, 0);

    TracySession session = TracyEventReader.read(items.stream(), TICKS_ARE_NS_V82);

    // the first 100 ms bucket is busy from 71005 on; had the samples not advanced the reference it would start at 5
    double firstBucketPercent = session.processCpu().getFirst().value();
    assertEquals((100_000_000 - 71_005) / 1_000_000.0, firstBucketPercent, 1e-6, "the packed sample deltas advanced the ctx reference");
  }

  private static TracySession replayFixture(TracyProtocolVersion version) throws IOException {
    TracyWelcome welcome;
    try (InputStream in = resource("/tracy/welcome-v" + version.wire() + ".bin")) {
      welcome = version.format().parseWelcome(in.readAllBytes(), version);
    }
    try (InputStream raw = resource("/tracy/session-v" + version.wire() + ".raw")) {
      return TracyEventReader.read(new TracyLz4Stream(raw), welcome);
    }
  }

  private static TracyWelcome ticksAreNs(TracyProtocolVersion version) {
    return new TracyWelcome(version, 1.0, 0, 0, 0, 0, 0, 0, 1, 0, false, "synthetic");
  }

  private static InputStream resource(String name) {
    return TracyEventReaderTest.class.getResourceAsStream(name);
  }

  /** Writes items in one protocol version's numbering (v74 unless given). */
  private static final class ItemBuilder {
    /** v82's packed-delta offsets: what the 32 and 64-bit items store their delta minus. */
    private static final long OFFSET_16BIT = 1L << 16;
    private static final long OFFSET_32BIT = (1L << 16) + (1L << 32);

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final TracyQueueTable table;

    ItemBuilder() {
      this(TracyProtocolVersion.V74);
    }

    ItemBuilder(TracyProtocolVersion version) {
      this.table = version.table();
    }

    private void type(TracyQueueType type) {
      int ordinal = table.ordinalOf(type);
      if (ordinal < 0) throw new IllegalArgumentException(type + " is not in this protocol version");
      out.write(ordinal);
    }

    void threadContext(int thread) {
      type(TracyQueueType.ThreadContext);
      writeInt(thread);
    }

    void sourceLocation(String function, String file, int line) {
      type(TracyQueueType.SourceLocationPayload);
      writeLong(0xDEAD); // the client-side pointer; unused by the reader
      byte[] functionUtf8 = function.getBytes(StandardCharsets.UTF_8);
      byte[] fileUtf8 = file.getBytes(StandardCharsets.UTF_8);
      writeU16(4 + 4 + functionUtf8.length + 1 + fileUtf8.length + 1);
      writeInt(0); // color
      writeInt(line);
      out.writeBytes(functionUtf8);
      out.write(0);
      out.writeBytes(fileUtf8);
      out.write(0);
    }

    void zoneBeginAlloc(long deltaTicks) {
      type(TracyQueueType.ZoneBeginAllocSrcLoc);
      writeLong(deltaTicks);
    }

    void zoneEnd(long deltaTicks) {
      type(TracyQueueType.ZoneEnd);
      writeLong(deltaTicks);
    }

    void frameMark(long absoluteTicks) {
      type(TracyQueueType.FrameMarkMsg);
      writeLong(absoluteTicks);
      writeLong(0); // name pointer; 0 = the continuous frame set
    }

    void plotDouble(long namePointer, long deltaTicks, double value) {
      type(TracyQueueType.PlotDataDouble);
      writeLong(namePointer);
      writeLong(deltaTicks);
      writeLong(Double.doubleToLongBits(value));
    }

    void memName(long namePointer) {
      type(TracyQueueType.MemNamePayload);
      writeLong(namePointer);
    }

    void memAlloc(long deltaTicks, long pointer, long size) {
      type(TracyQueueType.MemAllocNamed);
      writeLong(deltaTicks);
      writeInt(1); // owning thread
      writeLong(pointer);
      for (int i = 0; i < 6; i++) out.write((int)(size >> (8 * i) & 0xFF));
    }

    void memFree(long deltaTicks, long pointer) {
      type(TracyQueueType.MemFreeNamed);
      writeLong(deltaTicks);
      writeInt(1); // owning thread
      writeLong(pointer);
    }

    void singleString(String text) {
      type(TracyQueueType.SingleStringData);
      byte[] utf8 = text.getBytes(StandardCharsets.UTF_8);
      writeU16(utf8.length);
      out.writeBytes(utf8);
    }

    void messageColor(long absoluteTicks, int r, int g, int b) {
      type(TracyQueueType.MessageColor);
      writeLong(absoluteTicks);
      out.write(b);
      out.write(g);
      out.write(r);
    }

    void zoneEnd16(int deltaTicks) {
      type(TracyQueueType.ZoneEnd16);
      writeU16(deltaTicks);
    }

    void zoneEnd32(long deltaTicks) {
      type(TracyQueueType.ZoneEnd32);
      writeInt((int)(deltaTicks - OFFSET_16BIT));
    }

    /** v82's 64-bit end: a non-negative delta is stored minus (2^16 + 2^32). */
    void zoneEnd64(long deltaTicks) {
      type(TracyQueueType.ZoneEnd);
      writeLong(deltaTicks >= 0 ? deltaTicks - OFFSET_32BIT : deltaTicks);
    }

    void zoneBeginStatic16(int deltaTicks, long srcloc) {
      type(TracyQueueType.ZoneBegin16);
      writeU16(deltaTicks);
      writeLong(srcloc);
    }

    void zoneBeginStatic32(long deltaTicks, long srcloc) {
      type(TracyQueueType.ZoneBegin32);
      writeInt((int)(deltaTicks - OFFSET_16BIT));
      writeLong(srcloc);
    }

    void zoneBeginStatic64(long deltaTicks, long srcloc) {
      type(TracyQueueType.ZoneBegin);
      writeLong(deltaTicks >= 0 ? deltaTicks - OFFSET_32BIT : deltaTicks);
      writeLong(srcloc);
    }

    void singleString8(String text) {
      type(TracyQueueType.SingleStringData8);
      byte[] utf8 = text.getBytes(StandardCharsets.UTF_8);
      out.write(utf8.length);
      out.writeBytes(utf8);
    }

    /** v82's u16 string transfer stores the length minus 256. */
    void singleString16Offset(String text) {
      type(TracyQueueType.SingleStringData);
      byte[] utf8 = text.getBytes(StandardCharsets.UTF_8);
      writeU16(utf8.length - 256);
      out.writeBytes(utf8);
    }

    void message82(long absoluteTicks) {
      type(TracyQueueType.Message);
      writeLong(absoluteTicks);
      out.write(0x10); // metadata: severity info, source user
    }

    void messageColor82(long absoluteTicks, int r, int g, int b) {
      type(TracyQueueType.MessageColor);
      writeLong(absoluteTicks);
      out.write(b);
      out.write(g);
      out.write(r);
      out.write(0x10);
    }

    /** v82 samples: thread first, then the packed delta. */
    void callstackSample16(int deltaTicks, int thread) {
      type(TracyQueueType.CallstackSample16);
      writeInt(thread);
      writeU16(deltaTicks);
    }

    void callstackSample32(long deltaTicks, int thread) {
      type(TracyQueueType.CallstackSample32);
      writeInt(thread);
      writeInt((int)(deltaTicks - OFFSET_16BIT));
    }

    void tidToPid(long tid, long pid) {
      type(TracyQueueType.TidToPid);
      writeLong(tid);
      writeLong(pid);
    }

    void contextSwitch(long deltaTicks, int oldThread, int newThread, int cpu) {
      type(TracyQueueType.ContextSwitch);
      writeLong(deltaTicks);
      writeInt(oldThread);
      writeInt(newThread);
      out.write(cpu);
      out.write(new byte[5], 0, 5); // wait reason, state, c-state, priorities
    }

    void threadWakeup(long deltaTicks, int thread) {
      type(TracyQueueType.ThreadWakeup);
      writeLong(deltaTicks);
      writeInt(thread);
      out.write(new byte[3], 0, 3); // cpu, adjust reason + increment
    }

    void callstackSample(long deltaTicks, int thread) {
      type(TracyQueueType.CallstackSample);
      writeLong(deltaTicks);
      writeInt(thread);
    }

    void stringData(long pointer, String text) {
      type(TracyQueueType.StringData);
      writeLong(pointer);
      byte[] utf8 = text.getBytes(StandardCharsets.UTF_8);
      writeU16(utf8.length);
      out.writeBytes(utf8);
    }

    InputStream stream() {
      return new ByteArrayInputStream(out.toByteArray());
    }

    byte[] bytes() {
      return out.toByteArray();
    }

    private void writeU16(int value) {
      out.write(value & 0xFF);
      out.write(value >> 8 & 0xFF);
    }

    private void writeInt(int value) {
      for (int i = 0; i < 4; i++) out.write(value >> (8 * i) & 0xFF);
    }

    private void writeLong(long value) {
      for (int i = 0; i < 8; i++) out.write((int)(value >> (8 * i) & 0xFF));
    }
  }
}
