package com.intellij.plugins.haxe.profiler.js;

import com.intellij.plugins.haxe.profiler.hxt.HxtSessionWriter;
import com.intellij.plugins.haxe.profiler.hxt.HxtSessionWriter.WeightedStack;
import com.intellij.plugins.haxe.profiler.js.CpuProfileTranslator.NodeTree;
import com.intellij.plugins.haxe.profiler.model.ProfilerFormatException;
import com.intellij.plugins.haxe.profiler.model.StackFrame;
import com.intellij.plugins.haxe.profiler.model.PseudoFrames;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds an HXTS v1 session from successive V8 profile SEGMENTS — the
 * results of periodic {@code Profiler.stop}/{@code start} cycles over one
 * browser session. Each segment appends flushed records, so the live view
 * follows the run and a browser closed mid-run keeps every segment already
 * collected (only the in-flight one is lost).
 *
 * A segment's window splits at the DISPLAY FRAMES the caller observed
 * (CDP Tracing's DrawFrame instants, on the SAME microsecond clock as the
 * profile) — those records chart as real frames, and each carries the
 * segment's heap reading when one was polled. The tail up to the flush
 * boundary is marked a collection window: not a frame. Without observed
 * frames a segment is one such window.
 *
 * Segments share the target's microsecond clock ({@code startTime}/
 * {@code endTime}); the first segment's start rebases the session to zero.
 * V8's synthetic nodes map to the chart's pseudo-frames: {@code (idle)}
 * and {@code (program)} become [idle], {@code (garbage collector)} becomes
 * [gc] and counts into its window's GC time. Positions map back to the
 * .hx sources through {@link JsSourceMap} when the caller supplies maps;
 * unmapped frames keep the generated file's own position. Either way the
 * position travels in the v1 name convention {@code symbol(file:line)}.
 */
public final class CpuProfileSessionBuilder {

  /** Source maps for the scripts samples land in; null or a null result leaves that script's positions unmapped. */
  public interface SourceMaps {
    @Nullable
    JsSourceMap forScript(@NotNull String scriptUrl);
  }

  private final HxtSessionWriter writer;
  private final ObjectMapper mapper = new ObjectMapper();
  private final SourceMaps sourceMaps;
  /** The first segment's startTime; every stamp is relative to it. */
  private long baseUs = -1;
  private long lastStampUs;
  /** Rebased frame stamps observed past a segment's flush boundary; they cut the NEXT segment's gap. */
  private final List<Long> carriedFrameStampsUs = new ArrayList<>();

  public CpuProfileSessionBuilder(@NotNull OutputStream out) {
    this(out, null);
  }

  public CpuProfileSessionBuilder(@NotNull OutputStream out, @Nullable SourceMaps sourceMaps) {
    writer = new HxtSessionWriter(out, CpuProfileTranslator.TARGET);
    this.sourceMaps = sourceMaps;
  }

  public long bytesWritten() {
    return writer.bytesWritten();
  }

  /** A segment without observed frames or a heap reading: one collection-window record. */
  public void appendSegment(@NotNull String profileJson) throws IOException {
    appendSegment(profileJson, List.of(), 0, 0);
  }

  /**
   * Appends one {@code Profiler.stop} result's profile object, split at
   * {@code frameStampsUs} (display-frame instants on the profile's own
   * clock, unrebased); {@code heapUsedBytes}/{@code heapTotalBytes} ride
   * every record of the segment — pass 0 totals when nothing was polled.
   */
  public void appendSegment(@NotNull String profileJson, @NotNull List<Long> frameStampsUs,
                            long heapUsedBytes, long heapTotalBytes) throws IOException {
    JsonNode profile;
    try {
      profile = mapper.readTree(profileJson);
    }
    catch (JacksonException malformed) {
      throw new ProfilerFormatException("not a cpuprofile segment: " + malformed.getMessage());
    }
    NodeTree tree = NodeTree.of(profile, this::resolveFrame);
    JsonNode samples = profile.path("samples");
    JsonNode deltas = profile.path("timeDeltas");
    long startUs = profile.path("startTime").asLong(0);
    if (baseUs < 0) {
      baseUs = startUs;
      lastStampUs = 0;
    }

    List<WeightedStack> weighted = new ArrayList<>(samples.size() + 1);
    // the stop-to-start gap between segments is charted as idle, so the
    // next segment's first sample cannot inherit it (delta IS weight)
    long gapUs = startUs - baseUs - lastStampUs;
    if (gapUs > 0) {
      weighted.add(new WeightedStack(List.of(PseudoFrames.IDLE), gapUs));
    }

    long elapsedUs = 0;
    for (int i = 0; i < samples.size(); i++) {
      long weight = Math.max(deltas.path(i).asLong(0), 1);
      elapsedUs += weight;
      int nodeId = samples.path(i).asInt(-1);
      String function = tree.functionOf(nodeId);
      if (CpuProfileTranslator.isIdleNode(function)) {
        weighted.add(new WeightedStack(List.of(PseudoFrames.IDLE), weight));
        continue;
      }
      if (CpuProfileTranslator.GC_NODE.equals(function)) {
        weighted.add(new WeightedStack(List.of(PseudoFrames.GC), weight));
        continue;
      }
      List<StackFrame> stack = tree.stackOf(nodeId);
      if (stack.isEmpty()) {
        weighted.add(new WeightedStack(List.of(PseudoFrames.IDLE), weight));
        continue;
      }
      weighted.add(new WeightedStack(names(stack), weight));
    }

    // endTime when present, else the samples' extent; never backward, since
    // the v1 window chain needs monotonic stamps
    long stampUs = Math.max(profile.path("endTime").asLong(0) - baseUs, startUs - baseUs + elapsedUs);
    stampUs = Math.max(stampUs, lastStampUs);

    int heapFlags = heapTotalBytes > 0 ? 0 : HxtSessionWriter.FLAG_NO_HEAP_READING;
    writeWindows(weighted, frameBoundaries(frameStampsUs, stampUs), stampUs, heapUsedBytes, heapTotalBytes, heapFlags);
    lastStampUs = stampUs;
  }

  /** The rebased frame stamps cutting this segment, oldest first; seam frames past the flush boundary carry over. */
  private List<Long> frameBoundaries(List<Long> frameStampsUs, long segmentStampUs) {
    List<Long> boundaries = new ArrayList<>(carriedFrameStampsUs);
    carriedFrameStampsUs.clear();
    for (long stamp : frameStampsUs) {
      boundaries.add(stamp - baseUs);
    }
    boundaries.sort(Long::compare);
    List<Long> inWindow = new ArrayList<>(boundaries.size());
    for (long boundary : boundaries) {
      if (boundary <= lastStampUs) continue; // stale - already behind the window chain
      if (boundary >= segmentStampUs) {
        carriedFrameStampsUs.add(boundary);
      }
      else {
        inWindow.add(boundary);
      }
    }
    return inWindow;
  }

  /**
   * Walks the segment's weighted samples along the window chain, closing a
   * record at every frame boundary (a sample straddling one splits, so
   * windows tile exactly) and a final collection-window record at the
   * segment's flush boundary.
   */
  private void writeWindows(List<WeightedStack> weighted, List<Long> frameBoundaries, long segmentStampUs,
                            long heapUsedBytes, long heapTotalBytes, int heapFlags) throws IOException {
    long cursor = lastStampUs;
    int nextBoundary = 0;
    List<WeightedStack> window = new ArrayList<>();
    long windowGcUs = 0;
    for (WeightedStack sample : weighted) {
      long remaining = sample.weightUs();
      while (nextBoundary < frameBoundaries.size() && cursor + remaining >= frameBoundaries.get(nextBoundary)) {
        long boundary = frameBoundaries.get(nextBoundary);
        long head = boundary - cursor;
        if (head > 0) {
          window.add(new WeightedStack(sample.rootFirstStack(), head));
          if (isGcStack(sample)) windowGcUs += head;
        }
        writer.writeFrame(boundary / 1_000_000.0, windowGcUs, heapUsedBytes, heapTotalBytes, window, heapFlags);
        window = new ArrayList<>();
        windowGcUs = 0;
        cursor = boundary;
        remaining -= head;
        nextBoundary++;
      }
      if (remaining > 0) {
        window.add(new WeightedStack(sample.rootFirstStack(), remaining));
        if (isGcStack(sample)) windowGcUs += remaining;
        cursor += remaining;
      }
    }
    writer.writeFrame(segmentStampUs / 1_000_000.0, windowGcUs, heapUsedBytes, heapTotalBytes, window,
                      heapFlags | HxtSessionWriter.FLAG_SEGMENT_WINDOW);
  }

  private static boolean isGcStack(WeightedStack sample) {
    return sample.rootFirstStack().size() == 1 && PseudoFrames.GC.equals(sample.rootFirstStack().get(0));
  }

  /** The plain frame, or the source-mapped one when a map covers the sampled position. */
  private StackFrame resolveFrame(String functionName, String url, int line0, int column0) {
    StackFrame plain = CpuProfileTranslator.frameOf(functionName, url, line0, column0);
    StackFrame frame = plain;
    JsSourceMap map = sourceMaps != null && !url.isEmpty() ? sourceMaps.forScript(url) : null;
    if (map != null && line0 >= 0) {
      JsSourceMap.Position mapped = map.resolve(line0, Math.max(column0, 0));
      if (mapped != null) {
        frame = new StackFrame(qualified(plain.symbol(), mapped.file()), mapped.file(), mapped.line());
      }
    }
    String symbol = demangle(frame.symbol());
    // no script at all = a browser-native call (canvas, DOM, WebGL): an
    // extern's target has no position to map, so say so rather than read
    // as a failed mapping; V8's own (...) names stay untouched
    if (url.isEmpty() && !symbol.startsWith("(")) {
      symbol += " (native)";
    }
    return new StackFrame(symbol, frame.file(), frame.line());
  }

  /**
   * A bare method name gains its .hx module for identity: V8 names
   * prototype methods by property alone, and two files' same-named
   * methods must not read (or merge) as one.
   */
  private static String qualified(String symbol, String sourceFile) {
    if (symbol.indexOf('.') >= 0) return symbol;
    String fileName = sourceFile.substring(sourceFile.lastIndexOf('/') + 1);
    if (!fileName.endsWith(".hx")) return symbol;
    return fileName.substring(0, fileName.length() - ".hx".length()) + "." + symbol;
  }

  /**
   * Haxe's js generator writes a type's dotted path with {@code _} as the
   * separator and marks a segment's own leading underscore with a following
   * {@code $} ({@code pack_sub__$internal_Shape} is
   * {@code pack.sub._internal.Shape}). Only pieces that start lowercase (a
   * package root) and carry an underscore are touched, so plain method names
   * like {@code __update} pass through.
   */
  static String demangle(String name) {
    StringBuilder result = new StringBuilder(name.length());
    // the pieces of V8's dotted "Type.method" inferred names
    for (String piece : name.split("\\.", -1)) {
      if (result.length() > 0) result.append('.');
      // a mangled type path: lowercase start, at least one underscore
      if (piece.matches("[a-z][a-zA-Z0-9$]*_[a-zA-Z0-9_$]*")) {
        // an underscore NOT followed by $ separates segments; _$ keeps its
        // underscore (the segment's own) and drops the marker
        result.append(piece.replaceAll("_(?!\\$)", ".").replace("_$", "_"));
      }
      else {
        result.append(piece);
      }
    }
    return result.toString();
  }

  private static List<String> names(List<StackFrame> stack) {
    List<String> names = new ArrayList<>(stack.size());
    for (StackFrame frame : stack) {
      // the v1 name convention the reader parses positions back out of
      names.add(frame.file() != null && frame.line() != StackFrame.NO_LINE
                ? frame.symbol() + "(" + frame.file() + ":" + frame.line() + ")"
                : frame.symbol());
    }
    return names;
  }
}
