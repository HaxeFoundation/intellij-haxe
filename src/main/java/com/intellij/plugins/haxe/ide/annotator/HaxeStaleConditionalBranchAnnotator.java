package com.intellij.plugins.haxe.ide.annotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.ide.annotator.semantics.AnnotatorUtil;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeInactiveBody;
import com.intellij.plugins.haxe.lang.util.HaxeConditionalExpression;
import com.intellij.plugins.haxe.v2.buildtools.info.HaxeDefineContextService;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;

/**
 * Re-lexes a file whose {@code #if} branches disagree with its current
 * defines. The lexer fixes each branch as live or dead when it runs; when
 * the defines change, the define context asks for a re-lex, and this
 * annotator is the safety net for a request that did not reach an open
 * file. On every highlighting pass it evaluates each condition with the
 * current defines and compares the verdict with the branch the lexer
 * produced: a dead branch whose condition now holds, or a live one whose
 * condition now fails. It asks once per define set and file, so a
 * disagreement the re-lex cannot settle is not retried until the defines
 * change again.
 */
public class HaxeStaleConditionalBranchAnnotator implements Annotator, DumbAware {
  /** The defines the last re-lex of a file was requested for. */
  private static final Key<Map<String, String>> RELEX_REQUESTED_FOR = Key.create("haxe.cc.relex.requested.for");

  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (!(element instanceof PsiComment comment)) return;
    if (comment.getTokenType() != HaxeTokenTypeSets.PPEXPRESSION) return;
    if (HaxeConditionalRuns.runHead(comment) != comment) return;
    PsiElement directive = HaxeConditionalRuns.directiveOf(comment);
    if (directive == null) return;
    PsiFile file = comment.getContainingFile();
    Map<String, String> definitions = HaxeConditionalExpression.definitionsFor(comment.getProject(), file);
    if (branchDisagrees(comment, directive, definitions)) {
      requestRelex(file, definitions);
    }
  }

  /** Whether the branch the run guards should be live under the defines but was lexed dead, or the reverse. */
  private static boolean branchDisagrees(@NotNull PsiElement runHead,
                                         @NotNull PsiElement directive,
                                         @NotNull Map<String, String> definitions) {
    HaxeConditionalExpression condition = HaxeConditionalExpression.fromCondition(HaxeConditionalRuns.runText(runHead));
    if (condition == null) return false;
    PsiElement branchStart = HaxeConditionalRuns.branchStart(runHead);
    // an empty branch shows nothing to disagree with
    if (branchStart == null || isDirective(branchStart)) return false;
    boolean shouldBeLive = HaxeConditionalRuns.wouldBeEvaluated(directive, definitions) && condition.evaluate(definitions);
    boolean lexedDead = branchStart instanceof HaxeInactiveBody || AnnotatorUtil.isInInactiveBranch(branchStart);
    return shouldBeLive == lexedDead;
  }

  private static boolean isDirective(@NotNull PsiElement leaf) {
    IElementType type = leaf.getNode().getElementType();
    return type == PPELSEIF || type == PPELSE || type == PPEND;
  }

  private static void requestRelex(@NotNull PsiFile file, @NotNull Map<String, String> definitions) {
    VirtualFile virtualFile = HaxeDefineContextService.physicalFileOf(file);
    if (virtualFile == null) return;
    if (Objects.equals(virtualFile.getUserData(RELEX_REQUESTED_FOR), definitions)) return;
    virtualFile.putUserData(RELEX_REQUESTED_FOR, definitions);
    HaxeDefineContextService.getInstance(file.getProject()).relexFile(virtualFile);
  }
}
