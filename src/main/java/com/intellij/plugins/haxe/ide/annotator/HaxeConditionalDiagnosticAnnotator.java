package com.intellij.plugins.haxe.ide.annotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.AnnotationSession;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.plugins.haxe.ide.annotator.semantics.AnnotatorUtil;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeInactiveBody;
import com.intellij.plugins.haxe.lang.util.HaxeConditionalExpression;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;

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

    PsiElement runHead = runHead(comment);
    String message = runVerdict(runHead, comment.getProject(), holder);
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
  private static String runVerdict(@NotNull PsiElement runHead, @NotNull Project project, @NotNull AnnotationHolder holder) {
    AnnotationSession session = holder.getCurrentAnnotationSession();
    Map<PsiElement, String> verdicts = session.getUserData(RUN_VERDICTS);
    if (verdicts == null) {
      // benign race: a concurrently created map only costs a recomputation
      verdicts = new ConcurrentHashMap<>();
      session.putUserData(RUN_VERDICTS, verdicts);
    }
    return verdicts.computeIfAbsent(runHead, head -> computeVerdict(head, project));
  }

  @NotNull
  private static String computeVerdict(@NotNull PsiElement runHead, @NotNull Project project) {
    PsiElement directive = PsiTreeUtil.prevLeaf(runHead);
    if (directive == null) return NO_ERROR;
    IElementType directiveType = directive.getNode().getElementType();
    if (directiveType != PPIF && directiveType != PPELSEIF) return NO_ERROR;
    if (!wouldBeEvaluated(directive, directiveType, project)) return NO_ERROR;

    HaxeConditionalExpression condition = HaxeConditionalExpression.fromCondition(runText(runHead));
    if (condition == null) return NO_ERROR;
    String message = condition.diagnose(project);
    return message == null ? NO_ERROR : message;
  }

  /** The first PPEXPRESSION leaf of the run this leaf belongs to. */
  @NotNull
  private static PsiElement runHead(@NotNull PsiElement expression) {
    PsiElement head = expression;
    for (PsiElement leaf = PsiTreeUtil.prevLeaf(head); isConditionLeaf(leaf); leaf = PsiTreeUtil.prevLeaf(leaf)) {
      head = leaf;
    }
    return head;
  }

  @NotNull
  private static String runText(@NotNull PsiElement runHead) {
    StringBuilder text = new StringBuilder(runHead.getText());
    for (PsiElement leaf = PsiTreeUtil.nextLeaf(runHead); isConditionLeaf(leaf); leaf = PsiTreeUtil.nextLeaf(leaf)) {
      text.append(leaf.getText());
    }
    return text.toString();
  }

  private static boolean isConditionLeaf(@Nullable PsiElement leaf) {
    return leaf != null && leaf.getNode().getElementType() == HaxeTokenTypeSets.PPEXPRESSION;
  }

  private static boolean wouldBeEvaluated(@NotNull PsiElement directive, @NotNull IElementType directiveType, @NotNull Project project) {
    if (directiveType == PPIF) {
      return !inDeadRegion(directive);
    }
    return elseifWouldBeEvaluated(directive, project);
  }

  /**
   * Walks leaves backward to the chain-opening #if (skipping nested sections
   * by depth), collecting the earlier branches' conditions; the #elseif is
   * evaluated only when the chain start sits in live code and none of those
   * conditions holds.
   */
  private static boolean elseifWouldBeEvaluated(@NotNull PsiElement elseifDirective, @NotNull Project project) {
    List<String> earlierConditions = new ArrayList<>();
    StringBuilder pendingCondition = null;
    int depth = 0;
    for (PsiElement leaf = PsiTreeUtil.prevLeaf(elseifDirective); leaf != null; leaf = PsiTreeUtil.prevLeaf(leaf)) {
      IElementType type = leaf.getNode().getElementType();
      if (type == PPEND) {
        depth++;
      }
      else if (type == HaxeTokenTypeSets.PPEXPRESSION && depth == 0) {
        // condition leaves precede their directive in this walk - prepend
        if (pendingCondition == null) pendingCondition = new StringBuilder();
        pendingCondition.insert(0, leaf.getText());
      }
      else if (type == PPELSEIF && depth == 0) {
        if (pendingCondition != null) earlierConditions.add(pendingCondition.toString());
        pendingCondition = null;
      }
      else if (type == PPIF) {
        if (depth > 0) {
          depth--;
          pendingCondition = null;
          continue;
        }
        if (pendingCondition != null) earlierConditions.add(pendingCondition.toString());
        if (inDeadRegion(leaf)) return false;
        return noEarlierBranchTaken(earlierConditions, project);
      }
    }
    return false;  // no opening #if found - malformed chain, stay silent
  }

  private static boolean noEarlierBranchTaken(@NotNull List<String> earlierConditions, @NotNull Project project) {
    for (String conditionText : earlierConditions) {
      HaxeConditionalExpression earlier = HaxeConditionalExpression.fromCondition(conditionText);
      if (earlier == null) return false;           // cannot model the chain - stay silent
      if (earlier.evaluate(project)) return false; // an earlier branch was taken
    }
    return true;
  }

  /**
   * Whether the directive sits inside an INACTIVE outer region: the nearest
   * preceding content is dead-branch material. Even a whitespace leaf counts
   * when it lives inside the dead blob (a whitespace-only region is one).
   */
  private static boolean inDeadRegion(@NotNull PsiElement directive) {
    for (PsiElement before = PsiTreeUtil.prevLeaf(directive); before != null; before = PsiTreeUtil.prevLeaf(before)) {
      if (before instanceof HaxeInactiveBody || AnnotatorUtil.isInInactiveBranch(before)) return true;
      if (!(before instanceof PsiWhiteSpace)) return false;
    }
    return false;
  }
}
