package com.intellij.plugins.haxe.model.evaluator;

import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.RecursionGuard;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Cross-cache dirtiness tracking. RecursionManager's {@code StackStamp} only
 * sees guards fired on the CURRENT thread's stack, but the call-expression
 * cache serves evaluations computed on other stacks — including
 * guard-truncated ones it deliberately stores (they terminate deep resolve
 * recursion). A consumer of such an entry inherits the truncation while its
 * own stamp stays clean, so "complete" would be judged on truncated data.
 *
 * Any observation of truncated data — a fired prevention, a memoized cycle
 * value, a guard-dirty cache entry — bumps a per-thread counter. Caching
 * boundaries bracket their computation with {@link #mark()} /
 * {@link #taintedSince} and refuse to cache when the counter moved. This is
 * what makes caching FAILED evaluations safe: a failure may only be cached
 * when it is complete on this stack AND built from complete data.
 *
 * The platform's own signal, {@code StackStamp.mayCacheNow()}, cannot be
 * read inside deep evaluation: under the test-mode assertOnMissedCache it
 * THROWS whenever it would return false, and the call-expression cache sits
 * exactly where preventions fire (it caches anyway, by design — the
 * assertion's "you failed to cache" premise does not apply to it).
 */
public final class HaxeEvaluationTaint {

  /** Per-thread mutable state; plain fields since the value is thread-confined. */
  private static final class ThreadState {
    long truncationEvents;
    int guardedDepth;
  }

  private static final ThreadLocal<ThreadState> state = ThreadLocal.withInitial(ThreadState::new);

  private HaxeEvaluationTaint() {
  }

  /** Marks the start of a computation; compare with {@link #taintedSince}. */
  public static long mark() {
    return state.get().truncationEvents;
  }

  /** Records that truncated data was observed on the current thread. */
  public static void taint() {
    state.get().truncationEvents++;
  }

  public static boolean taintedSince(long mark) {
    return state.get().truncationEvents != mark;
  }

  /**
   * True while the current thread is inside a computation running under any
   * guard entered through {@link #computeOrTaint}. In there, a result can be shaped by the held
   * guard keys and the surrounding evaluation context WITHOUT any prevention
   * firing (self-reference bails, skipped scope contributions), so a clean
   * stamp and clean taint do not make a FAILURE trustworthy: the same
   * element can evaluate to Unknown here and to a real type at top level.
   */
  public static boolean insideGuardedComputation() {
    return state.get().guardedDepth > 0;
  }

  /**
   * Runs the computation under the guard, tainting the thread when the
   * caller receives truncated data instead of a fresh computation: a fired
   * prevention (null result — the Ref wrapping makes null unambiguous, a
   * computation that legitimately returns null still yields a non-null Ref)
   * or a MEMOIZED value from an earlier prevented cycle. Memoized reuse must
   * taint even though the original prevention already did: a caching window
   * can open between the prevention and the reuse, and would otherwise judge
   * the memoized (truncated-influenced) data as clean.
   * Guard sites whose computations produce type/resolve data that evaluation
   * can consume go through here, so taint marks see all truncation of that
   * data — not just the sites that happened to remember. Guards protecting
   * navigation/UI-only computations (line marker hierarchy walks etc.) stay
   * on the plain platform calls: their truncation never reaches a caching
   * decision, and counting them in the guard depth would only block failure
   * caching in contexts that cannot shape evaluation results.
   */
  public static <Key, T> @Nullable T computeOrTaint(@NotNull RecursionGuard<Key> guard,
                                                    @NotNull Key key,
                                                    boolean memoize,
                                                    @NotNull Computable<T> computation) {
    AtomicBoolean ran = new AtomicBoolean(false);
    Ref<T> result = guard.doPreventingRecursion(key, memoize, () -> {
      ran.set(true);
      ThreadState threadState = state.get();
      threadState.guardedDepth++;
      try {
        return Ref.create(computation.compute());
      } finally {
        threadState.guardedDepth--;
      }
    });
    if (result == null) {
      taint();
      return null;
    }
    if (!ran.get()) {
      taint();
    }
    return result.get();
  }
}
