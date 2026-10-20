package com.intellij.plugins.haxe.model.evaluator;

import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The for-loop iteration protocol: the member names the compiler looks for,
 * and whether a value iterates. The for-loop intentions ask through the
 * evaluator's own lookup, so an iterable the evaluator understands (typedefs,
 * {@code Null<T>}, {@code using} extensions) is one they offer a loop for.
 */
// TODO: iterates() lacks the ArrayAccess<T> fallback (Vector<T>) that HaxeExpressionEvaluatorHandlers.handleIterable applies
public final class HaxeIterationProtocol {

  public static final String ITERATOR = "iterator";
  public static final String KEY_VALUE_ITERATOR = "keyValueIterator";
  public static final String HAS_NEXT = "hasNext";
  public static final String NEXT = "next";

  private HaxeIterationProtocol() {
  }

  /** The iterator a for loop over the type uses: its {@code iterator()} or {@code keyValueIterator()} result; null when it has none. */
  @Nullable
  public static ResultHolder iteratorType(@NotNull SpecificHaxeClassReference type, boolean keyValue, @NotNull PsiElement context) {
    return HaxeExpressionEvaluatorHandlers.searchForIteratorType(type, keyValue ? KEY_VALUE_ITERATOR : ITERATOR, context);
  }

  /** Whether a for loop can iterate the value: an array literal, or a known type with the matching iterator. */
  public static boolean iterates(@NotNull ResultHolder value, boolean keyValue, @NotNull PsiElement context) {
    SpecificHaxeClassReference type = value.getClassType();
    if (type == null || type.isUnknown()) return false;
    if (type.isLiteralArray()) return true;
    return iteratorType(type, keyValue, context) != null;
  }
}
