package com.intellij.plugins.haxe.ide.annotator;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.LocalQuickFixBackedByIntentionAction;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * One reporting seam for semantic checks, so the same check code can run as
 * an annotator (on-the-fly errors in the highlighting pass) or as a
 * {@link com.intellij.codeInspection.LocalInspectionTool} (profile-controlled
 * severity, suppression, batch runs under the Haxe group). The platform's
 * AnnotationHolder/AnnotationBuilder are {@code @ApiStatus.NonExtendable}, so
 * this is a parallel builder, not an implementation of them.
 *
 * In inspection mode a problem whose severity equals the tool's DEFAULT level
 * follows the profile (the user's severity choice wins); any other severity is
 * emitted verbatim — a weak-warning detail inside an error-level tool stays a
 * weak warning.
 */
public final class HaxeProblemReporter {

  private final @Nullable AnnotationHolder annotations;
  private final @Nullable ProblemsHolder problems;
  private final @Nullable HighlightSeverity toolDefaultLevel;

  private HaxeProblemReporter(@Nullable AnnotationHolder annotations,
                              @Nullable ProblemsHolder problems,
                              @Nullable HighlightSeverity toolDefaultLevel) {
    this.annotations = annotations;
    this.problems = problems;
    this.toolDefaultLevel = toolDefaultLevel;
  }

  public static HaxeProblemReporter of(@NotNull AnnotationHolder holder) {
    return new HaxeProblemReporter(holder, null, null);
  }

  public static HaxeProblemReporter of(@NotNull ProblemsHolder holder, @NotNull HighlightSeverity toolDefaultLevel) {
    return new HaxeProblemReporter(null, holder, toolDefaultLevel);
  }

  @NotNull
  public Problem problem(@NotNull HighlightSeverity severity, @NotNull String message) {
    return new Problem(severity, message);
  }

  /** Builder for one problem; a {@link #range} is REQUIRED before {@link #create()}. */
  public final class Problem {
    private final HighlightSeverity severity;
    private final String message;
    private final List<IntentionAction> fixes = new ArrayList<>();
    private PsiElement element;
    private TextRange textRange;

    private Problem(@NotNull HighlightSeverity severity, @NotNull String message) {
      this.severity = severity;
      this.message = message;
    }

    @NotNull
    public Problem range(@NotNull PsiElement rangeElement) {
      this.element = rangeElement;
      return this;
    }

    /** An absolute document range; in inspection mode it anchors to the smallest containing element. */
    @NotNull
    public Problem range(@NotNull TextRange absoluteRange) {
      this.textRange = absoluteRange;
      return this;
    }

    @NotNull
    public Problem withFix(@NotNull IntentionAction fix) {
      fixes.add(fix);
      return this;
    }

    public void create() {
      if (annotations != null) {
        createAnnotation();
      }
      else {
        registerProblem();
      }
    }

    private void createAnnotation() {
      AnnotationBuilder builder = annotations.newAnnotation(severity, message);
      if (textRange != null) {
        builder = builder.range(textRange);
      }
      else if (element != null) {
        builder = builder.range(element);
      }
      for (IntentionAction fix : fixes) {
        builder = builder.withFix(fix);
      }
      builder.create();
    }

    private void registerProblem() {
      PsiElement anchor = element;
      TextRange rangeInAnchor = null;
      if (anchor == null && textRange != null) {
        anchor = smallestElementContaining(problems.getFile(), textRange);
        if (anchor == null) return;
        rangeInAnchor = textRange.shiftLeft(anchor.getTextRange().getStartOffset());
      }
      if (anchor == null) throw new IllegalStateException("problem created without a range: " + message);

      ProblemHighlightType highlightType = highlightTypeFor(severity);
      ProblemDescriptor descriptor = problems.getManager()
        .createProblemDescriptor(anchor, rangeInAnchor, message, highlightType, problems.isOnTheFly(), quickFixes());
      problems.registerProblem(descriptor);
    }

    private LocalQuickFix[] quickFixes() {
      return fixes.stream()
        .map(Problem::asQuickFix)
        .toArray(LocalQuickFix[]::new);
    }

    private static LocalQuickFix asQuickFix(IntentionAction fix) {
      return fix instanceof LocalQuickFix quickFix ? quickFix : new LocalQuickFixBackedByIntentionAction(fix);
    }
  }

  @NotNull
  private ProblemHighlightType highlightTypeFor(@NotNull HighlightSeverity severity) {
    if (severity.equals(toolDefaultLevel)) return ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
    if (severity == HighlightSeverity.ERROR) return ProblemHighlightType.GENERIC_ERROR;
    if (severity == HighlightSeverity.WARNING) return ProblemHighlightType.WARNING;
    if (severity == HighlightSeverity.WEAK_WARNING) return ProblemHighlightType.WEAK_WARNING;
    return ProblemHighlightType.INFORMATION;
  }

  @Nullable
  private static PsiElement smallestElementContaining(@NotNull PsiFile file, @NotNull TextRange range) {
    PsiElement element = file.findElementAt(range.getStartOffset());
    while (element != null && !element.getTextRange().contains(range)) {
      element = element.getParent();
    }
    return element;
  }
}
