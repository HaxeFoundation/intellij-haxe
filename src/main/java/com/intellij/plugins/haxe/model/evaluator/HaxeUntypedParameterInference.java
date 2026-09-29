package com.intellij.plugins.haxe.model.evaluator;

import com.intellij.openapi.util.RecursionGuard;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeTypeParameterDeclaration;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.HaxeParameterModel;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator.*;

/**
 * Type inference for method parameters without a type tag, ordered like the compiler's monomorph binding.
 * Usage inside the BODY binds first; argument types at CALL SITES only fill parameters the body leaves open.
 * A conflict between the two is the compiler's error case, so the body-derived type is never overridden.
 */
public final class HaxeUntypedParameterInference {

  // limits how many call sites the probe evaluates before giving up; the
  // first informative site binds (mirroring the compiler's first-typed-call
  // rule as closely as IDE typing order allows)
  private static final int MAX_PROBED_CALL_SITES = 8;

  // A probed argument can itself be an untyped parameter, whose own probe
  // continues the chain until some call site passes a concrete value. The
  // budget bounds the TOTAL argument evaluations one top-level query spends
  // across the whole chain rather than its depth: a linear chain (one call
  // site per hop) still reaches an informative site 64 hops away, while a
  // branching call graph stops after as much work as 8 sites on 8 levels.
  // Cycles never reach the budget: callSiteProbeGuard cuts a re-entered
  // parameter.
  private static final int MAX_PROBE_WORK = 64;

  private static final ThreadLocal<MutableInt> probeChainDepth = ThreadLocal.withInitial(MutableInt::new);
  private static final ThreadLocal<MutableInt> probeWorkSpent = ThreadLocal.withInitial(MutableInt::new);

  private static final RecursionGuard<PsiElement>
    callSiteProbeGuard = RecursionManager.createGuard("haxeUntypedParameterCallSiteProbe");

  private HaxeUntypedParameterInference() {
  }

  /**
   * Probe outcomes, clean misses included, valid until the next code change:
   * probing chains into argument evaluations that can cycle across methods,
   * and without the memo every query re-runs the whole probe. Only UNTAINTED
   * outcomes are stored; a result shaped by a cut or prevention recomputes
   * until a clean one lands.
   */
  private static Map<HaxeParameter, Optional<ResultHolder>> bindingCache(HaxeParameter parameter) {
    return HaxeUntypedParameterBindingCache.getInstance(parameter.getProject()).bindings();
  }

  public static @Nullable ResultHolder inferMethodParameterType(@NotNull HaxeParameter parameter,
                                                                @NotNull HaxeExpressionEvaluatorContext context,
                                                                @NotNull HaxeGenericResolver resolver) {
    // a settled binding (clean body-derived or call-site type from an earlier
    // query since the last code change) answers directly; without it, a deep query would
    // re-run the body search inside guards where its own usage walk
    // truncates and return Unknown for a parameter the signature shows typed
    Optional<ResultHolder> settled = bindingCache(parameter).get(parameter);
    if (settled != null && settled.isPresent()) return settled.get();

    long taintMark = HaxeEvaluationTaint.mark();
    ResultHolder bodyDerived = bodyDerivedType(parameter, context, resolver);
    if (bodyDerived != null) {
      if (!HaxeEvaluationTaint.taintedSince(taintMark) && isInformative(bodyDerived)) {
        settleBinding(parameter, bodyDerived);
      }
      return bodyDerived;
    }
    return callSiteDerivedType(parameter);
  }

  /** Body usage: the compiler's primary binding source. */
  private static @Nullable ResultHolder bodyDerivedType(HaxeParameter parameter,
                                                        HaxeExpressionEvaluatorContext context,
                                                        HaxeGenericResolver resolver) {
    HaxeMethod method = PsiTreeUtil.getParentOfType(parameter, HaxeMethod.class);
    if (method == null || method.getBody() == null) return null;
    ResultHolder holder = searchReferencesForType(parameter.getComponentName(), context, resolver, method.getBody());
    if (holder.isUnknown()) return null;
    // a generic call the body feeds this parameter into can answer with the
    // callee's own unbound type parameter; that name means nothing in this
    // method's scope and must not shadow a call-site answer
    if (!typeParametersVisibleFrom(method, holder)) return null;
    return holder;
  }

  /** True when every type parameter the type carries is declared by the method itself or an enclosing class. */
  private static boolean typeParametersVisibleFrom(@NotNull HaxeMethod method, @NotNull ResultHolder holder) {
    SpecificTypeReference type = holder.getType();
    if (type instanceof SpecificHaxeClassReference classReference) {
      if (classReference.getHaxeClass() instanceof HaxeTypeParameterDeclaration typeParameter) {
        return isVisibleFrom(typeParameter, method);
      }
      for (ResultHolder specific : classReference.getSpecifics()) {
        if (!typeParametersVisibleFrom(method, specific)) return false;
      }
    }
    if (type instanceof SpecificFunctionReference function) {
      for (HaxeArgument argument : function.getArguments()) {
        if (!typeParametersVisibleFrom(method, argument.getType())) return false;
      }
      return typeParametersVisibleFrom(method, function.getReturnType());
    }
    return true;
  }

  /** True when the type parameter is declared by the method itself or an enclosing class. */
  private static boolean isVisibleFrom(@NotNull HaxeTypeParameterDeclaration typeParameter, @NotNull HaxeMethod method) {
    HaxeNamedComponent owner = typeParameter.getOwner();
    return owner != null && PsiTreeUtil.isAncestor(owner, method, false);
  }

  /**
   * The down-pass: the argument EXPRESSION at a call site is evaluated
   * directly - never the callee's call context - because an argument's type
   * cannot depend on the callee's parameter types. That is what keeps this
   * source free of the call-evaluation re-entry the hole contexts manage.
   * Cross-method cycles (m probes its caller c, whose parameter probes c's
   * call sites inside m) are cut by the guard, tainting like any prevention.
   */
  public static @Nullable ResultHolder callSiteDerivedType(@NotNull HaxeParameter parameter) {
    HaxeMethod method = PsiTreeUtil.getParentOfType(parameter, HaxeMethod.class);
    if (method == null) return null;
    // overload selection owns argument typing for overloaded methods
    if (isOverloaded(method.getModel())) return null;
    HaxeComponentName methodName = method.getComponentName();
    if (methodName == null) return null;
    int parameterIndex = parameterIndex(parameter);
    if (parameterIndex < 0) return null;

    Optional<ResultHolder> cached = bindingCache(parameter).get(parameter);
    if (cached != null) return cached.orElse(null);

    // Probing must stay out of deep evaluation towers: inside a
    // call-context compute the probe's fan-out (usage search + argument
    // evaluations) multiplies every level, and a gate-miss must TAINT so no
    // consumer freezes a judgment that a later, probed query would
    // contradict. Cheap queries (an inlay provider evaluating a
    // declaration) never have a call compute in flight and may probe. The
    // other exception is a probe running under ANOTHER probe: a probed
    // argument that is itself an untyped parameter continues the chain
    // toward a concrete call site, bounded by the probe work budget.
    int chainDepth = probeChainDepth.get().intValue();
    boolean insideProbeChain = chainDepth > 0;
    boolean insideCallCompute = HaxeCallExpressionEvaluatorCacheService.anyComputeInFlight() && !insideProbeChain;
    boolean budgetSpent = probeWorkSpent.get().intValue() >= MAX_PROBE_WORK;
    if (insideCallCompute || budgetSpent) {
      HaxeEvaluationTaint.taint();
      return null;
    }

    long probeMark = HaxeEvaluationTaint.mark();
    ProbeOutcome outcome;
    try {
      outcome = HaxeEvaluationTaint.computeOrTaint(callSiteProbeGuard, parameter, false, () -> {
        MutableInt depth = probeChainDepth.get();
        depth.increment();
        try {
          return probeCallSites(method, methodName, parameterIndex);
        } finally {
          depth.decrement();
        }
      });
    } finally {
      // the work budget spans one top-level query and everything it chains into
      if (chainDepth == 0) probeWorkSpent.get().setValue(0);
    }
    if (outcome == null) return null;
    // a binding is trusted when ITS argument evaluated clean; a MISS is only
    // trusted when the whole probe (search included) was clean - a truncated
    // search may simply not have seen the informative site yet
    boolean cacheable = outcome.binding() != null
                        ? outcome.bindingEvaluatedClean()
                        : !HaxeEvaluationTaint.taintedSince(probeMark);
    if (cacheable) {
      if (outcome.binding() != null) {
        settleBinding(parameter, outcome.binding());
      } else {
        bindingCache(parameter).put(parameter, Optional.empty());
      }
    }
    return outcome.binding();
  }

  /**
   * A newly settled binding is new information: call-cache entries computed
   * while this parameter's type was still open re-arm their dirty-entry
   * refresh on the stamp advance. A clean MISS does not advance it - an
   * entry embedding a genuinely untypable parameter cannot improve.
   */
  private static void settleBinding(HaxeParameter parameter, ResultHolder binding) {
    Optional<ResultHolder> previous = bindingCache(parameter).put(parameter, Optional.of(binding));
    if (previous == null || previous.isEmpty()) {
      HaxeCallExpressionEvaluatorCacheService.informationSettled();
    }
  }

  private record ProbeOutcome(@Nullable ResultHolder binding, boolean bindingEvaluatedClean) {}

  private static @NotNull ProbeOutcome probeCallSites(HaxeMethod method, HaxeComponentName methodName, int parameterIndex) {
    // self-recursive call sites are already filtered out by referenceSearch
    List<PsiReference> callSites = referenceSearch(methodName, (PsiElement)null);
    int probed = 0;
    for (PsiReference callSite : callSites) {
      if (probed >= MAX_PROBED_CALL_SITES) break;
      HaxeExpression argument = argumentAt(callSite, parameterIndex);
      if (argument == null) continue;
      probed++;
      probeWorkSpent.get().increment();
      long argumentMark = HaxeEvaluationTaint.mark();
      ResultHolder argumentType = evaluateWithRecursionGuard(argument).result;
      if (isInformative(argumentType)) {
        boolean clean = !HaxeEvaluationTaint.taintedSince(argumentMark);
        return new ProbeOutcome(argumentType, clean);
      }
      ResultHolder translated = translatedToOwnTypeParameters(method, argument, parameterIndex, argumentType);
      if (translated != null) {
        boolean clean = !HaxeEvaluationTaint.taintedSince(argumentMark);
        return new ProbeOutcome(translated, clean);
      }
    }
    return new ProbeOutcome(null, false);
  }

  /// A call-site answer spelled in the CALLER's type parameters is still usable
  /// when the call pairs the callee's own type parameters with those caller
  /// parameters; that pairing rewrites the answer into the callee's vocabulary:
  /// ```haxe
  /// function outer<T>(a:Array<T>, f:T->Int) { inner(a, f); }
  /// function inner<T>(a, f) {}   // a pairs the T's -> f : T->Int in inner's T
  /// ```
  /// An answer keeping any untranslatable foreign type parameter stays rejected -
  /// freezing it into the signature is the order-dependent over-specialization
  /// the informative rule protects against.
  private static @Nullable ResultHolder translatedToOwnTypeParameters(HaxeMethod method,
                                                                      HaxeExpression argument,
                                                                      int argumentIndex,
                                                                      @Nullable ResultHolder argumentType) {
    boolean worthTranslating = argumentType != null
      && !argumentType.isUnknown()
      && argumentType.isOrContainsTypeParameters()
      && argumentType.isCacheable();
    if (!worthTranslating) return null;
    HaxeCallExpression call = PsiTreeUtil.getParentOfType(argument, HaxeCallExpression.class);
    if (call == null) return null;
    Map<HaxeTypeParameterDeclaration, HaxeTypeParameterDeclaration> inverse = callerToOwnTypeParameters(method, call, argumentIndex);
    ResultHolder translated = translateTypeParameters(argumentType, method, inverse);
    if (translated == null || !translated.isCacheable()) return null;
    return translated;
  }

  /// Derives caller-parameter -> callee-parameter from the call's OTHER
  /// arguments: an argument whose evaluated type carries a caller type
  /// parameter in a slot the callee's declared parameter type spells with one
  /// of its own type parameters pairs the two (the caller's `T` with the
  /// callee's `T` through a shared `Array<T>` slot). The call evaluation's
  /// resolver cannot supply this pairing: it hint-resolves each argument
  /// against the parameter type first, which substitutes the callee's own
  /// parameter into the argument before binding and leaves only an identity
  /// entry. A caller parameter paired with several different callee
  /// parameters is dropped - either translation would be arbitrary.
  private static Map<HaxeTypeParameterDeclaration, HaxeTypeParameterDeclaration> callerToOwnTypeParameters(HaxeMethod method,
                                                                                                           HaxeCallExpression call,
                                                                                                           int holeArgumentIndex) {
    Map<HaxeTypeParameterDeclaration, HaxeTypeParameterDeclaration> inverse = new HashMap<>();
    Set<HaxeTypeParameterDeclaration> ambiguous = new HashSet<>();
    HaxeCallExpressionList expressionList = call.getExpressionList();
    HaxeMethodModel model = method.getModel();
    if (expressionList == null || model == null) return inverse;
    List<HaxeExpression> arguments = expressionList.getExpressionList();
    List<HaxeParameterModel> parameters = model.getParameters();
    int pairCount = Math.min(arguments.size(), parameters.size());
    for (int i = 0; i < pairCount; i++) {
      if (i == holeArgumentIndex) continue;
      HaxeParameterModel parameter = parameters.get(i);
      HaxeTypeTag typeTag = parameter.getTypeTagPsi();
      if (typeTag == null) continue;
      ResultHolder declared = HaxeTypeResolver.getTypeFromTypeTag(typeTag, parameter.getParameterPsi());
      if (!declared.isOrContainsTypeParameters()) continue;
      probeWorkSpent.get().increment();
      ResultHolder argumentType = evaluateWithRecursionGuard(arguments.get(i)).result;
      collectTypeParameterPairs(declared, argumentType, method, inverse, ambiguous);
    }
    inverse.keySet().removeAll(ambiguous);
    return inverse;
  }

  /** Walks the declared parameter type and the argument type in parallel, pairing a callee-owned type parameter with the caller type parameter in the same slot. */
  private static void collectTypeParameterPairs(ResultHolder declared,
                                                ResultHolder argument,
                                                HaxeMethod method,
                                                Map<HaxeTypeParameterDeclaration, HaxeTypeParameterDeclaration> inverse,
                                                Set<HaxeTypeParameterDeclaration> ambiguous) {
    SpecificTypeReference declaredType = declared.getType();
    SpecificTypeReference argumentType = argument.getType();
    if (declaredType instanceof SpecificHaxeClassReference declaredClass) {
      if (!(argumentType instanceof SpecificHaxeClassReference argumentClass)) return;
      if (declaredClass.getHaxeClass() instanceof HaxeTypeParameterDeclaration ownParameter) {
        if (!isVisibleFrom(ownParameter, method)) return;
        if (!(argumentClass.getHaxeClass() instanceof HaxeTypeParameterDeclaration callerParameter)) return;
        // a parameter already in the callee's own vocabulary needs no pairing
        if (isVisibleFrom(callerParameter, method)) return;
        HaxeTypeParameterDeclaration existing = inverse.putIfAbsent(callerParameter, ownParameter);
        if (existing != null && !existing.equals(ownParameter)) ambiguous.add(callerParameter);
        return;
      }
      // same class on both sides makes the specifics positionally comparable
      if (!declaredClass.getHaxeClassReference().refersToSameClass(argumentClass.getHaxeClassReference())) return;
      ResultHolder[] declaredSpecifics = declaredClass.getSpecifics();
      ResultHolder[] argumentSpecifics = argumentClass.getSpecifics();
      int specificCount = Math.min(declaredSpecifics.length, argumentSpecifics.length);
      for (int i = 0; i < specificCount; i++) {
        collectTypeParameterPairs(declaredSpecifics[i], argumentSpecifics[i], method, inverse, ambiguous);
      }
      return;
    }
    if (declaredType instanceof SpecificFunctionReference declaredFunction
        && argumentType instanceof SpecificFunctionReference argumentFunction) {
      List<HaxeArgument> declaredArguments = declaredFunction.getArguments();
      List<HaxeArgument> argumentArguments = argumentFunction.getArguments();
      int argumentCount = Math.min(declaredArguments.size(), argumentArguments.size());
      for (int i = 0; i < argumentCount; i++) {
        collectTypeParameterPairs(declaredArguments.get(i).getType(), argumentArguments.get(i).getType(), method, inverse, ambiguous);
      }
      collectTypeParameterPairs(declaredFunction.getReturnType(), argumentFunction.getReturnType(), method, inverse, ambiguous);
    }
  }

  /**
   * Rewrites every type-parameter reference the type carries into the callee's
   * own vocabulary: the callee's own parameters pass through, foreign ones go
   * through the inverse binding. Null when any foreign parameter has no translation.
   */
  private static @Nullable ResultHolder translateTypeParameters(ResultHolder holder,
                                                                HaxeMethod method,
                                                                Map<HaxeTypeParameterDeclaration, HaxeTypeParameterDeclaration> inverse) {
    SpecificTypeReference type = holder.getType();
    if (type instanceof SpecificHaxeClassReference classReference) {
      if (classReference.getHaxeClass() instanceof HaxeTypeParameterDeclaration typeParameter) {
        if (isVisibleFrom(typeParameter, method)) return holder;
        HaxeTypeParameterDeclaration ownParameter = inverse.get(typeParameter);
        return ownParameter != null ? ownParameter.getModel().getInstanceType() : null;
      }
      ResultHolder[] specifics = classReference.getSpecifics();
      ResultHolder[] translated = new ResultHolder[specifics.length];
      for (int i = 0; i < specifics.length; i++) {
        translated[i] = translateTypeParameters(specifics[i], method, inverse);
        if (translated[i] == null) return null;
      }
      return SpecificHaxeClassReference.withGenerics(classReference.getHaxeClassReference(), translated).createHolder();
    }
    if (type instanceof SpecificFunctionReference function) {
      List<HaxeArgument> arguments = new ArrayList<>();
      for (HaxeArgument functionArgument : function.getArguments()) {
        ResultHolder argumentType = translateTypeParameters(functionArgument.getType(), method, inverse);
        if (argumentType == null) return null;
        arguments.add(functionArgument.withType(argumentType));
      }
      ResultHolder returnType = translateTypeParameters(function.getReturnType(), method, inverse);
      if (returnType == null) return null;
      return function.withTypes(arguments, returnType).createHolder();
    }
    return holder;
  }

  /**
   * The argument expression at the given parameter index, when the usage is
   * a call shape whose arguments map 1:1 onto parameters. Qualified calls
   * are skipped: for a static method a dotted usage may be a `using`
   * static-extension call, which shifts every index by the receiver.
   */
  private static @Nullable HaxeExpression argumentAt(PsiReference callSite, int parameterIndex) {
    if (!(callSite.getElement() instanceof HaxeExpression element)) return null;
    if (!(element.getParent() instanceof HaxeCallExpression call)) return null;
    if (call.getExpression() != element) return null;
    HaxeCallExpressionList expressionList = call.getExpressionList();
    if (expressionList == null) return null;
    List<HaxeExpression> arguments = expressionList.getExpressionList();
    return parameterIndex < arguments.size() ? arguments.get(parameterIndex) : null;
  }

  /** A binding accepted as-is must be a settled concrete type; a type-parameter-carrying answer only counts after translation into the callee's own vocabulary. */
  private static boolean isInformative(@Nullable ResultHolder type) {
    return type != null
           && !type.isUnknown()
           && !type.isOrContainsTypeParameters()
           && type.isCacheable();
  }

  private static boolean isOverloaded(@Nullable HaxeMethodModel model) {
    if (model == null) return false;
    return model.hasModifier(HaxePsiModifier.OVERLOAD) || !model.getOverloadsFromMeta().isEmpty();
  }

  private static int parameterIndex(HaxeParameter parameter) {
    if (!(parameter.getParent() instanceof HaxeParameterList parameterList)) return -1;
    return parameterList.getParameterList().indexOf(parameter);
  }
}
