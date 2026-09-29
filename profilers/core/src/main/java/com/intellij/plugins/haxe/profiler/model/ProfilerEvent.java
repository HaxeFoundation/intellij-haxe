package com.intellij.plugins.haxe.profiler.model;

import org.jetbrains.annotations.NotNull;

/**
 * An event on the capture's time axis — emitted by the profiled application
 * (HashLink's {@code hl.Profile.event}) or synthesized by a translator
 * (telemetry frames). {@code code} follows one cross-format convention:
 * {@link #FRAME_CODE} marks an end of frame, {@link #GC_TIME_CODE} carries a
 * frame's GC time; other codes are application-defined.
 *
 * @param time seconds since the target's own capture epoch
 */
public record ProfilerEvent(double time, int threadId, int code, @NotNull String data) {

  /** End-of-frame marker; the Frames lane and frame-duration series read these. */
  public static final int FRAME_CODE = 0;

  /** A frame's total GC time; {@code data} carries the microseconds as a decimal string. */
  public static final int GC_TIME_CODE = 1;
}
