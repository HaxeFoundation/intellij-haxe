package com.intellij.plugins.haxe.profiler.model;

import org.jetbrains.annotations.NotNull;

/** One sampled thread; {@code id} is the target's native thread id. */
public record ProfilerThread(int id, @NotNull String name) {

  /** The name of a capture's main thread when the target reports none. */
  public static final String MAIN_NAME = "Main";

  /** The only thread of a capture that does not distinguish threads. */
  @NotNull
  public static ProfilerThread main() {
    return new ProfilerThread(0, MAIN_NAME);
  }

  /** The display name of a thread the target never named. */
  @NotNull
  public static String unnamed(int threadId) {
    return "Thread " + Integer.toUnsignedString(threadId);
  }
}
