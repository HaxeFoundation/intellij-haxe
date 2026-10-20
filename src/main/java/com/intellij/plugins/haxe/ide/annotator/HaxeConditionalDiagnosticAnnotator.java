package com.intellij.plugins.haxe.ide.annotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.AnnotationSession;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.Key;
import com.intellij.plugins.haxe.ide.annotator.semantics.AnnotatorUtil;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.util.HaxeConditionalExpression;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Flags conditional-compilation conditions the compiler would hard-error on:
 * invalid version() literals, unknown functions, cross-type version
 * comparisons. The compiler validates a condition only when it actually
 * EVALUATES it - never inside an inactive outer region, and a #elseif only
 * while no earlier branch of its chain was taken (a bad version() in either
 * spot compiles cleanly) - so this flags exactly those. A condition lexes as
 * a RUN of PPEXPRESSION leaves; each leaf gets the annotation, painting the
 * whole condition.
 */
public class HaxeConditionalDiagnosticAnnotator implements Annotator, DumbAware {

  // one verdict per condition RUN, cached for the highlighting session -
  // every leaf of a run would otherwise redo the whole-chain
  // wouldBeEvaluated walk and the condition evaluation
  private static final Key<Map<PsiElement, String>> RUN_VERDICTS = Key.create("haxe.cc.condition.run.verdicts");
  // the cache map cannot hold null - a clean condition caches as this
  private static final String NO_ERROR = "";

  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (!(element instanceof PsiComment comment)) return;
    if (comment.getTokenType() != HaxeTokenTypeSets.PPEXPRESSION) return;
    if (AnnotatorUtil.isStaticAnalysisSuppressed(comment)) return;

    PsiElement runHead = HaxeConditionalRuns.runHead(comment);
    Map<String, String> definitions = HaxeConditionalExpression.definitionsFor(comment.getProject(), comment.getContainingFile());
    String message = runVerdict(runHead, definitions, holder);
    if (message.isEmpty()) return;
    if (comment == runHead) {
      // ONE real error per condition (a run is many leaves - per-leaf errors
      // would flood the problems view with identical entries)
      holder.newAnnotation(HighlightSeverity.ERROR, message).range(comment).create();
    }
    else {
      // the rest of the run continues the error styling without a new entry
      holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
        .textAttributes(CodeInsightColors.ERRORS_ATTRIBUTES)
        .range(comment)
        .create();
    }
  }

  /** The run's diagnostic ({@link #NO_ERROR} when clean), computed at most once per session. */
  @NotNull
  private static String runVerdict(@NotNull PsiElement runHead, @NotNull Map<String, String> definitions, @NotNull AnnotationHolder holder) {
    AnnotationSession session = holder.getCurrentAnnotationSession();
    Map<PsiElement, String> verdicts = session.getUserData(RUN_VERDICTS);
    if (verdicts == null) {
      // benign race: a concurrently created map only costs a recomputation
      verdicts = new ConcurrentHashMap<>();
      session.putUserData(RUN_VERDICTS, verdicts);
    }
    return verdicts.computeIfAbsent(runHead, head -> computeVerdict(head, definitions));
  }

  @NotNull
  private static String computeVerdict(@NotNull PsiElement runHead, @NotNull Map<String, String> definitions) {
    PsiElement directive = HaxeConditionalRuns.directiveOf(runHead);
    if (directive == null) return NO_ERROR;
    if (!HaxeConditionalRuns.wouldBeEvaluated(directive, definitions)) return NO_ERROR;

    HaxeConditionalExpression condition = HaxeConditionalExpression.fromCondition(HaxeConditionalRuns.runText(runHead));
    if (condition == null) return NO_ERROR;
    String message = condition.diagnose(definitions);
    return message == null ? NO_ERROR : message;
  }
}
