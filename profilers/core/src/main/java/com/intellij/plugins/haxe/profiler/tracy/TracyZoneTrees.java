package com.intellij.plugins.haxe.profiler.tracy;

import com.intellij.plugins.haxe.profiler.model.StackFrame;
import com.intellij.plugins.haxe.profiler.timeline.ProfilerTimeline;
import com.intellij.plugins.haxe.profiler.timeline.ProfilerTimeline.FlameNode;
import com.intellij.plugins.haxe.profiler.timeline.ProfilerTimeline.UsSpan;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Chart projections of a zone capture. Zones are EXACT intervals, so the
 * flame tree needs no sampling reconstruction: nesting is containment, and
 * every box width is a measured duration. Times convert from the session's
 * nanoseconds to the charts' microsecond axis.
 */
public final class TracyZoneTrees {

  private TracyZoneTrees() {
  }

  /** Builds the flame tree from one thread's START-ORDERED zones (windowed loads sort before calling). */
  @NotNull
  public static FlameNode treeFromOrdered(@NotNull List<TracyZone> ordered, long durationNs) {
    Builder root = new Builder(null, 0, Math.max(durationNs / 1000, 1));
    Deque<Builder> open = new ArrayDeque<>();
    for (TracyZone zone : ordered) {
      long startUs = zone.startNs() / 1000;
      long endUs = Math.max(zone.endNs() / 1000, startUs + 1);
      while (!open.isEmpty() && startUs >= open.peek().endUs) {
        open.pop();
      }
      Builder parent = open.isEmpty() ? root : open.peek();
      // exact data can still collide on the µs grid; clamp inside the parent
      long clampedStartUs = Math.max(startUs, parent.startUs);
      long clampedEndUs = Math.min(endUs, parent.endUs);
      Builder child = new Builder(frameOf(zone.location()), clampedStartUs, clampedEndUs);
      parent.children.add(child);
      open.push(child);
    }
    return root.freeze();
  }

  /**
   * Consecutive frame marks as frame spans for the Frames lane. Marks come
   * from hxcpp's telemetry frame hook, which frameworks drive once per
   * frame (lime/openfl do); a plain hxcpp run has no caller and its lane
   * stays empty.
   */
  @NotNull
  public static List<UsSpan> frameSpans(@NotNull TracySession session) {
    List<UsSpan> spans = new ArrayList<>();
    Long previous = null;
    for (long markNs : session.frameMarksNs()) {
      if (previous != null && markNs > previous) {
        spans.add(new UsSpan(previous / 1000, markNs / 1000));
      }
      previous = markNs;
    }
    return spans;
  }

  @NotNull
  private static StackFrame frameOf(TracySourceLocation location) {
    String file = location.file().isEmpty() ? null : location.file();
    return new StackFrame(location.function(), file, location.line() > 0 ? location.line() : StackFrame.NO_LINE);
  }

  /** A zone's node under construction; one measured run, so every node counts one sample. */
  private static final class Builder {
    final StackFrame frame;
    final long startUs;
    final long endUs;
    final List<Builder> children = new ArrayList<>();

    Builder(StackFrame frame, long startUs, long endUs) {
      this.frame = frame;
      this.startUs = startUs;
      this.endUs = endUs;
    }

    FlameNode freeze() {
      List<FlameNode> frozenChildren = children.stream().map(Builder::freeze).toList();
      return ProfilerTimeline.flameNode(frame, startUs, endUs, 1, frozenChildren);
    }
  }
}
