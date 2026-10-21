/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2019-2020 Eric Bishton
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
package com.intellij.plugins.haxe.util;

import com.intellij.plugins.haxe.ide.refactoring.HaxeNamesValidator;
import com.intellij.plugins.haxe.ide.refactoring.HaxeRefactoringUtil;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.type.HaxeGenericResolver;
import com.intellij.plugins.haxe.model.type.HaxeTypeResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Suggests names for a value, for the refactorings, the create-from-usage
 * fixes and completion. Candidates come first from the initializer, from
 * what it is and where it sits ({@link HaxeExpressionNames}), then from the
 * value's type ({@link HaxeTypeNames}). Each candidate is cased for the kind
 * of declaration and offered together with its shorter word tails
 * ({@link HaxeNameKind}). Names the user picked before for a similar value
 * move to the front ({@link HaxeNameStatistics}). Finally, a name that is a
 * keyword or already in use around the value gets a numeric suffix.
 */
public class HaxeNameSuggesterUtil {
  /** The name offered when nothing about the value suggests one. */
  private static final String FALLBACK_NAME = "value";

  private HaxeNameSuggesterUtil() {
  }

  /**
   * Names for a value of {@code type} initialized by {@code initializer};
   * either may be absent, and the type is inferred from the initializer when
   * not given. The names avoid those in use around {@code context} and those
   * in {@code alsoUsed}.
   */
  @NotNull
  public static HaxeSuggestedNames suggest(@Nullable PsiElement initializer,
                                           @Nullable ResultHolder type,
                                           @NotNull HaxeNameKind kind,
                                           @Nullable PsiElement context,
                                           @NotNull Set<String> alsoUsed) {
    ResultHolder valueType = type != null ? type : typeOf(initializer);
    List<String> raw = new ArrayList<>();
    raw.addAll(HaxeExpressionNames.ofExpression(initializer));
    raw.addAll(HaxeExpressionNames.ofPlace(initializer));
    raw.addAll(HaxeTypeNames.of(valueType));
    if (raw.isEmpty() && initializer != null) raw.add(defaultNameFor(initializer));
    String propertyName = raw.isEmpty() ? null : raw.getFirst();
    String typeText = valueType == null || valueType.isUnknown() ? null : valueType.toStringWithoutConstant();

    List<String> names = casedVariants(raw, kind);
    names.addAll(HaxeNameStatistics.frequentlyChosen(kind, propertyName, typeText, names));
    names = HaxeNameStatistics.mostChosenFirst(kind, propertyName, typeText, names);
    if (names.isEmpty()) names = kind.variantsOf(FALLBACK_NAME);

    Set<String> taken = takenNames(context, alsoUsed);
    return new HaxeSuggestedNames(uniqueAgainst(names, taken), kind, propertyName, typeText);
  }

  /**
   * Names for a value known only by the name its declaration gives it, if
   * any, and the simple name of its type, as a lambda parameter is known.
   */
  @NotNull
  public static List<String> suggestForType(@Nullable String declaredName,
                                            @Nullable String typeName,
                                            boolean isFunction,
                                            @Nullable PsiElement context,
                                            @NotNull Set<String> alsoUsed) {
    List<String> raw = new ArrayList<>();
    if (declaredName != null && !declaredName.isEmpty()) raw.add(declaredName);
    raw.addAll(HaxeTypeNames.ofTypeName(typeName, isFunction));
    List<String> names = casedVariants(raw, HaxeNameKind.VARIABLE);
    if (names.isEmpty()) names = HaxeTypeNames.ofTypeName("Dynamic", false);
    return uniqueAgainst(names, takenNames(context, alsoUsed));
  }

  /** Names built from the given raw names: each cased for the kind and offered with its tails, avoiding {@code alsoUsed}. */
  @NotNull
  public static List<String> suggestFrom(@NotNull List<String> rawNames, @NotNull HaxeNameKind kind, @NotNull Set<String> alsoUsed) {
    return uniqueAgainst(casedVariants(rawNames, kind), takenNames(null, alsoUsed));
  }

  /** The whole name in each casing the kind uses, avoiding {@code alsoUsed}. */
  @NotNull
  public static List<String> recased(@NotNull String name, @NotNull HaxeNameKind kind, @NotNull Set<String> alsoUsed) {
    if (!HaxeNamesValidator.isIdentifier(name)) return List.of();
    return uniqueAgainst(kind.recased(name), takenNames(null, alsoUsed));
  }

  /**
   * Every raw name with its shorter tails in the kind's casing, followed by
   * the same in the kind's alternate casing. Raw names that are not valid
   * Haxe identifiers are dropped.
   */
  @NotNull
  private static List<String> casedVariants(@NotNull List<String> raw, @NotNull HaxeNameKind kind) {
    List<String> identifiers = raw.stream().filter(HaxeNamesValidator::isIdentifier).toList();
    Set<String> names = new LinkedHashSet<>();
    for (String name : identifiers) names.addAll(kind.variantsOf(name));
    for (String name : identifiers) names.addAll(kind.alternateVariantsOf(name));
    return new ArrayList<>(names);
  }

  @Nullable
  private static ResultHolder typeOf(@Nullable PsiElement initializer) {
    if (initializer == null) return null;
    return HaxeTypeResolver.getPsiElementType(initializer, new HaxeGenericResolver());
  }

  @NotNull
  private static Set<String> takenNames(@Nullable PsiElement context, @NotNull Set<String> alsoUsed) {
    Set<String> taken = new HashSet<>(HaxeRefactoringUtil.collectKeywords());
    taken.addAll(alsoUsed);
    if (context != null) taken.addAll(HaxeRefactoringUtil.collectUsedNames(context));
    return taken;
  }

  /** A generic name, by kind of expression, for an expression that suggests no name itself and has no known type. */
  @NotNull
  private static String defaultNameFor(@NotNull PsiElement expression) {
    return switch (expression) {
      case HaxeSwitchCaseExpr ignored -> "result";
      case HaxeTernaryExpression ignored -> "result";
      case HaxeCompareExpression ignored -> "result";
      case HaxeLogicAndExpression ignored -> "result";
      case HaxeLogicOrExpression ignored -> "result";
      case HaxeBitwiseExpression ignored -> "bits";
      case HaxeShiftExpression ignored -> "bits";
      case HaxeSuperExpression ignored -> "parent";
      case HaxeThisExpression ignored -> "self";
      case HaxeIteratorExpression ignored -> "iter";
      case HaxeMapLiteral ignored -> "map";
      case HaxeMapInitializerExpression ignored -> "map";
      case HaxeArrayLiteral ignored -> "arr";
      case HaxeArrayAccessExpression ignored -> "element";
      case HaxeObjectLiteral ignored -> "anon";
      case HaxePropertyAccessor ignored -> "prop";
      default -> FALLBACK_NAME;
    };
  }

  /** Each candidate with the smallest numeric suffix that keeps it out of {@code taken}. */
  @NotNull
  private static List<String> uniqueAgainst(@NotNull Collection<String> candidates, @NotNull Set<String> taken) {
    final List<String> result = new ArrayList<>();
    for (String candidate : candidates) {
      int index = 0;
      String suffix = "";
      while (taken.contains(candidate + suffix)) {
        suffix = Integer.toString(++index);
      }
      result.add(candidate + suffix);
    }
    return result;
  }
}
