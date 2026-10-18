package com.intellij.plugins.haxe.model.evaluator;

import com.intellij.plugins.haxe.lang.psi.HaxeExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeParenthesizedExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeSwitchStatement;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolverUtil;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The type a switch statement's patterns MATCH, computed once per PSI
 * modification and shared by everything that needs it while resolving the
 * case patterns: enum member hints, extractor enum resolution,
 * extracted-value typing. It is the subject expression's evaluated type,
 * except for hxparse's stream DSL, where the patterns match the parser's
 * token list ({@link HaxeStreamSwitch}). Without it every pattern reference
 * re-evaluates the subject; for a generic subject that is a full
 * constructor/type-parameter inference whose evaluation taints, so the
 * general caches cannot absorb the repetition.
 * <p>
 * Only SUCCESSFUL evaluations are shared: an unknown (or partially
 * unresolved) result can come from a truncated evaluation and may heal on
 * recomputation, so a failure is evaluated afresh on every ask.
 */
public final class HaxeSwitchSubjectTypeCache {

  private HaxeSwitchSubjectTypeCache() {
  }

  /** The type the patterns match; null when the switch has no subject expression. */
  @Nullable
  public static ResultHolder subjectType(@Nullable HaxeSwitchStatement switchStatement) {
    if (switchStatement == null) return null;
    HaxeExpression subject = unwrappedSubject(switchStatement);
    if (subject == null) return null;

    ResultHolder shared = CachedValuesManager.getCachedValue(switchStatement, () ->
      CachedValueProvider.Result.create(computeShareableType(switchStatement), PsiModificationTracker.MODIFICATION_COUNT));
    if (shared != null) return shared;
    return matchedType(switchStatement, subject);
  }

  @Nullable
  private static ResultHolder computeShareableType(@NotNull HaxeSwitchStatement switchStatement) {
    HaxeExpression subject = unwrappedSubject(switchStatement);
    if (subject == null) return null;
    ResultHolder type = matchedType(switchStatement, subject);
    boolean shareable = !type.isUnknown() && !type.containsUnknownOrUnresolvedTypes();
    return shareable ? type : null;
  }

  private static ResultHolder matchedType(@NotNull HaxeSwitchStatement switchStatement, @NotNull HaxeExpression subject) {
    if (HaxeStreamSwitch.isStreamSwitch(switchStatement)) {
      ResultHolder tokens = HaxeStreamSwitch.tokenListType(switchStatement);
      if (tokens != null) return tokens;
    }
    return evaluateSubject(subject);
  }

  private static ResultHolder evaluateSubject(@NotNull HaxeExpression subject) {
    HaxeGenericResolver resolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(subject);
    return HaxeExpressionEvaluator.evaluate(subject, new HaxeExpressionEvaluatorContext(subject), resolver).result;
  }

  @Nullable
  private static HaxeExpression unwrappedSubject(@NotNull HaxeSwitchStatement switchStatement) {
    HaxeExpression expression = switchStatement.getExpression();
    while (expression instanceof HaxeParenthesizedExpression parenthesized) {
      expression = parenthesized.getExpression();
    }
    return expression;
  }
}
