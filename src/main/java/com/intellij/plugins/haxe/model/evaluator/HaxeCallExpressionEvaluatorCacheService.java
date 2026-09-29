package com.intellij.plugins.haxe.model.evaluator;

import com.intellij.plugins.haxe.lang.psi.HaxeCallExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.model.evaluator.callexpression.HaxeCallExpressionContext;
import com.intellij.plugins.haxe.model.evaluator.callexpression.HaxeCallExpressionContextContainer;
import com.intellij.plugins.haxe.model.evaluator.callexpression.HaxeCallExpressionEvaluation;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import lombok.CustomLog;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static com.intellij.plugins.haxe.model.evaluator.callexpression.HaxeCallExpressionUtil.createContextForMethodCall;

/**
 * Avoids unnecessary re-evaluation of callExpressions
 */
@CustomLog
public class HaxeCallExpressionEvaluatorCacheService  {

  private volatile  Map<CallExpressionEvaluationKey, HaxeCallExpressionEvaluation> cacheMap = new ConcurrentHashMap<>();
  // usage-based parameter inference evaluates the call with a HOLE at the
  // argument being typed, so those evaluations key on the hole index too
  private volatile Map<CallExpressionHoleKey, HoleEvaluation> holeCacheMap = new ConcurrentHashMap<>();
  public static boolean skipCaching = false;// just convenience flag for debugging

  // WHY THE FOLLOWING LIMITS EXIST
  //
  // Evaluating a call can indirectly start evaluating the SAME call again
  // before the first evaluation has finished: typing a call evaluates its
  // arguments, and typing an argument may ask which parameter it lands in,
  // which evaluates the enclosing call again. Each round uses fresh helper
  // objects, so the platform's recursion guards (which only detect an exact
  // repeat of the same object) never notice. Unlimited, this nesting grows
  // until the thread runs out of stack, which aborts the analysis and
  // corrupts the platform's recursion bookkeeping.
  //
  // AT THE LIMIT the newest attempt does not run: it returns null and taints
  // the thread (see HaxeEvaluationTaint), so nothing built on the missing
  // answer is stored as final.
  //
  // PER_KEY limits how deeply ONE call may nest inside its own evaluation.
  // Some nesting is needed (typing an argument through its enclosing call,
  // overload selection on top); below 4, extern overloads and untyped
  // parameters come out wrong or missing. 8 leaves headroom above that.
  //
  // TOTAL limits how many call evaluations of ANY kind nest on one thread:
  // the stack-overflow protection, and a performance guard where many calls
  // chain into each other, since deeper re-evaluation learns nothing the
  // shallower rounds are not already computing. Too low cuts evaluations
  // before they produce anything storable, so the same work repeats; 16 is
  // the fastest setting on recursion-heavy std code
  // (HaxeRecursiveStdInferenceTest).
  //
  // The in-flight counter also answers anyComputeInFlight(): deep inside
  // call evaluation versus a cheap top-level query.
  private static final int MAX_IN_FLIGHT_PER_KEY = 8;
  private static final int MAX_IN_FLIGHT_TOTAL = 16;

  private static final ThreadLocal<Map<Object, Integer>> inFlight = ThreadLocal.withInitial(HashMap::new);
  // per-thread (inside the ThreadLocal), so no atomicity is needed - just a mutable int
  private static final ThreadLocal<MutableInt> inFlightTotal = ThreadLocal.withInitial(MutableInt::new);

  // DIRTY-ENTRY REFRESH: a dirty entry is served-with-taint, but at TOP
  // LEVEL (no compute in flight, full budget free) a fresh compute may now
  // succeed where the stored one was truncated. The thrash bound is the
  // settled-information stamp: a dirty entry cannot improve unless new type
  // information settled after it was computed, so storing a dirty entry (or
  // attempting its refresh) records the current stamp and further top-level
  // reads serve it unchanged until the stamp advances. The stamp advances
  // when an untyped-parameter binding settles (HaxeUntypedParameterInference)
  // and everything resets with the PSI-change cache clear.
  private static final AtomicLong settledInfoStamp = new AtomicLong();
  private final Map<Object, Long> refreshAttempts = new ConcurrentHashMap<>();

  public static void informationSettled() {
    settledInfoStamp.incrementAndGet();
  }

  /** The current settled-information stamp. A dirty entry in any evaluator cache is worth recomputing at most once per value. */
  public static long settledInfoStamp() {
    return settledInfoStamp.longValue();
  }

  /** Records the attempt: at most one refresh per entry per stamp value. */
  private boolean refreshArmed(Object key) {
    if (anyComputeInFlight()) return false;
    long stamp = settledInfoStamp.longValue();
    Long lastAttempt = refreshAttempts.get(key);
    if (lastAttempt != null && lastAttempt == stamp) return false;
    refreshAttempts.put(key, stamp);
    return true;
  }

  public static @Nullable HaxeCallExpressionEvaluation cachedHaxeCallExpressionEvaluation(HaxeMethod method, HaxeCallExpression callExpression) {

    HaxeCallExpressionEvaluatorCacheService service = method.getProject().getService(HaxeCallExpressionEvaluatorCacheService.class);
    return  service.callExpressionCachedEvaluation(method, callExpression);

  }


  public @Nullable HaxeCallExpressionEvaluation callExpressionCachedEvaluation(HaxeMethod method, HaxeCallExpression callExpression) {

    if(skipCaching){
      HaxeCallExpressionContextContainer contextContainer = createContextForMethodCall(callExpression, method);
      return contextContainer.evaluateContexts();
    }

    CallExpressionEvaluationKey key = new CallExpressionEvaluationKey(method, callExpression);
    HaxeCallExpressionEvaluation cached = cacheMap.get(key);
    if (cached != null) {
      if (cached.isComputedWithGuardFired()) {
        HaxeCallExpressionEvaluation refreshed = refreshedCallEvaluation(key, method, callExpression);
        if (refreshed != null) return refreshed;
        HaxeEvaluationTaint.taint();
      }
      return cached;
    }

    ComputedCall computed = computeCallEvaluationInFlight(key, method, callExpression);
    if (computed == null) return null;
    // Deliberately NOT gated on the platform's mayCacheNow(): this cache
    // is load-bearing for termination, not just speed. Resolving one
    // reference can require evaluating a call, whose arguments resolve
    // further references, which evaluate further calls, and anything that
    // resolves many references in a row (refactorings, usage searches) sends
    // such chains very deep. The cache hit, including an entry computed
    // while a recursion guard had fired, is what stops a chain from growing;
    // without it the stack overflows. Staleness is bounded by the PSI-change
    // listener clearing the cache. The DIRTY flag travels with each entry so
    // consumers taint instead of trusting it as complete, and entries
    // containing Unknown types are stored too: in files whose types never
    // settle they are the ONLY entries, and refusing them means every
    // reference rebuilds the same call context on every pass.
    if (computed.storable()) {
      storeCallEvaluation(key, computed.evaluation());
    }
    return computed.evaluation();
  }

  private record ComputedCall(HaxeCallExpressionEvaluation evaluation, boolean storable) {}

  private @Nullable ComputedCall computeCallEvaluationInFlight(CallExpressionEvaluationKey key,
                                                               HaxeMethod method,
                                                               HaxeCallExpression callExpression) {
    return computeWithinInFlightBudget(key, () -> computeCallEvaluation(method, callExpression));
  }

  private static @Nullable ComputedCall computeCallEvaluation(HaxeMethod method, HaxeCallExpression callExpression) {
    // the mark answers "did anything this compute depends on come out
    // truncated?"; the platform's StackStamp cannot (see HaxeEvaluationTaint)
    long taintMark = HaxeEvaluationTaint.mark();
    HaxeCallExpressionContextContainer contextContainer = createContextForMethodCall(callExpression, method);
    HaxeCallExpressionEvaluation evaluate = contextContainer.evaluateContexts();
    if (evaluate == null) return null;

    HaxeCallExpressionContext context = contextContainer.getContext();
    evaluate.setComputedWithGuardFired(isDirty(taintMark, evaluate, context));
    boolean storable = evaluate.isValid() && evaluate.isCompleted() && context != null && context.canCache;
    return new ComputedCall(evaluate, storable);
  }

  /** Owns the in-flight enter / two-limit check / release around one compute; refusal taints and returns null. */
  private static <T> @Nullable T computeWithinInFlightBudget(Object key, Supplier<T> compute) {
    Map<Object, Integer> inProgress = inFlight.get();
    MutableInt total = inFlightTotal.get();

    int depth = inProgress.merge(key, 1, Integer::sum);
    total.increment();

    if (depth > MAX_IN_FLIGHT_PER_KEY || total.intValue() > MAX_IN_FLIGHT_TOTAL) {
      releaseInFlight(inProgress, key);
      total.decrement();
      HaxeEvaluationTaint.taint();
      return null;
    }

    try {
      return compute.get();
    } finally {
      releaseInFlight(inProgress, key);
      total.decrement();
    }
  }

  private void storeCallEvaluation(CallExpressionEvaluationKey key, HaxeCallExpressionEvaluation evaluation) {
    cacheMap.put(key, evaluation);
    if (evaluation.isComputedWithGuardFired()) {
      refreshAttempts.put(key, settledInfoStamp.longValue());
    } else {
      refreshAttempts.remove(key);
    }
  }

  /** Clean beats dirty; a still-dirty recompute keeps the cached entry. */
  private @Nullable HaxeCallExpressionEvaluation refreshedCallEvaluation(CallExpressionEvaluationKey key,
                                                                         HaxeMethod method,
                                                                         HaxeCallExpression callExpression) {
    if (!refreshArmed(key)) return null;
    ComputedCall fresh = computeCallEvaluationInFlight(key, method, callExpression);
    boolean clean = fresh != null && fresh.storable() && !fresh.evaluation().isComputedWithGuardFired();
    if (!clean) return null;
    storeCallEvaluation(key, fresh.evaluation());
    return fresh.evaluation();
  }

  private static void releaseInFlight(Map<Object, Integer> inProgress, Object key) {
    inProgress.merge(key, -1, (a, b) -> a + b <= 0 ? null : a + b);
  }

  /**
   * True while the current thread is computing any call-expression context.
   * The precise "inside a deep evaluation tower" signal: cheaper queries
   * (inlay providers evaluating a declaration) never set it, while resolve
   * storms and call evaluations always do.
   */
  public static boolean anyComputeInFlight() {
    return inFlightTotal.get().intValue() > 0;
  }

  public static @Nullable HoleEvaluation cachedHoleEvaluation(HaxeMethod method, HaxeCallExpression callExpression, int holeArgumentIndex) {
    HaxeCallExpressionEvaluatorCacheService service = method.getProject().getService(HaxeCallExpressionEvaluatorCacheService.class);
    return service.holeEvaluation(method, callExpression, holeArgumentIndex);
  }

  private @Nullable HoleEvaluation holeEvaluation(HaxeMethod method, HaxeCallExpression callExpression, int holeArgumentIndex) {
    if (skipCaching) {
      return computeHoleEvaluation(method, callExpression, holeArgumentIndex);
    }

    CallExpressionHoleKey key = new CallExpressionHoleKey(method, callExpression, holeArgumentIndex);
    HoleEvaluation cached = holeCacheMap.get(key);
    if (cached != null) {
      if (cached.dirty()) {
        HoleEvaluation refreshed = refreshedHoleEvaluation(key, method, callExpression, holeArgumentIndex);
        if (refreshed != null) return refreshed;
        HaxeEvaluationTaint.taint();
      }
      return cached;
    }

    HoleEvaluation result = computeHoleEvaluationInFlight(key, method, callExpression, holeArgumentIndex);
    if (result != null && holeStorable(result)) {
      storeHoleEvaluation(key, result);
    }
    return result;
  }

  private @Nullable HoleEvaluation computeHoleEvaluationInFlight(CallExpressionHoleKey key,
                                                                 HaxeMethod method,
                                                                 HaxeCallExpression callExpression,
                                                                 int holeArgumentIndex) {
    return computeWithinInFlightBudget(key, () -> computeHoleEvaluation(method, callExpression, holeArgumentIndex));
  }

  private static boolean holeStorable(HoleEvaluation result) {
    return result.evaluation().isValid()
        && result.evaluation().isCompleted()
        && result.canCache();
  }

  private void storeHoleEvaluation(CallExpressionHoleKey key, HoleEvaluation result) {
    holeCacheMap.put(key, result);
    if (result.dirty()) {
      refreshAttempts.put(key, settledInfoStamp.longValue());
    } else {
      refreshAttempts.remove(key);
    }
  }

  /** Clean beats dirty; a still-dirty recompute keeps the cached entry. */
  private @Nullable HoleEvaluation refreshedHoleEvaluation(CallExpressionHoleKey key,
                                                           HaxeMethod method,
                                                           HaxeCallExpression callExpression,
                                                           int holeArgumentIndex) {
    if (!refreshArmed(key)) return null;

    HoleEvaluation fresh = computeHoleEvaluationInFlight(key, method, callExpression, holeArgumentIndex);
    boolean clean = fresh != null && !fresh.dirty() && holeStorable(fresh);
    if (!clean) return null;

    storeHoleEvaluation(key, fresh);
    return fresh;
  }

  private static @Nullable HoleEvaluation computeHoleEvaluation(HaxeMethod method, HaxeCallExpression callExpression, int holeArgumentIndex) {
    long taintMark = HaxeEvaluationTaint.mark();
    HaxeCallExpressionContextContainer container = createContextForMethodCall(callExpression, method, holeArgumentIndex);
    HaxeCallExpressionEvaluation evaluate = container.evaluateContexts();
    if (evaluate == null) return null;

    HaxeCallExpressionContext context = container.getContext();
    boolean staticExtension = context != null && context.isStaticExtension;
    boolean canCache = context != null && context.canCache;
    return new HoleEvaluation(evaluate, staticExtension, canCache, isDirty(taintMark, evaluate, context));
  }

  /** A hole-context evaluation; dirty entries are served with a taint, like the main cache's. */
  public record HoleEvaluation(HaxeCallExpressionEvaluation evaluation, boolean staticExtension, boolean canCache, boolean dirty) {}

  /**
   * Dirty when the compute observed truncation, an ARGUMENT's recorded type
   * may be improvable (clipped by a recursion guard or Unknown — the context's
   * incomplete flag), OR the result carries unknown types anywhere. The last
   * two cover what the taint bracket cannot see: an Unknown binding from an
   * argument whose resolve was prevented inside the platform's ResolveCache,
   * and an Unknown argument that left no trace in the output because its
   * parameter is not generic. A dirty entry is still cached and served, but
   * serving it taints the reader, so no caching boundary judges a failure
   * "complete" on this data.
   */
  private static boolean isDirty(long taintMark, HaxeCallExpressionEvaluation evaluate, @Nullable HaxeCallExpressionContext context) {
    return HaxeEvaluationTaint.taintedSince(taintMark)
        || (context != null && context.hasIncompleteArguments())
        || !noUnknownResolvedValues(evaluate);
  }

  private static boolean noUnknownResolvedValues( HaxeCallExpressionEvaluation evaluate) {
      if(evaluate.getReturnTypeWithoutResolve().containsUnknownTypes()) {
        return false;
      }
      for (ResultHolder parameterType : evaluate.getParameterTypes()) {
        if(parameterType.containsUnknownTypes()) {
          return false;
        }
      }
      return resolverFreeOfUnknowns(evaluate.getCallExpressionResolver())
          && resolverFreeOfUnknowns(evaluate.getCallieResolver());
    }

  private static boolean resolverFreeOfUnknowns(@Nullable HaxeGenericResolver resolver) {
    if (resolver == null) return true;
    return resolver.entries().length == resolver.withoutUnknowns().entries().length;
  }


  public void clearCaches() {
    synchronized(this) {
      holeCacheMap.clear();
      cacheMap.clear();
      refreshAttempts.clear();
    }
  }
}

record CallExpressionEvaluationKey(HaxeMethod method, HaxeCallExpression callExpression) {
}

record CallExpressionHoleKey(HaxeMethod method, HaxeCallExpression callExpression, int holeArgumentIndex) {
}