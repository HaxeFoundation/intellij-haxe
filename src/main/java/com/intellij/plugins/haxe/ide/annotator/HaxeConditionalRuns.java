package com.intellij.plugins.haxe.ide.annotator;

import com.intellij.plugins.haxe.ide.annotator.semantics.AnnotatorUtil;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeInactiveBody;
import com.intellij.plugins.haxe.lang.util.HaxeConditionalExpression;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;

/**
 * Walks conditional-compilation chains in the PSI, for the annotators that
 * evaluate conditions. A condition is lexed as a run of consecutive
 * PPEXPRESSION leaves right after its {@code #if} or {@code #elseif}
 * directive; the run's first leaf is its head.
 */
final class HaxeConditionalRuns {
  private HaxeConditionalRuns() {
  }

  /** The run's {@code #if} or {@code #elseif} directive, null for any other leaf before it. */
  @Nullable
  static PsiElement directiveOf(@NotNull PsiElement runHead) {
    PsiElement directive = PsiTreeUtil.prevLeaf(runHead);
    if (directive == null) return null;
    IElementType type = directive.getNode().getElementType();
    return type == PPIF || type == PPELSEIF ? directive : null;
  }

  /** The first leaf of the branch the run guards, skipping whitespace; null at the end of the file. */
  @Nullable
  static PsiElement branchStart(@NotNull PsiElement runHead) {
    PsiElement leaf = PsiTreeUtil.nextLeaf(runHead);
    while (isConditionLeaf(leaf) || leaf instanceof PsiWhiteSpace) {
      leaf = PsiTreeUtil.nextLeaf(leaf);
    }
    return leaf;
  }

  /** The head of the run this condition leaf belongs to. */
  @NotNull
  static PsiElement runHead(@NotNull PsiElement expression) {
    PsiElement head = expression;
    for (PsiElement leaf = PsiTreeUtil.prevLeaf(head); isConditionLeaf(leaf); leaf = PsiTreeUtil.prevLeaf(leaf)) {
      head = leaf;
    }
    return head;
  }

  /** The condition's source text, all leaves of the run joined. */
  @NotNull
  static String runText(@NotNull PsiElement runHead) {
    StringBuilder text = new StringBuilder(runHead.getText());
    for (PsiElement leaf = PsiTreeUtil.nextLeaf(runHead); isConditionLeaf(leaf); leaf = PsiTreeUtil.nextLeaf(leaf)) {
      text.append(leaf.getText());
    }
    return text.toString();
  }

  private static boolean isConditionLeaf(@Nullable PsiElement leaf) {
    return leaf != null && leaf.getNode().getElementType() == HaxeTokenTypeSets.PPEXPRESSION;
  }

  /**
   * Whether the compiler evaluates the directive's condition at all: an
   * {@code #if} outside an inactive region; an {@code #elseif} only while no
   * earlier branch of its chain was taken.
   */
  static boolean wouldBeEvaluated(@NotNull PsiElement directive, @NotNull Map<String, String> definitions) {
    if (directive.getNode().getElementType() == PPIF) {
      return !inDeadRegion(directive);
    }
    return elseifWouldBeEvaluated(directive, definitions);
  }

  /**
   * Walks leaves backward to the chain-opening #if (skipping nested sections
   * by depth), collecting the earlier branches' conditions; the #elseif is
   * evaluated only when the chain start sits in live code and none of those
   * conditions holds.
   */
  private static boolean elseifWouldBeEvaluated(@NotNull PsiElement elseifDirective, @NotNull Map<String, String> definitions) {
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
        return noEarlierBranchTaken(earlierConditions, definitions);
      }
    }
    return false;  // no opening #if found - malformed chain, stay silent
  }

  private static boolean noEarlierBranchTaken(@NotNull List<String> earlierConditions, @NotNull Map<String, String> definitions) {
    for (String conditionText : earlierConditions) {
      HaxeConditionalExpression earlier = HaxeConditionalExpression.fromCondition(conditionText);
      if (earlier == null) return false;           // cannot model the chain - stay silent
      if (earlier.evaluate(definitions)) return false; // an earlier branch was taken
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
