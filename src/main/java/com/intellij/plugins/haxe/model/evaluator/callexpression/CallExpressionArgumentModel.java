package com.intellij.plugins.haxe.model.evaluator.callexpression;

import com.intellij.plugins.haxe.model.type.SpecificTypeReference;
import com.intellij.psi.PsiElement;
import lombok.Getter;

@Getter
public class CallExpressionArgumentModel {
  PsiElement psiElement;
  SpecificTypeReference type;
  boolean canCache;
  /**
   * This argument is a "hole": a deliberately left-out slot in the call.
   * <p>
   * Used when the question being answered is "what type does THIS argument
   * have?" - typically an untyped parameter or variable being typed by
   * looking at which parameter it lands in when passed to a call. To answer
   * that, the call is evaluated - but evaluating the argument itself would
   * need the very answer being computed, an endless loop. So the argument
   * is swapped for this placeholder: it still occupies its position (so the
   * OTHER arguments line up with the right parameters), but it is never
   * evaluated, never type-checked, and never contributes to type-parameter
   * binding. Its type stays Unknown on purpose.
   */
  boolean hole;
  /**
   * While this argument's type was being computed, a recursion guard
   * stopped part of the work, so the recorded type may be unfinished or
   * wrong. The flag marks it as worth re-evaluating later, once the call's
   * type parameters are known.
   * <p>
   * It is deliberately NOT used to hide error messages: most guard stops
   * are harmless (a typedef referring to itself, the same thing being
   * resolved twice) and still produce the right type - hiding errors on
   * this flag would mask real ones.
   * <p>
   * Consumed by the evaluator cache: an evaluation with an incomplete
   * argument is stored DIRTY and served with a taint, so no caching
   * boundary judges results built on it as final.
   */
  // TODO: the re-evaluation the flag anticipates — re-running an incomplete
  //  argument once the call's type parameters are bound — is not wired yet.
  boolean incomplete;

  public CallExpressionArgumentModel(PsiElement psiElement, SpecificTypeReference type, boolean canCache) {
    this(psiElement, type, canCache, false);
  }

  private CallExpressionArgumentModel(PsiElement psiElement, SpecificTypeReference type, boolean canCache, boolean incomplete) {
    this.psiElement = psiElement;
    this.type = type;
    this.canCache = canCache;
    this.hole = false;
    this.incomplete = incomplete;
  }

  private CallExpressionArgumentModel(PsiElement psiElement) {
    this.psiElement = psiElement;
    this.type = SpecificTypeReference.getUnknown(psiElement);
    // cacheable: hole evaluations are cached under a key that INCLUDES the
    // hole index, so a stored one is only ever reused for the same query
    this.canCache = true;
    this.hole = true;
    this.incomplete = false;
  }

  public static CallExpressionArgumentModel create(PsiElement psiElement, SpecificTypeReference type, boolean canCache) {
    return new CallExpressionArgumentModel(psiElement, type, canCache);
  }

  public static CallExpressionArgumentModel create(PsiElement psiElement, SpecificTypeReference type, boolean canCache, boolean incomplete) {
    return new CallExpressionArgumentModel(psiElement, type, canCache, incomplete);
  }

  public static CallExpressionArgumentModel holeArgument(PsiElement psiElement) {
    return new CallExpressionArgumentModel(psiElement);
  }
}
