/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2018 Ilya Malanin
 * Copyright 2017-2020 Eric Bishton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.lang.psi;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.lang.psi.impl.*;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.evaluator.HaxeEvaluationTaint;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.plugins.haxe.util.HaxeDebugUtil;
import com.intellij.plugins.haxe.v2.display.HaxeCompilerResolveService;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.resolve.ResolveCache;
import com.intellij.psi.util.PsiTreeUtil;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.intellij.plugins.haxe.lang.psi.impl.HaxeReferenceUtil.*;
import static com.intellij.plugins.haxe.model.type.SpecificTypeReference.*;
import static com.intellij.plugins.haxe.util.HaxeDebugLogUtil.traceAs;
import static com.intellij.plugins.haxe.util.HaxeResolveUtil.*;
import static com.intellij.plugins.haxe.lang.psi.HaxeResolveChecks.*;

/**
 * @author: Fedor.Korotkov
 */
@CustomLog
@Service(Service.Level.PROJECT)
public final class HaxeResolver implements ResolveCache.AbstractResolver<HaxeReference, List<? extends PsiElement>> {
  public static final List<? extends PsiElement> EMPTY_LIST = Collections.emptyList();
  public static final int MAX_DEBUG_MESSAGE_LENGTH = 200;

  //static {  // Remove when finished debugging.
  //  LOG.setLevel(LogLevel.DEBUG);
  //  LOG.debug(" ========= Starting up debug logger for HaxeResolver. ==========");
  //}

  private boolean reportCacheMetrics = false;   // Should always be false when checked in.
  private final AtomicInteger dumbRequests = new AtomicInteger(0);
  private final AtomicInteger requests = new AtomicInteger(0);
  private final AtomicInteger resolves = new AtomicInteger(0);
  private final int REPORT_FREQUENCY = 100;

  // the three stack frames every resolve runs under; all frame geometry
  // and RecursionManager mechanics live in HaxeResolveFrames
  private final HaxeResolveFrames frames = new HaxeResolveFrames();

  public static @NotNull HaxeResolver getInstance(@NotNull Project project) {
    return project.getService(HaxeResolver.class);
  }

  /**
   * Static resolution first; when it comes up empty, the compilation server's
   * cached knowledge is the last resort — the compiler runs macros, so it
   * knows generated members static analysis cannot see. The fallback sits
   * OUTSIDE the resolve cache on purpose: it is a cheap in-memory lookup, and
   * a blueprint arriving later must not be shadowed by a cached empty result.
   */
  @Override
  public List<? extends PsiElement> resolve(@NotNull HaxeReference reference, boolean incompleteCode) {
    List<? extends PsiElement> elements = staticResolve(reference, incompleteCode);
    if (elements == null || elements.isEmpty()) {
      List<? extends PsiElement> compilerResolved =
        HaxeCompilerResolveService.getInstance(reference.getProject()).tryResolve(reference);
      if (compilerResolved != null && !compilerResolved.isEmpty()) {
        return compilerResolved;
      }
    }
    return elements == null ? EMPTY_LIST : elements;
  }

  private List<? extends PsiElement> staticResolve(@NotNull HaxeReference reference, boolean incompleteCode) {
       /** See docs on {@link HaxeDebugUtil#isCachingDisabled} for how to set this flag. */
       boolean skipCachingForDebug = HaxeDebugUtil.isCachingDisabled();

      ProgressIndicatorProvider.checkCanceled();


       // If we are in dumb mode (e.g. we are still indexing files and resolving may
       // fail until the indices are complete), we don't want to cache the (likely incorrect)
       // results.
       boolean isDumb = DumbService.isDumb(reference.getProject());
       boolean skipCaching = skipCachingForDebug || isDumb;

        // Resolving a reference can evaluate expressions that CONTAIN that
        // same reference (the chain and enum-hint checks evaluate scope
        // statements); such re-entry is diverted to the restricted pipeline
        // before any platform guard can truncate it to a transient empty.
        if (frames.fullResolveInProgress(reference)) {
          return reentrantResolve(reference, incompleteCode);
        }
        if (skipCaching) {
          List<? extends PsiElement> computed = doResolve(reference, incompleteCode);
          return computed == null ? EMPTY_LIST : computed;
        }
        List<? extends PsiElement> elements = frames.runCacheGated(reference, () ->
          ResolveCache.getInstance(reference.getProject())
                       .resolveWithCaching(reference, this::doResolve, true, incompleteCode));
        if (elements == null) elements = EMPTY_LIST;

       if (reportCacheMetrics) {
         if (skipCachingForDebug) {
           log.debug("Resolve cache is disabled.  No metrics computed.");
           reportCacheMetrics = false;
         }
         else {
           int dumb = isDumb ? dumbRequests.incrementAndGet() : dumbRequests.get();
           int requestCount = isDumb ? requests.get() : requests.incrementAndGet();
           if ((dumb + requestCount) % REPORT_FREQUENCY == 0) {
             int res = resolves.get();
             Formatter formatter = new Formatter();
             formatter.format("Resolve requests: %d; cache misses: %d; (%2.2f%% effective); Dumb requests: %d",
                              requestCount, res,
                              (1.0 - (Float.intBitsToFloat(res) / Float.intBitsToFloat(requestCount))) * 100,
                              dumb);
             log.debug(formatter.toString());
           }
         }
       }
       return elements;
  }


  @Nullable
  private List<? extends PsiElement> doResolve(@NotNull HaxeReference reference, boolean incompleteCode) {
    boolean traceEnabled = log.isTraceEnabled();
    String referenceText =   getReferenceTextFromStubOrPsi(reference);
    if (traceEnabled) {
      log.trace(traceMsg("-----------------------------------------"));
      log.trace(traceMsg("Resolving reference: " + referenceText));
    }

    List<? extends PsiElement> foundElements = frames.runFullPipeline(reference, () -> {
      long taintMark = HaxeEvaluationTaint.mark();
      List<? extends PsiElement> resolved = doResolveInner(reference, incompleteCode, referenceText, false);
      // A miss is only cacheable when it is CERTAIN: an empty computed on
      // truncated data (visible via the taint counter) may succeed on
      // recomputation and must not be frozen until the next code change, so the
      // platform's cache write is suppressed. Certain misses stay cacheable
      // - a definitively-broken reference must not re-resolve every query.
      if ((resolved == null || resolved.isEmpty()) && HaxeEvaluationTaint.taintedSince(taintMark)) {
        frames.suppressCacheWrite(reference);
      }
      return resolved == null ? EMPTY_LIST : resolved;
    });

    if (traceEnabled) {
      log.trace(traceMsg("Finished  reference: " + referenceText));
      log.trace(traceMsg("-----------------------------------------"));
    }

    return foundElements;
  }

  /**
   * Uncached restricted pipeline for a reference whose resolve is already in
   * progress on this thread: the expression-evaluating checks are skipped so
   * the syntactic/tree-walk checks still resolve locals and members
   * correctly, instead of the platform cache guard truncating the whole
   * resolve to empty. Results are never cached (they were computed without
   * the full pipeline) and an empty result taints the thread — the full
   * pipeline might have resolved it, so caching boundaries must not treat
   * data built on it as complete.
   */
  private List<? extends PsiElement> reentrantResolve(@NotNull HaxeReference reference, boolean incompleteCode) {
    if (frames.restrictedResolveInProgress(reference)) {
      // second-level re-entry: no pipeline left to try
      HaxeEvaluationTaint.taint();
      return EMPTY_LIST;
    }
    List<? extends PsiElement> result = frames.runRestrictedPipeline(reference, () -> {
      String referenceText = getReferenceTextFromStubOrPsi(reference);
      return doResolveInner(reference, incompleteCode, referenceText, true);
    });
    if (result == null || result.isEmpty()) {
      HaxeEvaluationTaint.taint();
    }
    return result == null ? EMPTY_LIST : result;
  }


  /** Pipeline runs so far, for tests bounding the fan-out of a resolve. */
  @TestOnly
  public int pipelineRuns() {
    return resolves.get();
  }

  private List<? extends PsiElement> doResolveInner(@NotNull HaxeReference reference, boolean incompleteCode, String referenceText, boolean reentrant) {
    resolves.incrementAndGet();

    if (reference instanceof HaxeLiteralExpression || reference instanceof HaxeConstantExpression) {
      if (!(reference instanceof HaxeRegularExpression || reference instanceof HaxeStringLiteralExpression)) {
        return EMPTY_LIST;
      }
    }
    if(reference instanceof HaxeCallExpression) {
      return EMPTY_LIST;
    }

    PsiElement parent = reference.getParent();
    boolean isType = parent instanceof HaxeType || PsiTreeUtil.getStubOrPsiParentOfType(reference, HaxeTypeTag.class) != null;
    List<? extends PsiElement> result = checkIsTypeParameter(reference);

    // NOTE: always Keep checkIsType  high up, it is used a lot (ex. when resolving type for HaxeType)
    // and moving it down the stack will only result in unnecessary overhead and potential recursion problems
    if (result == null) result = checkIsType(reference); //HaxeReferenceExpression
    // the chain and enum-hint checks EVALUATE expressions, which is what
    // re-enters an in-progress resolve of this same reference - the
    // restricted (reentrant) pipeline skips them so tree-walk still answers
    if (result == null && !reentrant) result = checkIsChain(reference, referenceText);  //HaxeReferenceExpression

    if (result == null) result = checkIsAlias(reference);
    if (result == null && !reentrant) result = checkEnumMemberHints(reference);

    if (result == null) result = checkIsFullyQualifiedStatement(reference);
    if (result == null) result = checkIsSuperExpression(reference);
    if (result == null) result = checkIsNewExpression(reference);
    if (result == null) result = checkMacroIdentifier(reference);

    if (result == null) result = checkIsAccessor(reference);
    // also runs in the restricted pipeline: untyped-parameter inference has
    // no other source than the usage walk. The call-evaluation cycle it can
    // enter is cut at the call cache's same-key in-flight check instead.
    if (result == null) result = checkElementUsage(reference);


    // if we know we are looking for a type; and references got multiple parts we can skip
    // anything checking members and walking the tree structure
    if(!isQualifiedNameReferenceStructure(reference)) {
      if (result == null) result = checkEnumExtractor(reference);// do before walking tree
      if (result == null) result = checkReferenceInExtractorMatchExpression(reference);
      if (result == null) result = checkIsSwitchVar(reference);
      if (result == null) result = checkCaptureVarReference(reference);
      if (result == null) result = checkByTreeWalk(reference);  // Beware: This will also locate constraints in scope.
    }

    HaxeFileModel fileModel = HaxeFileModel.fromElement(reference);
    // search same file first (avoids incorrect resolve of common named Classes and member with same name in local file)
    if (result == null)result =  searchInSameFile(reference, fileModel, isType);
    if (result == null) result = checkIsModuleName(reference, referenceText);
    if (result == null) result = checkIsClassName(reference, referenceText);
    if (result == null) result = checkCaptureVar(reference);
    if (result == null) result = checkSwitchOnEnum(reference);
    if (result == null) result = checkIsFakeReference(reference, referenceText);
    // a type position never names an inherited member; the member walk would
    // resolve the enclosing class's supertypes, which are type positions
    // themselves, and recurse once per unresolvable supertype
    if (result == null && !isType) result = checkMemberReference(reference); // must be after resolvers that can find identifier inside a method
    if (result == null) result = checkImports(reference, fileModel, isType);
    if (result == null) result = checkIsForwardedName(reference);
    if (result == null) result = checkGlobalAlias(reference, referenceText);
    if (result == null) result = checkIsLocalModule(reference);
    if (result == null) result = checkIsMetadataSpecial(reference);

    if(result == null) {
      // check if this can be a switch extract variable,
      // checking here (and not in switch var method) because we want to make sure all other type resolve has been tried
      result = checkIfNamedSwitchValue(reference);
    }

    if (result == null) {
      LogResolution(reference, "failed after exhausting all options.");
      return EMPTY_LIST; // certain miss unless the taint counter says otherwise (see doResolve)
    }

    if (log.isTraceEnabled()) {
      String message = "caching result for :" + referenceText;
      traceAs(log, HaxeDebugUtil.getCallerStackFrame(), message);
    }

    return result;

  }
}