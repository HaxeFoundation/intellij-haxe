/*
 * Copyright 2019 Eric Bishton
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
package com.intellij.plugins.haxe.ide.annotator;

import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.model.evaluator.assign.AssignExplanation;
import com.intellij.plugins.haxe.model.fixer.HaxeFixer;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevel;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevelUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * A library of annotations that can be re-used.  Place annotations that are used more than
 * once (or should be) in this class.
 */
public class HaxeStandardAnnotation {

  private HaxeStandardAnnotation() {
  }

  public static void typeModelMissing(@NotNull HaxeProblemReporter reporter,
                                      @NotNull PsiElement incompatibleElement,
                                      String missingType) {
    String message = HaxeBundle.message("haxe.semantic.method.parameter.type.not.found", missingType);
    reporter.problem(HighlightSeverity.WEAK_WARNING, message).range(incompatibleElement).create();
  }

  public static @NotNull HaxeProblemReporter.Problem typeMismatch(@NotNull HaxeProblemReporter reporter,
                                                                  @NotNull PsiElement incompatibleElement,
                                                                  String incompatibleType,
                                                                  String correctType) {
    String message = HaxeBundle.message("haxe.semantic.incompatible.type.0.should.be.1", incompatibleType, correctType);
    return reporter.problem(HighlightSeverity.ERROR, message).range(incompatibleElement);
  }

  public static @NotNull HaxeProblemReporter.Problem typeMismatchShadowing(@NotNull HaxeProblemReporter reporter,
                                                                           @NotNull PsiElement incompatibleElement,
                                                                           String incompatibleType,
                                                                           String correctType) {
    String message = HaxeBundle.message("haxe.semantic.incompatible.type.shadowing", incompatibleType, correctType);
    return reporter.problem(HighlightSeverity.WEAK_WARNING, message).range(incompatibleElement);
  }

  public static @NotNull HaxeProblemReporter.Problem typeMismatchMissingMembers(@NotNull HaxeProblemReporter reporter,
                                                                                @NotNull PsiElement incompatibleElement,
                                                                                AssignExplanation context) {
    String message = HaxeBundle.message("haxe.semantic.incompatible.type.missing.members.0",
                                        context.createMissingMembersMessage());
    return reporter.problem(HighlightSeverity.ERROR, message).range(incompatibleElement);
  }

  public static void addTypeMismatchWrongTypeMembersAnnotations(@NotNull HaxeProblemReporter reporter,
                                                                @NotNull PsiElement incompatibleElement,
                                                                AssignExplanation context) {
    TextRange expectedRange = incompatibleElement.getTextRange();
    Map<PsiElement, String> wrongTypeMap = context.getWrongTypeMap();
    boolean allInRange = wrongTypeMap.keySet().stream().allMatch(psi -> expectedRange.contains(psi.getTextRange()));
    if (allInRange) {
      wrongTypeMap.forEach((key, value) -> reporter.problem(HighlightSeverity.ERROR, value).range(key).create());
    }
    else {
      String message = HaxeBundle.message("haxe.semantic.incompatible.type.wrong.member.types.0",
                                          context.createWrongTypeMembersMessage());
      reporter.problem(HighlightSeverity.ERROR, message).range(incompatibleElement).create();
    }
  }

  /**
   * "«feature» requires Haxe X" error for a construct below the module's
   * language level, carrying the "set language level" quickfix. Callers may
   * append their own fixes (removing the construct) and must {@code create()}.
   */
  public static @NotNull AnnotationBuilder requiresLanguageLevel(@NotNull AnnotationHolder holder,
                                                                 @NotNull PsiElement element,
                                                                 @NotNull HaxeLanguageLevel required,
                                                                 @NotNull String featureName) {
    return requiresLanguageLevel(holder, element, required, featureName, HighlightSeverity.ERROR);
  }

  public static @NotNull AnnotationBuilder requiresLanguageLevel(@NotNull AnnotationHolder holder,
                                                                 @NotNull PsiElement element,
                                                                 @NotNull HaxeLanguageLevel required,
                                                                 @NotNull String featureName,
                                                                 @NotNull HighlightSeverity severity) {
    HaxeLanguageLevel current = HaxeLanguageLevelUtil.getLanguageLevel(element);
    String message = HaxeBundle.message("haxe.semantic.feature.requires.language.level",
                                        featureName, required.getPresentableText(), current.getPresentableText());
    String fixText = HaxeBundle.message("haxe.quickfix.set.language.level", required.getPresentableText());
    return holder.newAnnotation(severity, message)
      .range(element)
      .withFix(HaxeFixer.create(fixText, () -> HaxeLanguageLevelUtil.setLanguageLevel(element, required)));
  }

  /**
   * "«construct» were removed in Haxe X" error for constructs valid only
   * below the module's language level. The quickfix lowers the level to the
   * last one still supporting the construct. Callers must {@code create()}.
   */
  public static @NotNull AnnotationBuilder removedAtLanguageLevel(@NotNull AnnotationHolder holder,
                                                                  @NotNull PsiElement element,
                                                                  @NotNull HaxeLanguageLevel removedIn,
                                                                  @NotNull String featureName) {
    HaxeLanguageLevel current = HaxeLanguageLevelUtil.getLanguageLevel(element);
    HaxeLanguageLevel lastSupported = removedIn.previous();
    String message = HaxeBundle.message("haxe.semantic.feature.removed.language.level",
                                        featureName, removedIn.getPresentableText(), current.getPresentableText());
    String fixText = HaxeBundle.message("haxe.quickfix.set.language.level", lastSupported.getPresentableText());
    return holder.newAnnotation(HighlightSeverity.ERROR, message)
      .range(element)
      .withFix(HaxeFixer.create(fixText, () -> HaxeLanguageLevelUtil.setLanguageLevel(element, lastSupported)));
  }

  public static @NotNull AnnotationBuilder typeMismatch(@NotNull AnnotationHolder holder,
                                                        @NotNull PsiElement incompatibleElement,
                                                        String incompatibleType,
                                                        String correctType) {

    String message = HaxeBundle.message("haxe.semantic.incompatible.type.0.should.be.1", incompatibleType, correctType);
    return holder.newAnnotation(HighlightSeverity.ERROR, message).range(incompatibleElement);
  }
  public static @NotNull AnnotationBuilder typeMismatchMissingMembers(@NotNull AnnotationHolder holder,
                                                                      @NotNull PsiElement incompatibleElement,
                                                                      AssignExplanation context) {

    String message = HaxeBundle.message("haxe.semantic.incompatible.type.missing.members.0",
                                        context.createMissingMembersMessage());
    return holder.newAnnotation(HighlightSeverity.ERROR, message).range(incompatibleElement);
  }
  public static @NotNull void addTypeMismatchWrongTypeMembersAnnotations(@NotNull AnnotationHolder holder,
                                                                         @NotNull PsiElement incompatibleElement,
                                                                         AssignExplanation context) {

    TextRange expectedRange = incompatibleElement.getTextRange();
    Map<PsiElement, String> wrongTypeMap = context.getWrongTypeMap();
    boolean allInRange = wrongTypeMap.keySet().stream().allMatch(psi -> expectedRange.contains(psi.getTextRange()));
    if (allInRange) {
      wrongTypeMap.forEach((key, value) -> holder.newAnnotation(HighlightSeverity.ERROR, value).range(key).create());
    }else {
      String message = HaxeBundle.message("haxe.semantic.incompatible.type.wrong.member.types.0",  context.createWrongTypeMembersMessage());
      holder.newAnnotation(HighlightSeverity.ERROR, message).range(incompatibleElement).create();
    }
  }
}
