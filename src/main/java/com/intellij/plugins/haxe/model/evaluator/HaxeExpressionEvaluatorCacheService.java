package com.intellij.plugins.haxe.model.evaluator;

import com.intellij.openapi.util.LowMemoryWatcher;
import com.intellij.openapi.util.RecursionGuard;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificTypeReference;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator._handle;

/**
 * To avoid unnecessary re-evaluation of elements used by  other expressions (ex. functions without type tags etc)
 * We cache the evaluation result until a Psi change happens, we dont want to cache for longer as ResultHolder
 * contains SpecificTypeReference elements both as the type and as generics and these contain PsiElements
 * that might become invalid
 */
public class HaxeExpressionEvaluatorCacheService  {

  private volatile  Map<EvaluationKey, ResultHolder> cacheMap = new ConcurrentHashMap<>();
  private volatile Map<PsiElement, ResultHolder> methodReturnTypes = new ConcurrentHashMap<>();
  // Return types whose computation was truncated, for example by a
  // self-referential initializer or an unresolvable argument. They are
  // served with a taint, like the call-expression cache's dirty entries, so
  // no consumer stores anything built on them as complete. Without them, a
  // return type that can never compute cleanly (such as a function returning
  // a self-referential, macro-built rule table) is rebuilt for every
  // consumer on every pass.
  private volatile Map<PsiElement, ResultHolder> dirtyMethodReturnTypes = new ConcurrentHashMap<>();
  private final Map<PsiElement, Long> dirtyRefreshAttempts = new ConcurrentHashMap<>();
  public static boolean skipCaching = false;// just convenience flag for debugging


  public HaxeExpressionEvaluatorCacheService() {
    LowMemoryWatcher.register(() -> {
      clearCaches();
    });
  }

  public @NotNull ResultHolder handleWithResultCaching(@NotNull final PsiElement element,
                                                       @NotNull final HaxeExpressionEvaluatorContext context,
                                                       @Nullable final HaxeGenericResolver resolver) {

    if(skipCaching){
      ResultHolder holder = _handle(element, context, resolver);
      if(holder == null) return SpecificTypeReference.getUnknown(element).createHolder();
      return holder;
    }

    EvaluationKey key = new EvaluationKey(element, resolver == null ? "NO_RESOLVER" : resolver.toCacheString());
    ResultHolder cached = cacheMap.get(key);
    if (cached != null) {
      return cached;
    }

    // The stamp distinguishes COMPLETE results from guard-truncated ones: any
    // recursion guard firing beneath this point makes mayCacheNow() false, and
    // such a result is only valid for this exact evaluation stack. The taint
    // mark covers what the stamp cannot see: a guard-truncated CACHED call
    // evaluation served from another stack's computation. A failure computed
    // with both signals clean genuinely tried every path and is as
    // authoritative as a success - caching it is what keeps broken references
    // from re-running the whole evaluation on every visit.
    RecursionGuard.StackStamp stamp = RecursionManager.markStack();
    long taintMark = HaxeEvaluationTaint.mark();
    ResultHolder holder = _handle(element, context, resolver);
    if (holder == null) return SpecificTypeReference.getUnknown(element).createHolder();
    boolean complete = stamp.mayCacheNow() && !HaxeEvaluationTaint.taintedSince(taintMark);
    if (complete && holder.isCacheable()) {
      boolean isUnknown = holder.isUnknown();
      // success: fully resolved with all typeParameters
      boolean cacheableSuccess = !isUnknown && !holder.containsUnknownOrUnresolvedTypes();
      // A failure (Unknown) is only trustworthy when computed OUTSIDE any
      // recursion guard. Inside a guarded computation, a nested step that
      // needs an element already under evaluation backs out QUIETLY - no
      // prevention fires, so the stamp and taint checks above both stay
      // clean - and the result comes out Unknown even though the element
      // has a real type (evaluating it fresh at top level finds it).
      // Only at guard depth zero does Unknown reliably mean "genuinely has
      // no type" rather than "could not look at itself mid-evaluation".
      boolean cacheableFailure = isUnknown && !HaxeEvaluationTaint.insideGuardedComputation();

      if (cacheableSuccess || cacheableFailure) {
        cacheMap.put(key, holder);
      }
    }
    return holder;

  }


  /**
   * Inferred method return types. A clean result is stored and served as
   * is. A result whose computation was truncated (a probe gate refusal, a
   * recursion prevention) is stored as a dirty entry and served with a
   * taint, so no consumer caches anything built on it as complete.
   *
   * A dirty entry stored deep inside an evaluation is recomputed once by
   * the first top-level consumer, where a clean result may land. After
   * that, and for an entry stored at top level, it is recomputed only when
   * new type information settles
   * ({@link HaxeCallExpressionEvaluatorCacheService#informationSettled}).
   * This keeps an Unknown computed deep inside an evaluation from starving
   * the return-type inlay, while a type that can never compute cleanly is
   * not rebuilt for every consumer.
   */
  public @NotNull ResultHolder methodReturnType(@NotNull PsiElement method, @NotNull Supplier<ResultHolder> compute) {
    ResultHolder cached = methodReturnTypes.get(method);
    if (cached != null) return cached;
    ResultHolder dirty = dirtyMethodReturnTypes.get(method);
    boolean topLevel = isTopLevelCompute();
    if (dirty != null && !(topLevel && dirtyRefreshArmed(method))) {
      HaxeEvaluationTaint.taint();
      return dirty;
    }

    long taintMark = HaxeEvaluationTaint.mark();
    ResultHolder computed = compute.get();
    if (!computed.isCacheable()) return computed;
    boolean clean = !HaxeEvaluationTaint.taintedSince(taintMark);
    // clean Unknown inside a guarded computation is still path-dependent
    // (same rule as the expression cache's failure caching)
    boolean unknownInsideGuards = computed.isUnknown() && HaxeEvaluationTaint.insideGuardedComputation();
    if (clean && !unknownInsideGuards) {
      methodReturnTypes.put(method, computed);
      dirtyMethodReturnTypes.remove(method);
      dirtyRefreshAttempts.remove(method);
    }
    else if (dirty == null) {
      dirtyMethodReturnTypes.put(method, computed);
      if (topLevel) dirtyRefreshAttempts.put(method, HaxeCallExpressionEvaluatorCacheService.settledInfoStamp());
    }
    return computed;
  }

  /** Whether the current computation is a top-level query rather than a step deep inside a resolve or call-evaluation tower. */
  private static boolean isTopLevelCompute() {
    return !HaxeCallExpressionEvaluatorCacheService.anyComputeInFlight() && !HaxeEvaluationTaint.insideGuardedComputation();
  }

  /** Whether the dirty entry may be recomputed now; records the attempt, so this holds at most once per settled-information stamp. */
  private boolean dirtyRefreshArmed(@NotNull PsiElement method) {
    long stamp = HaxeCallExpressionEvaluatorCacheService.settledInfoStamp();
    Long lastAttempt = dirtyRefreshAttempts.get(method);
    if (lastAttempt != null && lastAttempt == stamp) return false;
    dirtyRefreshAttempts.put(method, stamp);
    return true;
  }

  public void clearCaches() {
    synchronized(this) {
      methodReturnTypes.clear();
      dirtyMethodReturnTypes.clear();
      dirtyRefreshAttempts.clear();
      cacheMap.clear();
    }
  }
}

record EvaluationKey( PsiElement element, String evalParamString) {
}