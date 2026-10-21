package com.intellij.plugins.haxe.profiler.model;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * The bracketed pseudo-frame names a capture carries where no application
 * function ran. The transcoders emit the named constants for time their
 * source reports without a stack; the runtimes' collectors emit their own
 * spellings for GC phases and event-loop waits, which the predicates cover.
 */
public final class PseudoFrames {

  public static final String IDLE = "[idle]";
  public static final String GC = "[gc]";
  public static final String RENDER = "[render]";
  public static final String SCRIPT = "[script]";

  /** Idle stretches, collector overhead marks and the samplers' event-loop pseudo-frames. */
  private static final Set<String> NOT_APPLICATION_CODE = Set.of(IDLE, "[io]", "[execute-queued]", "[profiler]");

  private PseudoFrames() {
  }

  /** Whether a stack ROOTED here means the thread was not executing application code. */
  public static boolean isIdle(@NotNull String symbol) {
    return NOT_APPLICATION_CODE.contains(symbol);
  }

  /** GC work however a collector spells it: the pseudo-frames, or hxcpp's own GC entry points sitting mid-stack. */
  public static boolean isGc(@NotNull String symbol) {
    return GC.equals(symbol) || "[mark]".equals(symbol) || "[sweep]".equals(symbol) || symbol.startsWith("GC::");
  }
}
