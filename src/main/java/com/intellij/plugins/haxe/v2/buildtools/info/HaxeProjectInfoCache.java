package com.intellij.plugins.haxe.v2.buildtools.info;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The caching/scheduling skeleton of the project info services
 * ({@link HaxeLimeProjectInfoService}, {@link HaxeNmeProjectInfoService}):
 * evaluations cached per (file, target, toolchain) key and run on a bounded
 * single-thread executor. Callers get the cached value (possibly stale,
 * possibly null on first ask) immediately and a callback once a refresh
 * lands. A value leaving the cache (replaced, invalidated, cleared) passes
 * through the evict hook.
 */
final class HaxeProjectInfoCache<V> {

  // unsettled evaluations retry this many times (per file revision) before
  // the result is accepted as final - keeps transient tool failures from
  // sticking without allowing refresh loops
  private static final int MAX_ATTEMPTS = 3;

  record Key(@NotNull String filePath, @NotNull String targetFlag, @NotNull String haxelibPath) {
  }

  /** One evaluation's result; {@code settled} = final for the file revision, never re-attempted. */
  record Outcome<V>(@Nullable V value, boolean settled) {
  }

  private record CacheValue<V>(long modificationStamp, @Nullable V value, boolean settled, int attempts) {
  }

  private final Project project;
  private final Consumer<V> onEvict;

  private final Map<Key, CacheValue<V>> cache = new ConcurrentHashMap<>();
  private final Set<Key> inFlight = ConcurrentHashMap.newKeySet();

  // every caller waiting on an in-flight evaluation gets its callback fired
  private final Map<Key, List<Runnable>> pendingCallbacks = new ConcurrentHashMap<>();
  private final ExecutorService executor;

  HaxeProjectInfoCache(@NotNull Project project, @NotNull String executorName, @Nullable Consumer<V> onEvict) {
    this.project = project;
    this.onEvict = onEvict != null ? onEvict : ignored -> { };
    this.executor = AppExecutorUtil.createBoundedApplicationPoolExecutor(executorName, 1);
  }

  /**
   * The key's cached value, or null when not resolved (yet). Schedules the
   * evaluation on the executor when the cache is stale; {@code onUpdated}
   * fires on the EDT after the run completes.
   */
  @Nullable
  V getCachedOrSchedule(@NotNull Key key,
                        long stamp,
                        @NotNull Supplier<Outcome<V>> evaluation,
                        @NotNull Runnable onUpdated) {

    CacheValue<V> cached = cache.get(key);
    boolean fresh = cached != null && cached.modificationStamp() == stamp;

    if (fresh && (cached.settled() || cached.attempts() >= MAX_ATTEMPTS)) {
      return cached.value();
    }

    schedule(key, stamp, fresh ? cached.attempts() : 0, evaluation, onUpdated);
    // serve the stale value while the refresh runs
    return cached != null ? cached.value() : null;
  }

  /** Drops every cached evaluation of one build file (all targets/toolchains), forcing a re-run on the next ask. */
  void invalidate(@NotNull String filePath) {
    cache.entrySet().removeIf(entry -> {
      boolean matches = entry.getKey().filePath().equals(filePath);
      if (matches) {
        evict(entry.getValue());
      }
      return matches;
    });
  }

  void clear() {
    cache.values().forEach(this::evict);
    cache.clear();
  }

  void shutdown() {
    executor.shutdownNow();
    clear();
  }

  private void schedule(@NotNull Key key,
                        long stamp,
                        int previousAttempts,
                        @NotNull Supplier<Outcome<V>> evaluation,
                        @NotNull Runnable onUpdated) {
    pendingCallbacks.computeIfAbsent(key, ignored -> new CopyOnWriteArrayList<>()).add(onUpdated);
    if (!inFlight.add(key)) {
      // an evaluation is already running; it fires the callback registered above
      return;
    }

    executor.execute(() -> {
      List<Runnable> callbacks;
      try {
        Outcome<V> outcome = evaluation.get();
        CacheValue<V> replaced = cache.put(key, new CacheValue<>(stamp, outcome.value(), outcome.settled(), previousAttempts + 1));
        evict(replaced);
      }
      finally {
        // callbacks drained BEFORE the in-flight flag drops: an ask arriving in
        // between re-registers and starts a fresh evaluation of its own
        callbacks = pendingCallbacks.remove(key);
        inFlight.remove(key);
      }
      List<Runnable> toRun = callbacks != null ? callbacks : List.of();
      ApplicationManager.getApplication().invokeLater(() -> {
        if (!project.isDisposed()) {
          toRun.forEach(Runnable::run);
        }
      });
    });
  }

  private void evict(@Nullable CacheValue<V> value) {
    if (value != null && value.value() != null) {
      onEvict.accept(value.value());
    }
  }

  /** The first stderr line of a failed tool run, or its exit code - log material. */
  @NotNull
  static String firstErrorLine(@NotNull ProcessOutput output) {
    return output.getStderr().lines().findFirst().orElse("exit code " + output.getExitCode());
  }
}
