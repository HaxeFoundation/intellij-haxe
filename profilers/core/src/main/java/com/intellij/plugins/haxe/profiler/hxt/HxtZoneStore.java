package com.intellij.plugins.haxe.profiler.hxt;

import com.intellij.plugins.haxe.profiler.model.ProfilerFormatException;
import com.intellij.plugins.haxe.profiler.model.ProfilerThread;
import com.intellij.plugins.haxe.profiler.model.TimelineEvent;
import com.intellij.plugins.haxe.profiler.tracy.TracySession;
import com.intellij.plugins.haxe.profiler.tracy.TracySourceLocation;
import com.intellij.plugins.haxe.profiler.tracy.wire.TracyProtocolVersion;
import com.intellij.plugins.haxe.profiler.tracy.wire.TracyWelcome;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.intellij.plugins.haxe.profiler.io.LittleEndian.*;

/**
 * A zone capture served FROM ITS FILE: opening indexes the zone chunks and
 * reads the small records, but the zones themselves stay on disk.
 * {@link #scanZones} decodes only chunks whose time and duration bounds pass
 * the query, so a minutes-long capture never loads whole. Zones stream in
 * close order (per thread a post-order walk of the zone tree, depth
 * included) with times rebased to the session's zero. Series records may
 * repeat, since a live writer streams increments, and append; v6 files carry
 * their stamps RAW like the zones (rebased here), v5 wrote them pre-rebased.
 */
public final class HxtZoneStore {

  /** The oldest zone-capture layout this store reads. */
  static final int OLDEST_VERSION = 5;
  /** Files from this version on carry their series stamps RAW; older ones wrote them pre-rebased. */
  private static final int RAW_SERIES_VERSION = 6;
  // TODO: record the protocol version in the HXTS header; a stored session is never decoded through the wire reader again
  private static final TracyProtocolVersion STORED_SESSION_PROTOCOL = TracyProtocolVersion.V74;

  private final Path file;
  private final TracySession session;
  private final List<TracySourceLocation> locations;
  private final List<ChunkRef> chunks;
  private final List<ThreadEntry> threads;
  private final long baseNs;
  private final long zoneCount;
  private final int compressionLevel;

  private record ChunkRef(long payloadOffset, int compressedLength, int zoneCount,
                          long minStartNs, long maxEndNs, long maxDurationNs) {
  }

  /** One captured thread, its resolved name and how many zones it closed. */
  public record ThreadEntry(int id, @NotNull String name, long zoneCount) {
  }

  /** Receives zones from a scan; times are session-relative nanoseconds. */
  public interface ZoneConsumer {
    void zone(int threadId, int depth, long startNs, long endNs, @NotNull TracySourceLocation location);
  }

  private HxtZoneStore(Path file, TracySession session, List<TracySourceLocation> locations,
                       List<ChunkRef> chunks, List<ThreadEntry> threads, long baseNs, long zoneCount,
                       int compressionLevel) {
    this.file = file;
    this.session = session;
    this.locations = locations;
    this.chunks = chunks;
    this.threads = threads;
    this.baseNs = baseNs;
    this.zoneCount = zoneCount;
    this.compressionLevel = compressionLevel;
  }

  /** Indexes the file: chunk offsets and bounds, the source-location table and every small record. */
  @NotNull
  public static HxtZoneStore open(@NotNull Path file) throws IOException {
    String programName = "";
    long pid = 0;
    long epoch = 0;
    long durationNs = 0;
    int unmatched = 0;
    long baseNs = 0;
    int compressionLevel = -1;
    long zoneCount = 0;
    List<TracySourceLocation> locations = new ArrayList<>();
    List<ChunkRef> chunks = new ArrayList<>();
    List<Long> frames = new ArrayList<>();
    Map<String, List<TracySession.PlotPoint>> plots = new HashMap<>();
    Map<String, List<TracySession.PlotPoint>> memoryCurves = new HashMap<>();
    List<TracySession.GcSweep> gcSweeps = new ArrayList<>();
    List<TimelineEvent> events = new ArrayList<>();
    List<TracySession.PlotPoint> cpu = new ArrayList<>();
    List<TracySession.PlotPoint> processCpu = new ArrayList<>();
    Map<Integer, String> threadNames = new HashMap<>();
    Map<Integer, Long> threadZones = new HashMap<>();

    boolean infoSeen = false;
    long minChunkStartNs = Long.MAX_VALUE;
    long maxChunkEndNs = Long.MIN_VALUE;
    int version;
    try (InputStream raw = new BufferedInputStream(Files.newInputStream(file))) {
      CountingStream in = new CountingStream(raw);
      DataInputStream data = new DataInputStream(in);
      version = HxtFormat.readHeader(data).version();
      if (version < OLDEST_VERSION || version > HxtZoneWriter.VERSION) {
        throw new ProfilerFormatException("unsupported HXTS zone version " + version);
      }
      while (true) {
        int type;
        int length;
        try {
          type = data.readUnsignedByte();
          length = readIntLe(data);
        }
        catch (EOFException end) {
          break; // clean end, or a live writer caught mid-record-header
        }
        if (type == HxtZoneWriter.ZONES_RECORD) {
          ChunkRef chunk;
          int count;
          try {
            count = readIntLe(data);
            long minStart = readLongLe(data);
            long maxEnd = readLongLe(data);
            long maxDuration = readLongLe(data);
            int compressedLength = length - HxtFormat.CHUNK_BOUNDS_BYTES;
            chunk = new ChunkRef(in.position, compressedLength, count, minStart, maxEnd, maxDuration);
            data.skipNBytes(compressedLength);
          }
          catch (EOFException truncated) {
            break; // the capture died mid-chunk - keep the complete chunks
          }
          chunks.add(chunk);
          zoneCount += count;
          minChunkStartNs = Math.min(minChunkStartNs, chunk.minStartNs());
          maxChunkEndNs = Math.max(maxChunkEndNs, chunk.maxEndNs());
          continue;
        }
        byte[] payloadBytes = data.readNBytes(length);
        if (payloadBytes.length < length) break; // truncated tail - keep what is complete
        DataInputStream payload = new DataInputStream(new ByteArrayInputStream(payloadBytes));
        switch (type) {
          case HxtZoneWriter.SRCLOC_RECORD -> {
            int count = readIntLe(payload);
            for (int i = 0; i < count; i++) {
              locations.add(new TracySourceLocation(readString16Le(payload), readString16Le(payload),
                                                    readIntLe(payload), readIntLe(payload)));
            }
          }
          case HxtZoneWriter.FRAMES_RECORD -> {
            int count = readIntLe(payload);
            for (int i = 0; i < count; i++) frames.add(readLongLe(payload));
          }
          // series records may arrive incrementally from a live writer - append
          case HxtZoneWriter.PLOT_RECORD ->
            plots.computeIfAbsent(readString16Le(payload), key -> new ArrayList<>()).addAll(readPoints(payload));
          case HxtZoneWriter.CPU_RECORD -> cpu.addAll(readPoints(payload));
          case HxtZoneWriter.PROCESS_CPU_RECORD -> processCpu.addAll(readPoints(payload));
          case HxtZoneWriter.THREAD_RECORD -> {
            int id = readIntLe(payload);
            threadNames.put(id, readString16Le(payload));
            threadZones.put(id, readLongLe(payload));
          }
          case HxtZoneWriter.MEMORY_RECORD ->
            memoryCurves.computeIfAbsent(readString16Le(payload), key -> new ArrayList<>()).addAll(readPoints(payload));
          case HxtZoneWriter.GC_RECORD -> {
            int count = readIntLe(payload);
            for (int i = 0; i < count; i++) {
              gcSweeps.add(new TracySession.GcSweep(readLongLe(payload), readLongLe(payload),
                                                    readLongLe(payload), readIntLe(payload)));
            }
          }
          case HxtZoneWriter.EVENTS_RECORD -> {
            int count = readIntLe(payload);
            for (int i = 0; i < count; i++) {
              int eventThread = readIntLe(payload);
              long timeNs = readLongLe(payload);
              int color = readIntLe(payload);
              events.add(new TimelineEvent(eventThread, timeNs, readString16Le(payload), color));
            }
          }
          case HxtZoneWriter.INFO_RECORD -> {
            infoSeen = true;
            programName = readString16Le(payload);
            pid = readLongLe(payload);
            epoch = readLongLe(payload);
            durationNs = readLongLe(payload);
            unmatched = readIntLe(payload);
            baseNs = readLongLe(payload);
            // optional trailing field - files written before it stay readable
            if (payload.available() > 0) compressionLevel = payload.readUnsignedByte();
          }
          default -> {
            // future record types skip by their length
          }
        }
      }
    }

    boolean rawSeries = version >= RAW_SERIES_VERSION;
    if (!infoSeen) {
      // a LIVE read of a growing capture: INFO lands only at finish, so
      // the raw bounds of the chunks - and, on raw-series files, of the
      // series - stand in for the rebase base and duration
      RawBounds series = rawSeries
                         ? seriesBounds(frames, List.of(plots, memoryCurves), List.of(cpu, processCpu),
                                        gcSweeps, events)
                         : RawBounds.NONE;
      long rawMin = Math.min(chunks.isEmpty() ? Long.MAX_VALUE : minChunkStartNs, series.minNs());
      long rawMax = Math.max(chunks.isEmpty() ? Long.MIN_VALUE : maxChunkEndNs, series.maxNs());
      if (rawMin != Long.MAX_VALUE) {
        baseNs = rawMin;
        durationNs = Math.max(rawMax - rawMin, 0);
      }
    }
    long seriesShiftNs = rawSeries ? baseNs : 0;
    plots.replaceAll((name, points) -> rebasedPoints(points, seriesShiftNs));
    memoryCurves.replaceAll((name, points) -> rebasedPoints(points, seriesShiftNs));
    frames = rebasedFrames(frames, seriesShiftNs);
    cpu = rebasedPoints(cpu, seriesShiftNs);
    processCpu = rebasedPoints(processCpu, seriesShiftNs);
    gcSweeps = rebasedSweeps(gcSweeps, seriesShiftNs);
    events = rebasedEvents(events, seriesShiftNs);
    TracyWelcome welcome = storedWelcome(programName, pid, epoch);
    TracySession session = new TracySession(welcome, List.of(), List.copyOf(frames), Map.copyOf(plots),
                                            Map.copyOf(memoryCurves), List.copyOf(gcSweeps), List.copyOf(events),
                                            List.copyOf(cpu), List.copyOf(processCpu), Map.copyOf(threadNames),
                                            durationNs, unmatched);
    List<ThreadEntry> threads = threadZones.entrySet().stream()
      .sorted(Map.Entry.<Integer, Long>comparingByValue(Comparator.reverseOrder()))
      .map(entry -> new ThreadEntry(entry.getKey(), threadName(threadNames, entry.getKey()), entry.getValue()))
      .toList();
    return new HxtZoneStore(file, session, List.copyOf(locations), List.copyOf(chunks),
                            threads, baseNs, zoneCount, compressionLevel);
  }

  /**
   * The welcome of a stored session: only the program name, pid and epoch
   * are kept in INFO; the timer is already applied (one tick = 1 ns).
   */
  private static TracyWelcome storedWelcome(String programName, long pid, long epoch) {
    return new TracyWelcome(STORED_SESSION_PROTOCOL, 1.0, 0, 0, 0, 0, epoch, 0, pid, 0, false, programName);
  }

  /** The raw stamp bounds across every small series; MAX/MIN sentinels when there are none. */
  private record RawBounds(long minNs, long maxNs) {
    static final RawBounds NONE = new RawBounds(Long.MAX_VALUE, Long.MIN_VALUE);

    RawBounds fold(long ns) {
      return new RawBounds(Math.min(minNs, ns), Math.max(maxNs, ns));
    }
  }

  private static RawBounds seriesBounds(List<Long> frames,
                                        List<Map<String, List<TracySession.PlotPoint>>> namedSeries,
                                        List<List<TracySession.PlotPoint>> pointSeries,
                                        List<TracySession.GcSweep> gcSweeps, List<TimelineEvent> events) {
    RawBounds bounds = RawBounds.NONE;
    for (long ns : frames) bounds = bounds.fold(ns);
    List<List<TracySession.PlotPoint>> allPoints = new ArrayList<>(pointSeries);
    namedSeries.forEach(series -> allPoints.addAll(series.values()));
    for (List<TracySession.PlotPoint> points : allPoints) {
      for (TracySession.PlotPoint point : points) bounds = bounds.fold(point.timeNs());
    }
    for (TracySession.GcSweep sweep : gcSweeps) {
      bounds = bounds.fold(sweep.startNs()).fold(sweep.endNs());
    }
    for (TimelineEvent event : events) bounds = bounds.fold(event.timeNs());
    return bounds;
  }

  /** Raw stamps to session time; the clamp covers a process-CPU bucket start preceding the base. */
  private static List<TracySession.PlotPoint> rebasedPoints(List<TracySession.PlotPoint> points, long shiftNs) {
    return points.stream()
      .map(point -> new TracySession.PlotPoint(Math.max(point.timeNs() - shiftNs, 0), point.value()))
      .sorted(Comparator.comparingLong(TracySession.PlotPoint::timeNs))
      .toList();
  }

  private static List<Long> rebasedFrames(List<Long> frames, long shiftNs) {
    return frames.stream().map(ns -> Math.max(ns - shiftNs, 0)).sorted().toList();
  }

  private static List<TracySession.GcSweep> rebasedSweeps(List<TracySession.GcSweep> sweeps, long shiftNs) {
    return sweeps.stream()
      .map(sweep -> new TracySession.GcSweep(Math.max(sweep.startNs() - shiftNs, 0),
                                             Math.max(sweep.endNs() - shiftNs, 0),
                                             sweep.freedBytes(), sweep.freedObjects()))
      .sorted(Comparator.comparingLong(TracySession.GcSweep::startNs))
      .toList();
  }

  private static List<TimelineEvent> rebasedEvents(List<TimelineEvent> events, long shiftNs) {
    return events.stream()
      .map(event -> new TimelineEvent(event.threadId(), Math.max(event.timeNs() - shiftNs, 0),
                                      event.text(), event.color()))
      .sorted(Comparator.comparingLong(TimelineEvent::timeNs))
      .toList();
  }

  /** The capture minus its zones (the zone list is always empty — zones come from {@link #scanZones}). */
  @NotNull
  public TracySession session() {
    return session;
  }

  /** The capture file this store serves from — a live view reopens it as it grows. */
  @NotNull
  public Path file() {
    return file;
  }

  public long zoneCount() {
    return zoneCount;
  }

  /** The deflate level the zone chunks were written at, or -1 for a file that predates the field. Provenance only — reading never needs it. */
  public int compressionLevel() {
    return compressionLevel;
  }

  /** The captured threads, busiest first. */
  @NotNull
  public List<ThreadEntry> threads() {
    return threads;
  }

  /**
   * Streams the zones matching the query in close order. {@code fromNs} /
   * {@code toNs} bound the SESSION-RELATIVE window (pass 0 and
   * {@link Long#MAX_VALUE} for all); zones shorter than
   * {@code minDurationNs} are skipped, and chunks whose bounds cannot
   * match are never read. {@code threadId} of -1 accepts every thread.
   */
  public void scanZones(int threadId, long fromNs, long toNs, long minDurationNs,
                        @NotNull ZoneConsumer consumer) throws IOException {
    long rawFrom = fromNs == 0 ? Long.MIN_VALUE : baseNs + fromNs;
    long rawTo = toNs == Long.MAX_VALUE ? Long.MAX_VALUE : baseNs + toNs;
    try (RandomAccessFile access = new RandomAccessFile(file.toFile(), "r")) {
      ChunkBuffers buffers = new ChunkBuffers();
      for (ChunkRef chunk : chunks) {
        boolean overlaps = chunk.minStartNs() <= rawTo && chunk.maxEndNs() >= rawFrom;
        if (!overlaps || chunk.maxDurationNs() < minDurationNs) continue;
        scanChunk(access, chunk, buffers, threadId, rawFrom, rawTo, minDurationNs, consumer);
      }
    }
  }

  /** How many zone chunks the file held when this store opened — a live reader's incremental cursor. */
  public int chunkCount() {
    return chunks.size();
  }

  /**
   * Every zone of chunks {@code [fromChunk, chunkCount())}, rebased and
   * unfiltered — the incremental feed a live view folds so a refresh only
   * ever inflates the NEWLY appended chunks, not the whole file.
   */
  public void scanZonesFromChunk(int fromChunk, @NotNull ZoneConsumer consumer) throws IOException {
    try (RandomAccessFile access = new RandomAccessFile(file.toFile(), "r")) {
      ChunkBuffers buffers = new ChunkBuffers();
      for (int i = Math.max(fromChunk, 0); i < chunks.size(); i++) {
        scanChunk(access, chunks.get(i), buffers, -1, Long.MIN_VALUE, Long.MAX_VALUE, 0, consumer);
      }
    }
  }

  /** Reusable inflate targets across one scan's chunks. */
  private static final class ChunkBuffers {
    byte[] compressed = new byte[0];
    byte[] raw = new byte[0];
  }

  private void scanChunk(RandomAccessFile access, ChunkRef chunk, ChunkBuffers buffers,
                         int threadId, long rawFrom, long rawTo, long minDurationNs,
                         ZoneConsumer consumer) throws IOException {
    if (buffers.compressed.length < chunk.compressedLength()) buffers.compressed = new byte[chunk.compressedLength()];
    access.seek(chunk.payloadOffset());
    access.readFully(buffers.compressed, 0, chunk.compressedLength());
    int byteLength = chunk.zoneCount() * HxtZoneWriter.ZONE_BYTES;
    if (buffers.raw.length < byteLength) buffers.raw = new byte[byteLength];
    HxtFormat.inflate(buffers.compressed, chunk.compressedLength(), buffers.raw, byteLength);
    ByteBuffer zones = ByteBuffer.wrap(buffers.raw, 0, byteLength).order(ByteOrder.LITTLE_ENDIAN);
    long previousStartNs = 0;
    for (int i = 0; i < chunk.zoneCount(); i++) {
      int thread = zones.getInt();
      int depth = zones.getShort() & 0xFFFF;
      long startNs = previousStartNs + zones.getLong();
      long endNs = startNs + zones.getLong();
      int location = zones.getInt();
      previousStartNs = startNs;
      if (threadId >= 0 && thread != threadId) continue;
      if (endNs - startNs < minDurationNs) continue;
      if (startNs > rawTo || endNs < rawFrom) continue;
      if (location < 0 || location >= locations.size()) {
        throw new ProfilerFormatException("zone references unknown source location " + location);
      }
      consumer.zone(thread, depth, startNs - baseNs, endNs - baseNs, locations.get(location));
    }
  }

  private static String threadName(Map<Integer, String> names, int threadId) {
    String name = names.get(threadId);
    // a LIVE capture's incremental THREAD records carry no name yet
    return name != null && !name.isEmpty() ? name : ProfilerThread.unnamed(threadId);
  }

  private static List<TracySession.PlotPoint> readPoints(DataInputStream payload) throws IOException {
    int count = readIntLe(payload);
    List<TracySession.PlotPoint> points = new ArrayList<>(count);
    for (int i = 0; i < count; i++) {
      points.add(new TracySession.PlotPoint(readLongLe(payload), readDoubleLe(payload)));
    }
    return points;
  }

  /** Tracks the absolute file position so chunk payload offsets can be recorded. */
  private static final class CountingStream extends InputStream {
    private final InputStream in;
    long position;

    CountingStream(InputStream in) {
      this.in = in;
    }

    @Override
    public int read() throws IOException {
      int value = in.read();
      if (value >= 0) position++;
      return value;
    }

    @Override
    public int read(byte @NotNull [] buffer, int offset, int length) throws IOException {
      int read = in.read(buffer, offset, length);
      if (read > 0) position += read;
      return read;
    }
  }
}
