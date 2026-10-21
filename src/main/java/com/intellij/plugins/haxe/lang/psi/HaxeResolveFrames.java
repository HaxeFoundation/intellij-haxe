package com.intellij.plugins.haxe.lang.psi;

import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.RecursionGuard;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.openapi.util.Ref;
import com.intellij.plugins.haxe.model.evaluator.HaxeEvaluationTaint;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/// Every resolve runs inside three nested "frames" (RecursionManager guard
/// scopes). This class owns all of them, so HaxeResolver itself reads as
/// plain steps. Nesting order, outermost first:
///
/// ```text
///   cache gate frame            (runCacheGated)
///     ResolveCache frame        (the platform's resolveWithCaching)
///       full pipeline frame     (runFullPipeline)
///         [restricted frame]    (runRestrictedPipeline, re-entry only)
/// ```
///
/// THE CACHE GATE FRAME solves one problem: sometimes a resolve finishes
/// with an answer not trustworthy enough to save (the compute observed
/// truncated data - see [HaxeEvaluationTaint]), and the
/// platform's ResolveCache would otherwise store it until the next PSI
/// change. The platform only skips its cache write when a frame OUTSIDE its
/// own is flagged with `prohibitResultCaching` - flags on frames
/// inside it are invisible to it. So this frame is opened around the
/// platform call purely to be that flaggable outer frame:
/// [#suppressCacheWrite] flags it, the platform skips saving, and the
/// flag disappears when the frame closes so nothing around the resolve is
/// affected.
///
/// THE FULL PIPELINE FRAME solves re-entry detection: some resolver checks
/// evaluate expressions, and those expressions can contain the very
/// reference currently being resolved - resolving it again the normal way
/// would loop. Entering this frame records "this reference is being
/// resolved right now" ([#fullResolveInProgress]), so the resolver
/// can send the inner attempt to the restricted pipeline instead of letting
/// the platform cut it off with a wrong empty answer. Results are never
/// memoized here: callers track whether an answer was computed complete or
/// truncated, and a replayed value would hide that.
///
/// THE RESTRICTED FRAME hosts that inner attempt. It runs the check list
/// minus the expression-evaluating checks (they are what caused the loop),
/// which is enough to still find locals and members by walking the tree.
/// Having its own frame lets a THIRD attempt on the same reference be
/// detected ([#restrictedResolveInProgress]) and stopped.
final class HaxeResolveFrames {

  private final RecursionGuard<PsiElement> fullPipelineGuard = RecursionManager.createGuard("haxeResolveFullPipeline");
  private final RecursionGuard<PsiElement> restrictedPipelineGuard = RecursionManager.createGuard("haxeResolveRestrictedPipeline");
  private final RecursionGuard<PsiElement> cacheGateGuard = RecursionManager.createGuard("haxeResolveCacheGate");

  // Re-entry membership is tracked here, not via RecursionGuard.currentStack():
  // that call walks EVERY guard frame on the thread and allocates a list, far
  // too costly at one check per resolve. The run* brackets push/pop; the
  // checks are O(1) and allocation-free.
  private static final ThreadLocal<Set<HaxeReference>> fullInProgress = identitySet();
  private static final ThreadLocal<Set<HaxeReference>> restrictedInProgress = identitySet();

  private static ThreadLocal<Set<HaxeReference>> identitySet() {
    return ThreadLocal.withInitial(() -> Collections.newSetFromMap(new IdentityHashMap<>()));
  }

  /** A full-pipeline resolve of this reference is already running on this thread. */
  boolean fullResolveInProgress(@NotNull HaxeReference reference) {
    return fullInProgress.get().contains(reference);
  }

  /** A restricted resolve of this reference is already running on this thread. */
  boolean restrictedResolveInProgress(@NotNull HaxeReference reference) {
    return restrictedInProgress.get().contains(reference);
  }

  /**
   * Runs the full check pipeline under its frame. memoize=false: this frame
   * must never serve a memoized resolve result — the caller's certainty
   * accounting needs every resolve computed fresh.
   */
  @Nullable
  List<? extends PsiElement> runFullPipeline(@NotNull HaxeReference reference,
                                             @NotNull Computable<List<? extends PsiElement>> pipeline) {
    Set<HaxeReference> inProgress = fullInProgress.get();
    inProgress.add(reference);
    try {
      return HaxeEvaluationTaint.computeOrTaint(fullPipelineGuard, reference, false, pipeline);
    } finally {
      inProgress.remove(reference);
    }
  }

  /**
   * Runs the restricted pipeline under its own frame so second-level
   * re-entry shows on {@link #restrictedResolveInProgress}. The caller's
   * in-progress pre-check keeps this frame's prevention unreachable; the
   * Ref wrapping makes a null return mean prevention rather than a pipeline
   * that found nothing.
   */
  @Nullable
  List<? extends PsiElement> runRestrictedPipeline(@NotNull HaxeReference reference,
                                                   @NotNull Computable<List<? extends PsiElement>> pipeline) {
    Set<HaxeReference> inProgress = restrictedInProgress.get();
    inProgress.add(reference);
    try {
      Ref<List<? extends PsiElement>> computed = restrictedPipelineGuard.doPreventingRecursion(reference, false, () -> Ref.create(pipeline.get()));
      return computed == null ? null : computed.get();
    } finally {
      inProgress.remove(reference);
    }
  }

  /**
   * Runs the platform-cached resolve inside the gate frame, so
   * {@link #suppressCacheWrite} has a key BELOW ResolveCache's frame to
   * prohibit on. The gate cannot actually prevent — same-reference re-entry
   * is diverted before this frame is pushed.
   */
  @Nullable
  List<? extends PsiElement> runCacheGated(@NotNull HaxeReference reference,
                                           @NotNull Computable<List<? extends PsiElement>> cachedResolve) {
    Ref<List<? extends PsiElement>> gated = cacheGateGuard.doPreventingRecursion(reference, false, () -> Ref.create(cachedResolve.get()));
    return gated == null ? null : gated.get();
  }

  /**
   * Makes the enclosing ResolveCache compute skip its cache write (and the
   * IdempotenceChecker comparison that comes with it): marks the platform's
   * frame through the gate key so its {@code mayCacheNow()} sees the bump.
   * Must be called while still inside {@link #runCacheGated}'s computation.
   * Test-mode note: under assertOnMissedCache the platform then throws a
   * CachingPreventedException naming the gate — that is the assertion
   * observing this INTENDED suppression; the test bases opt out.
   */
  void suppressCacheWrite(@NotNull HaxeReference reference) {
    cacheGateGuard.prohibitResultCaching(reference);
  }
}
