package com.intellij.plugins.haxe.v2.buildtools.libraries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Runs an asynchronous action one run at a time. Requests arriving while a
 * run is in progress fold into ONE follow-up run, started when the current
 * run signals completion - a burst of triggers costs one extra run. A
 * request's callback runs once a run that started after the request has
 * completed, so that run saw the state the request was made in.
 */
final class HaxeCoalescingRunner {

  /** Starts one run; the run calls the handed Runnable exactly once when it has completed. */
  private final Consumer<Runnable> startRun;

  // guarded by this: a run in progress, and the callbacks of the requests
  // that arrived during it (non-null = a follow-up run is wanted)
  private boolean running;
  private List<Runnable> pending;

  HaxeCoalescingRunner(@NotNull Consumer<Runnable> startRun) {
    this.startRun = startRun;
  }

  void request(@Nullable Runnable onFinished) {
    List<Runnable> callbacks;
    synchronized (this) {
      if (running) {
        if (pending == null) pending = new ArrayList<>();
        if (onFinished != null) pending.add(onFinished);
        return;
      }
      running = true;
      callbacks = onFinished == null ? List.of() : List.of(onFinished);
    }
    start(callbacks);
  }

  private void start(@NotNull List<Runnable> callbacks) {
    startRun.accept(() -> finish(callbacks));
  }

  private void finish(@NotNull List<Runnable> callbacks) {
    callbacks.forEach(Runnable::run);
    List<Runnable> followUp;
    synchronized (this) {
      followUp = pending;
      pending = null;
      running = followUp != null;
    }
    if (followUp != null) start(followUp);
  }
}
