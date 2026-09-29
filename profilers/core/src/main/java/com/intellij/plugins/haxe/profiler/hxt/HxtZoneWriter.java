package com.intellij.plugins.haxe.profiler.hxt;

import com.intellij.plugins.haxe.profiler.io.LittleEndianPayload;
import com.intellij.plugins.haxe.profiler.model.ProfilerThread;
import com.intellij.plugins.haxe.profiler.model.TimelineEvent;
import com.intellij.plugins.haxe.profiler.tracy.TracyEventReader;
import com.intellij.plugins.haxe.profiler.tracy.TracySession;
import com.intellij.plugins.haxe.profiler.tracy.TracySourceLocation;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Streams an HXTS v6 zone capture to disk WHILE it is being received:
 * zones spool out in chunks as they close, so a minutes-long capture never
 * holds its zones on the heap, and the small series (frames, curves,
 * sweeps, events, names) follow as incremental records whenever the event
 * reader hands over a {@link TracyEventReader.SeriesBatch} — a live
 * reader's lanes populate as the capture runs. {@link #finish} writes what
 * the session still carries, the final THREAD records and INFO last.
 *
 * <pre>
 * header:  'H','X','T','S', u16 version=6, u32 tickHz=0,
 *          f64 epoch (seconds), u16 targetLen + utf8 "hxcpp-tracy"
 * record:  u8 type, u32 payloadLength, payload — unknown types skippable
 * SRCLOC(3): i32 count, count * (u16+utf8 function, u16+utf8 file,
 *            i32 line, i32 color) — appended to the accumulating table,
 *            always before the first chunk that references the entries
 * ZONES (2): i32 count, i64 minStartNs, i64 maxEndNs, i64 maxDurationNs,
 *            then one raw DEFLATE stream (JDK zlib, level 6; one
 *            independent stream per chunk so random access survives) of
 *            count * (i32 threadId, u16 depth, i64 startDeltaNs,
 *            i64 durationNs, i32 srclocIndex) — CLOSE order (per thread a
 *            post-order walk of the zone tree), RAW nanoseconds (rebase
 *            base in INFO); startDeltaNs is against the PREVIOUS zone's
 *            start in the same chunk (first zone: absolute), which turns
 *            the near-monotonic starts into small values the entropy
 *            coder folds tightly
 * FRAMES(4): i32 count, count * i64 ns
 * PLOT  (5): u16+utf8 name, i32 count, count * (i64 ns, f64 value)
 * CPU   (6): i32 count, count * (i64 ns, f64 percent)
 * PCPU (12): i32 count, count * (i64 ns, f64 percentOfOneCore) — the
 *            profiled process's scheduler-exact CPU use; absent unless the
 *            run had the privileges system tracing needs
 * THREAD(8): i32 threadId, u16+utf8 name, i64 zoneCount
 * MEMORY(9): u16+utf8 poolName, i32 count, count * (i64 ns, f64 liveBytes)
 * GC   (10): i32 count, count * (i64 startNs, i64 endNs, i64 freedBytes,
 *            i32 freedObjects)
 * EVENTS(11): i32 count, count * (i32 threadId, i64 ns, i32 rgb,
 *            u16+utf8 text) — instant marks (tracy messages)
 * INFO  (7): u16+utf8 programName, u64 pid, u64 epoch, i64 durationNs,
 *            i32 unmatchedZoneEnds, i64 baseNs, u8 deflateLevel (optional
 *            trailing — provenance only, inflate never needs it) —
 *            written LAST
 *
 * All series times are RAW nanoseconds like the zones (v5 carried them
 * pre-rebased); the reader subtracts INFO's baseNs. Series records
 * (FRAMES/PLOT/CPU/PCPU/MEMORY/GC/EVENTS) may repeat — a live capture
 * streams increments — and readers APPEND them; a later THREAD record for
 * the same id overrides the earlier one.
 * </pre>
 */
public final class HxtZoneWriter implements TracyEventReader.ZoneSink {

  static final int VERSION = 6;
  static final int INFO_RECORD = 7;
  static final int SRCLOC_RECORD = 3;
  static final int ZONES_RECORD = 2;
  static final int FRAMES_RECORD = 4;
  static final int PLOT_RECORD = 5;
  static final int CPU_RECORD = 6;
  static final int THREAD_RECORD = 8;
  static final int MEMORY_RECORD = 9;
  static final int GC_RECORD = 10;
  static final int EVENTS_RECORD = 11;
  static final int PROCESS_CPU_RECORD = 12;

  static final int ZONE_BYTES = 4 + 2 + 8 + 8 + 4;
  private static final String TARGET = "hxcpp-tracy";
  private static final int CHUNK_ZONES = 65_536;
  /** The archive level a finished file settles at (zlib's knee on our data). */
  public static final int FINAL_LEVEL = 6;
  /**
   * The level a LIVE capture writes at: the profiled app runs concurrently
   * with this thread, so minimal CPU beats ratio while it is alive —
   * {@link HxtZoneRecompressor} brings the file to {@link #FINAL_LEVEL}
   * after the app has exited. Readers never care: inflate is
   * level-agnostic.
   */
  public static final int LIVE_LEVEL = 1;

  /**
   * A partial chunk also flushes once it is this old: a full chunk takes
   * seconds to fill at ordinary zone rates, and a live view can only see
   * what is on disk; without an age bound its refresh cadence IS the
   * chunk-fill time.
   */
  private static final long MAX_CHUNK_AGE_NANOS = 500_000_000;

  private final OutputStream out;
  private final int level;
  private final Map<TracySourceLocation, Integer> locationIndex = new LinkedHashMap<>();
  private int flushedLocations;

  private final ByteBuffer chunk = ByteBuffer.allocate(CHUNK_ZONES * ZONE_BYTES).order(ByteOrder.LITTLE_ENDIAN);
  private long chunkOpenedAtNanos;
  private int chunkZones;
  private long chunkMinStartNs;
  private long chunkMaxEndNs;
  private long chunkMaxDurationNs;
  private long previousStartNs;

  private final Map<Integer, Long> zoneCountByThread = new HashMap<>();
  /** Threads whose first zone landed since the last chunk flush; announced then so a live reader can list them. */
  private final List<Integer> unannouncedThreads = new ArrayList<>();
  private long zoneCount;
  private long flushedZones;
  private long baseNs;

  public HxtZoneWriter(@NotNull OutputStream out, double epochSeconds) throws IOException {
    this(out, epochSeconds, FINAL_LEVEL);
  }

  public HxtZoneWriter(@NotNull OutputStream out, double epochSeconds, int level) throws IOException {
    this.out = out;
    this.level = level;
    // the tick rate is meaningless for exact zones
    HxtFormat.writeHeader(out, new HxtFormat.Header(VERSION, 0, epochSeconds, TARGET));
    resetChunk();
  }

  @Override
  public void zone(int threadId, int depth, long startNs, long endNs, @NotNull TracySourceLocation location) {
    Integer index = locationIndex.computeIfAbsent(location, key -> locationIndex.size());
    chunk.putInt(threadId);
    chunk.putShort((short)Math.min(depth, 0xFFFF));
    chunk.putLong(startNs - previousStartNs);
    chunk.putLong(endNs - startNs);
    chunk.putInt(index);
    previousStartNs = startNs;
    chunkZones++;
    chunkMinStartNs = Math.min(chunkMinStartNs, startNs);
    chunkMaxEndNs = Math.max(chunkMaxEndNs, endNs);
    chunkMaxDurationNs = Math.max(chunkMaxDurationNs, endNs - startNs);
    zoneCount++;
    if (zoneCountByThread.merge(threadId, 1L, Long::sum) == 1L) {
      unannouncedThreads.add(threadId);
    }
    if (chunkZones == CHUNK_ZONES || System.nanoTime() - chunkOpenedAtNanos > MAX_CHUNK_AGE_NANOS) {
      try {
        flushChunk();
      }
      catch (IOException e) {
        // the sink is called from the capture's read loop, which only
        // throws IOException - surface the disk failure as one there
        throw new UncheckedIOException(e);
      }
    }
  }

  @Override
  public void finished(long base) {
    baseNs = base;
  }

  /** Zones spooled so far — the whole capture once the stream has ended. */
  public long zoneCount() {
    return zoneCount;
  }

  /** Zones already ON DISK in complete chunks — what a live reader can see. */
  public long flushedZones() {
    return flushedZones;
  }

  /**
   * One live increment of the small series, raw times — each piece lands
   * as its own record and readers append. Flushed immediately so a live
   * reader sees it.
   */
  @Override
  public void series(TracyEventReader.@NotNull SeriesBatch batch) {
    try {
      writeSeries(batch, 0);
      out.flush();
    }
    catch (IOException e) {
      // the sink is called from the capture's read loop, which only
      // throws IOException - surface the disk failure as one there
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Writes what the session still carries (a batch-fed session's series
   * are empty — the increments already landed), the final named THREAD
   * records and INFO last. A session's times are session-relative while
   * the file's are raw, so the base is added back. Call once, after the
   * read returned the session.
   */
  public void finish(@NotNull TracySession session) throws IOException {
    flushChunk();
    TracyEventReader.SeriesBatch remainder =
      new TracyEventReader.SeriesBatch(session.frameMarksNs(), session.plots(), session.cpuUsage(),
                                       session.processCpu(), session.memoryCurves(), session.gcSweeps(),
                                       session.events(), Map.of());
    writeSeries(remainder, baseNs);

    // every thread that closed zones gets a record, answered name or not -
    // the picker must list it either way
    Map<Integer, String> threadNames = new TreeMap<>(session.threadNames());
    for (Integer threadId : zoneCountByThread.keySet()) {
      threadNames.putIfAbsent(threadId, ProfilerThread.unnamed(threadId));
    }
    for (Map.Entry<Integer, String> thread : threadNames.entrySet()) {
      record(THREAD_RECORD, threadPayload(thread.getKey(), thread.getValue()));
    }

    LittleEndianPayload info = new LittleEndianPayload()
      .string16(session.welcome().programName())
      .i64(session.welcome().pid())
      .i64(session.welcome().epoch())
      .i64(session.durationNs())
      .i32(session.unmatchedZoneEnds())
      .i64(baseNs)
      .u8(level);
    record(INFO_RECORD, info);
  }

  /** Every non-empty piece of the batch as its own record; {@code shiftNs} moves session-relative times back to raw. */
  private void writeSeries(TracyEventReader.SeriesBatch batch, long shiftNs) throws IOException {
    if (!batch.frameMarksNs().isEmpty()) record(FRAMES_RECORD, framesPayload(batch.frameMarksNs(), shiftNs));
    // deterministic order keeps the file byte-stable for identical batches
    for (Map.Entry<String, List<TracySession.PlotPoint>> plot : new TreeMap<>(batch.plots()).entrySet()) {
      record(PLOT_RECORD, namedPointsPayload(plot.getKey(), plot.getValue(), shiftNs));
    }
    if (!batch.cpuUsage().isEmpty()) record(CPU_RECORD, pointsPayload(batch.cpuUsage(), shiftNs));
    if (!batch.processCpu().isEmpty()) record(PROCESS_CPU_RECORD, pointsPayload(batch.processCpu(), shiftNs));
    for (Map.Entry<String, List<TracySession.PlotPoint>> curve : new TreeMap<>(batch.memoryCurves()).entrySet()) {
      record(MEMORY_RECORD, namedPointsPayload(curve.getKey(), curve.getValue(), shiftNs));
    }
    if (!batch.gcSweeps().isEmpty()) record(GC_RECORD, sweepsPayload(batch.gcSweeps(), shiftNs));
    if (!batch.events().isEmpty()) record(EVENTS_RECORD, eventsPayload(batch.events(), shiftNs));
    for (Map.Entry<Integer, String> thread : new TreeMap<>(batch.threadNames()).entrySet()) {
      record(THREAD_RECORD, threadPayload(thread.getKey(), thread.getValue()));
    }
  }

  private static LittleEndianPayload framesPayload(List<Long> frameMarksNs, long shiftNs) {
    LittleEndianPayload frames = new LittleEndianPayload().i32(frameMarksNs.size());
    for (long ns : frameMarksNs) frames.i64(ns + shiftNs);
    return frames;
  }

  private static LittleEndianPayload namedPointsPayload(String name, List<TracySession.PlotPoint> points, long shiftNs) {
    return appendPoints(new LittleEndianPayload().string16(name), points, shiftNs);
  }

  private static LittleEndianPayload pointsPayload(List<TracySession.PlotPoint> points, long shiftNs) {
    return appendPoints(new LittleEndianPayload(), points, shiftNs);
  }

  private static LittleEndianPayload appendPoints(LittleEndianPayload payload, List<TracySession.PlotPoint> points,
                                                  long shiftNs) {
    payload.i32(points.size());
    for (TracySession.PlotPoint point : points) {
      payload.i64(point.timeNs() + shiftNs).f64(point.value());
    }
    return payload;
  }

  private static LittleEndianPayload sweepsPayload(List<TracySession.GcSweep> gcSweeps, long shiftNs) {
    LittleEndianPayload sweeps = new LittleEndianPayload().i32(gcSweeps.size());
    for (TracySession.GcSweep sweep : gcSweeps) {
      sweeps.i64(sweep.startNs() + shiftNs)
        .i64(sweep.endNs() + shiftNs)
        .i64(sweep.freedBytes())
        .i32(sweep.freedObjects());
    }
    return sweeps;
  }

  private static LittleEndianPayload eventsPayload(List<TimelineEvent> timelineEvents, long shiftNs) {
    LittleEndianPayload events = new LittleEndianPayload().i32(timelineEvents.size());
    for (TimelineEvent event : timelineEvents) {
      events.i32(event.threadId())
        .i64(event.timeNs() + shiftNs)
        .i32(event.color())
        .string16(event.text());
    }
    return events;
  }

  private LittleEndianPayload threadPayload(int threadId, String name) {
    return new LittleEndianPayload()
      .i32(threadId)
      .string16(name)
      .i64(zoneCountByThread.getOrDefault(threadId, 0L));
  }

  /** New source locations first (chunks index into the accumulated table), then the zone chunk with its bounds. */
  private void flushChunk() throws IOException {
    if (locationIndex.size() > flushedLocations) {
      List<TracySourceLocation> all = List.copyOf(locationIndex.keySet());
      List<TracySourceLocation> fresh = all.subList(flushedLocations, all.size());
      LittleEndianPayload locations = new LittleEndianPayload().i32(fresh.size());
      for (TracySourceLocation location : fresh) {
        locations.string16(location.function())
          .string16(location.file())
          .i32(location.line())
          .i32(location.color());
      }
      record(SRCLOC_RECORD, locations);
      flushedLocations = locationIndex.size();
    }
    if (chunkZones == 0) return;

    // the JDK's own zlib, NOT commons-compress: the latter's LZ77 compressor
    // falls into a pathological slow path on real zone data (seconds per
    // chunk), and this runs on the socket thread the app's exit drain waits
    // behind; deflate also compresses better
    byte[] compressed = HxtFormat.deflate(chunk.array(), chunk.position(), level);
    LittleEndianPayload zones = new LittleEndianPayload()
      .i32(chunkZones)
      .i64(chunkMinStartNs)
      .i64(chunkMaxEndNs)
      .i64(chunkMaxDurationNs)
      .raw(compressed);
    record(ZONES_RECORD, zones);
    flushedZones = zoneCount;

    // first-sighted threads announce themselves nameless, so a live reader
    // has a thread list before finish()'s named records override these
    for (Integer threadId : unannouncedThreads) {
      record(THREAD_RECORD, threadPayload(threadId, ""));
    }
    unannouncedThreads.clear();
    out.flush(); // a live reader re-reads the file while it grows
    resetChunk();
  }

  private void resetChunk() {
    chunk.clear();
    chunkZones = 0;
    chunkOpenedAtNanos = System.nanoTime();
    chunkMinStartNs = Long.MAX_VALUE;
    chunkMaxEndNs = Long.MIN_VALUE;
    chunkMaxDurationNs = 0;
    previousStartNs = 0; // each chunk's delta stream restarts, so chunks stay independent
  }

  private void record(int type, LittleEndianPayload payload) throws IOException {
    HxtFormat.writeRecord(out, type, payload);
  }
}
