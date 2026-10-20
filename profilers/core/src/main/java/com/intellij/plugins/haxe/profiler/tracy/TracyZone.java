package com.intellij.plugins.haxe.profiler.tracy;

import org.jetbrains.annotations.NotNull;

/**
 * One completed instrumented run: exact begin/end (nanoseconds relative to
 * the session start) on one thread. Nesting is implied by containment —
 * zones on a thread never partially overlap.
 */
public record TracyZone(int threadId, long startNs, long endNs, @NotNull TracySourceLocation location) {

  public long durationNs() {
    return endNs - startNs;
  }
}
