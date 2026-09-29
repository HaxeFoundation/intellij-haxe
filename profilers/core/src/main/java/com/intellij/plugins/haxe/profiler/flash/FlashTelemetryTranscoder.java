package com.intellij.plugins.haxe.profiler.flash;

import com.intellij.plugins.haxe.profiler.flash.Amf3Decoder.Amf3Object;
import com.intellij.plugins.haxe.profiler.hxt.HxtSessionWriter;
import com.intellij.plugins.haxe.profiler.hxt.HxtSessionWriter.WeightedStack;
import com.intellij.plugins.haxe.profiler.model.PseudoFrames;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

/**
 * Translates the flash runtime's telemetry stream (the wire Adobe Scout
 * reads: consecutive AMF3 objects) into an HXTS v1 sample session, one
 * FRAME record per display frame, flushed as it goes so the live view can
 * follow. The runtime connects out to the address in {@code ~/.telemetry.cfg}
 * when the swf was compiled with {@code -D advanced-telemetry}.
 *
 * Wire facts this reader depends on:
 * <ul>
 * <li>Every {@code .span}/{@code .spanValue}/{@code .time} message carries
 *     {@code delta} — microseconds since the previous timed message; their
 *     running sum is the session clock. A {@code span} is a duration ending
 *     at the message's clock position.</li>
 * <li>{@code .enter} opens a display frame; {@code .exit} closes its busy
 *     part. The frame's sampler and memory messages follow the exit, so a
 *     window is finalized at the NEXT {@code .enter} (or stream end) and
 *     spans enter-to-enter — the idle tail belongs to the frame.</li>
 * <li>{@code .sampler.methodNameMap} carries NUL-separated UTF-8 method
 *     names; indices are 1-based and cumulative across the session, and a
 *     large table ships as MANY map messages that LAG the samples first
 *     referencing them by up to seconds (samples ship immediately, names
 *     with a later stats batch). Windows therefore wait in a queue until
 *     their indices are announced before their frame record is written.
 *     The sampler only runs on the debugger runtime (adl without
 *     -nodebug).</li>
 * <li>{@code .mem.*} values are kilobytes.</li>
 * </ul>
 */
public final class FlashTelemetryTranscoder {

  /**
   * A sampler tick outside any {@code .player.enterframe} span (startup
   * scripts, non-frame event handlers) claims the time back to the previous
   * tick — capped, so the first tick after a long pause cannot claim the
   * whole pause as busy.
   */
  private static final long MAX_ORPHAN_TICK_CLAIM_US = 5_000;
  /**
   * How long a finalized window may wait for the name map to announce the
   * method indices its ticks reference before it is written with what is
   * known. Name batches normally arrive well within a second; only a stream
   * that dies mid-batch leaves names unannounced for good.
   */
  private static final long MAX_NAME_WAIT_US = 10_000_000;

  private FlashTelemetryTranscoder() {
  }

  /**
   * Reads telemetry until the stream ends and writes the v1 session,
   * flushing frame records as their names resolve. A torn or foreign tail
   * keeps the frames completed before it. Returns the bytes written.
   */
  public static long transcode(@NotNull InputStream in, @NotNull OutputStream out) throws IOException {
    return new Session(out).run(new Amf3Decoder(in));
  }

  /**
   * Telemetry method names read {@code owner/method} with {@code ::}
   * package separators, a trailing {@code $} on static owners
   * ({@code pkg::Class$/method}) and AS3 builtin namespace URIs
   * ({@code Array/http://ns::push}); the URI drops and the separators
   * unify to dots.
   */
  static String normalizeMethodName(String raw) {
    // a namespace URI segment: scheme://anything up to the :: that follows it
    String name = raw.replaceAll("https?://[^:]*::", "");
    return name.replace("$/", ".")
      .replace("::", ".")
      .replace('/', '.');
  }

  /**
   * A named stretch of the clock: sampler-attributed stretches carry the
   * tick's raw name-table indices (root-first, resolved only when the
   * frame record is written), the rest a pseudo-frame name.
   */
  private record Segment(long startUs, long endUs, int[] samplerStack, List<String> pseudoStack) {
    static Segment sampler(long startUs, long endUs, int[] rootFirstStack) {
      return new Segment(startUs, endUs, rootFirstStack, null);
    }

    static Segment pseudo(long startUs, long endUs, List<String> stack) {
      return new Segment(startUs, endUs, null, stack);
    }
  }

  private record Interval(long startUs, long endUs) {}

  private record Tick(long timeUs, int[] rootFirstStack) {}

  /** A finalized enter-to-enter window queued until its sampler indices are announced. */
  private record PendingWindow(long endUs, List<Segment> segments, long gcUs,
                               long usedKb, long reservedKb, int maxNameIndex) {}

  private static final class Session {
    private final HxtSessionWriter writer;

    private long clockUs;
    private long windowStartUs;

    /** Telemetry sampler method names; 1-based, cumulative across name-map messages. */
    private final List<String> samplerNames = new ArrayList<>(List.of(""));
    /** A name split across two map messages: the unterminated tail carries into the next chunk. */
    private byte[] samplerNameTail = new byte[0];

    private final List<Interval> scriptIntervals = new ArrayList<>();
    private final List<Interval> renderIntervals = new ArrayList<>();
    private final List<Interval> gcIntervals = new ArrayList<>();
    private final List<Tick> ticks = new ArrayList<>();
    private final Deque<PendingWindow> pendingWindows = new ArrayDeque<>();
    private long gcUs;
    private long previousTickUs;
    private long managedUsedKb;
    private long managedKb;

    Session(OutputStream out) {
      writer = new HxtSessionWriter(out, "flash");
    }

    long run(Amf3Decoder decoder) throws IOException {
      while (true) {
        Object value;
        try {
          value = decoder.readValue();
        }
        catch (IOException endOrTorn) {
          break; // clean end, torn tail or foreign bytes: keep the completed frames
        }
        if (value instanceof Amf3Object message) {
          handle(message);
        }
      }
      finalizeWindow(clockUs);
      flushResolvedWindows(true);
      return writer.bytesWritten();
    }

    private void handle(Amf3Object message) throws IOException {
      if (message.member("delta") instanceof Number delta) {
        clockUs += delta.longValue();
      }
      long span = message.member("span") instanceof Number value ? value.longValue() : 0;
      String name = message.member("name") instanceof String text ? text : "";
      switch (name) {
        case ".enter" -> {
          finalizeWindow(clockUs);
          windowStartUs = clockUs;
        }
        case ".player.enterframe" -> scriptIntervals.add(spanInterval(span));
        case ".rend.update", ".rend.screen" -> renderIntervals.add(spanInterval(span));
        case ".gc.Mark", ".gc.Sweep", ".gc.Reap" -> {
          gcIntervals.add(spanInterval(span));
          gcUs += span;
        }
        case ".sampler.methodNameMap" -> addSamplerNames(message);
        case ".sampler.sample" -> addTicks(message);
        case ".mem.managed.used" -> managedUsedKb = kilobytes(message);
        case ".mem.managed" -> managedKb = kilobytes(message);
        default -> { }
      }
    }

    private Interval spanInterval(long span) {
      return new Interval(clockUs - span, clockUs);
    }

    private static long kilobytes(Amf3Object message) {
      return message.member("value") instanceof Number value ? value.longValue() : 0;
    }

    private void addSamplerNames(Amf3Object message) throws IOException {
      if (!(message.member("value") instanceof byte[] chunk)) return;
      byte[] bytes = new byte[samplerNameTail.length + chunk.length];
      System.arraycopy(samplerNameTail, 0, bytes, 0, samplerNameTail.length);
      System.arraycopy(chunk, 0, bytes, samplerNameTail.length, chunk.length);
      // NUL-terminated UTF-8 names; an unterminated tail continues in the next chunk
      int start = 0;
      for (int i = 0; i < bytes.length; i++) {
        if (bytes[i] != 0) continue;
        samplerNames.add(normalizeMethodName(new String(bytes, start, i - start, StandardCharsets.UTF_8)));
        start = i + 1;
      }
      samplerNameTail = start == bytes.length ? new byte[0] : Arrays.copyOfRange(bytes, start, bytes.length);
      flushResolvedWindows(false); // the new names may unblock waiting windows
    }

    private void addTicks(Amf3Object message) {
      if (!(message.member("value") instanceof Amf3Object sample)) return;
      if (!(sample.member("ticktimes") instanceof List<?> tickTimes)) return;
      if (!(sample.member("callstack") instanceof List<?> callstack)) return;
      int depth = callstack.size();
      if (depth == 0) return;
      int[] rootFirst = new int[depth];
      for (int i = 0; i < depth; i++) {
        if (!(callstack.get(i) instanceof Number index)) return;
        rootFirst[depth - 1 - i] = index.intValue(); // the wire stack is leaf-first
      }
      for (Object tickTime : tickTimes) {
        if (tickTime instanceof Number time) {
          ticks.add(new Tick(time.longValue(), rootFirst));
        }
      }
    }

    private String samplerName(int index) {
      return index > 0 && index < samplerNames.size() ? samplerNames.get(index) : "(unknown)";
    }

    /** Closes the window at {@code endUs}: attributes its clock to segments and queues one frame record. */
    private void finalizeWindow(long endUs) throws IOException {
      if (endUs > windowStartUs) {
        List<Segment> segments = new ArrayList<>();
        attributeScript(segments, endUs);
        attributeOrphanTicks(segments, endUs);
        for (Interval render : renderIntervals) {
          segments.add(Segment.pseudo(render.startUs(), render.endUs(), List.of(PseudoFrames.RENDER)));
        }
        for (Interval gc : gcIntervals) {
          segments.add(Segment.pseudo(gc.startUs(), gc.endUs(), List.of(PseudoFrames.GC)));
        }
        List<Segment> tiled = tiled(segments, endUs);
        int maxNameIndex = 0;
        for (Segment segment : tiled) {
          if (segment.samplerStack() == null) continue;
          for (int index : segment.samplerStack()) {
            maxNameIndex = Math.max(maxNameIndex, index);
          }
        }
        pendingWindows.add(new PendingWindow(endUs, tiled, gcUs, managedUsedKb, managedKb, maxNameIndex));
      }
      scriptIntervals.clear();
      renderIntervals.clear();
      gcIntervals.clear();
      ticks.clear();
      gcUs = 0;
      flushResolvedWindows(false);
    }

    /**
     * Writes queued windows in order, each once the name table covers every
     * index its ticks reference — the map messages lag the samples, so a
     * window typically waits one name batch. A window older than the wait
     * bound (or everything, at stream end) writes with what is known.
     */
    private void flushResolvedWindows(boolean force) throws IOException {
      while (!pendingWindows.isEmpty()) {
        PendingWindow head = pendingWindows.peekFirst();
        boolean resolvable = head.maxNameIndex() < samplerNames.size();
        boolean expired = clockUs - head.endUs() > MAX_NAME_WAIT_US;
        if (!force && !resolvable && !expired) break;
        writeFrame(pendingWindows.removeFirst());
      }
    }

    /**
     * Splits each script span among the sampler ticks inside it — head and
     * tail join the nearest tick's claim, so the span's exact total is
     * preserved with the best stack attribution available. GC that ran
     * inside the span keeps its own segments, so it is carved out first.
     * A span no tick reached stays visible as one [script] segment.
     */
    private void attributeScript(List<Segment> segments, long endUs) {
      for (Interval script : subtract(scriptIntervals, gcIntervals)) {
        List<Tick> inside = new ArrayList<>();
        for (Tick tick : ticks) {
          if (tick.timeUs() >= script.startUs() && tick.timeUs() < script.endUs()) {
            inside.add(tick);
          }
        }
        if (inside.isEmpty()) {
          segments.add(Segment.pseudo(script.startUs(), Math.min(script.endUs(), endUs), List.of(PseudoFrames.SCRIPT)));
          continue;
        }
        long cursor = script.startUs();
        for (int i = 0; i < inside.size() - 1; i++) {
          Tick tick = inside.get(i);
          segments.add(Segment.sampler(cursor, tick.timeUs(), tick.rootFirstStack()));
          cursor = tick.timeUs();
        }
        Tick last = inside.get(inside.size() - 1);
        segments.add(Segment.sampler(cursor, Math.min(script.endUs(), endUs), last.rootFirstStack()));
        ticks.removeAll(inside);
        previousTickUs = last.timeUs();
      }
    }

    /** Ticks outside every script span (startup, non-frame handlers) claim back to the previous tick, capped. */
    private void attributeOrphanTicks(List<Segment> segments, long endUs) {
      for (Tick tick : ticks) {
        if (tick.timeUs() < windowStartUs || tick.timeUs() > endUs) continue;
        long spacing = previousTickUs > 0 ? tick.timeUs() - previousTickUs : MAX_ORPHAN_TICK_CLAIM_US;
        long claim = Math.clamp(spacing, 1, MAX_ORPHAN_TICK_CLAIM_US);
        segments.add(Segment.sampler(Math.max(tick.timeUs() - claim, windowStartUs), tick.timeUs(), tick.rootFirstStack()));
        previousTickUs = tick.timeUs();
      }
    }

    /** {@code from} minus every overlap with {@code cuts}; both lists hold in-window, mostly disjoint intervals. */
    private static List<Interval> subtract(List<Interval> from, List<Interval> cuts) {
      List<Interval> result = new ArrayList<>(from);
      for (Interval cut : cuts) {
        List<Interval> next = new ArrayList<>(result.size());
        for (Interval interval : result) {
          boolean disjoint = cut.endUs() <= interval.startUs() || cut.startUs() >= interval.endUs();
          if (disjoint) {
            next.add(interval);
            continue;
          }
          if (cut.startUs() > interval.startUs()) next.add(new Interval(interval.startUs(), cut.startUs()));
          if (cut.endUs() < interval.endUs()) next.add(new Interval(cut.endUs(), interval.endUs()));
        }
        result = next;
      }
      return result;
    }

    /**
     * Orders the segments and fills every gap with [idle], so the window is
     * tiled edge to edge — in the v1 format a sample's delta IS its weight,
     * so untiled gaps would inflate whichever sample follows them. Overlaps
     * (measurement jitter between adjacent spans) clip against the cursor.
     */
    private List<Segment> tiled(List<Segment> segments, long endUs) {
      segments.sort(Comparator.comparingLong(Segment::startUs));
      List<Segment> tiled = new ArrayList<>(segments.size() * 2);
      long cursor = windowStartUs;
      for (Segment segment : segments) {
        long start = Math.max(segment.startUs(), cursor);
        long end = Math.min(segment.endUs(), endUs);
        if (end <= start) continue;
        if (start > cursor) {
          tiled.add(Segment.pseudo(cursor, start, List.of(PseudoFrames.IDLE)));
        }
        tiled.add(new Segment(start, end, segment.samplerStack(), segment.pseudoStack()));
        cursor = end;
      }
      if (endUs > cursor) {
        tiled.add(Segment.pseudo(cursor, endUs, List.of(PseudoFrames.IDLE)));
      }
      return tiled;
    }

    private void writeFrame(PendingWindow window) throws IOException {
      List<WeightedStack> samples = new ArrayList<>(window.segments().size());
      for (Segment segment : window.segments()) {
        samples.add(new WeightedStack(resolvedStack(segment), segment.endUs() - segment.startUs()));
      }
      double endSeconds = window.endUs() / 1_000_000.0;
      long usedBytes = window.usedKb() * 1024;
      long reservedBytes = window.reservedKb() * 1024;
      writer.writeFrame(endSeconds, window.gcUs(), usedBytes, reservedBytes, samples);
    }

    private List<String> resolvedStack(Segment segment) {
      if (segment.pseudoStack() != null) return segment.pseudoStack();
      List<String> stack = new ArrayList<>(segment.samplerStack().length);
      for (int index : segment.samplerStack()) {
        stack.add(samplerName(index));
      }
      return stack;
    }
  }
}
