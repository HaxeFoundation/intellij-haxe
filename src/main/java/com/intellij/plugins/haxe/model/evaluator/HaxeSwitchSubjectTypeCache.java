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
 * The evaluated type of a switch statement's SUBJECT expression, computed
 * once per PSI modification and shared by everything that needs it while
 * resolving the switch's case patterns: enum member hints, extractor enum
 * resolution, extracted-value typing. Without it every pattern reference
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

  /** The subject's evaluated type; null when the switch has no subject expression. */
  @Nullable
  public static ResultHolder subjectType(@Nullable HaxeSwitchStatement switchStatement) {
    if (switchStatement == null) return null;
    HaxeExpression subject = unwrappedSubject(switchStatement);
    if (subject == null) return null;

    ResultHolder shared = CachedValuesManager.getCachedValue(switchStatement, () ->
      CachedValueProvider.Result.create(computeShareableType(switchStatement), PsiModificationTracker.MODIFICATION_COUNT));
    if (shared != null) return shared;
    return evaluateSubject(subject);
  }

  @Nullable
  private static ResultHolder computeShareableType(@NotNull HaxeSwitchStatement switchStatement) {
    HaxeExpression subject = unwrappedSubject(switchStatement);
    if (subject == null) return null;
    ResultHolder type = evaluateSubject(subject);
    boolean shareable = !type.isUnknown() && !type.containsUnknownOrUnresolvedTypes();
    return shareable ? type : null;
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
