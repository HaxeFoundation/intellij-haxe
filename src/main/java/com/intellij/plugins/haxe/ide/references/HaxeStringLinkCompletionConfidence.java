package com.intellij.plugins.haxe.ide.references;

import com.intellij.codeInsight.completion.CompletionConfidence;
import com.intellij.openapi.editor.Editor;
import com.intellij.plugins.haxe.lang.psi.HaxeStringLiteralExpression;
import com.intellij.plugins.haxe.util.HaxeQnameResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ThreeState;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Pattern;

/**
 * Decides completion auto-popup inside string literals, authoritatively in
 * both directions (ordered directly after the Set Value guard, which owns
 * its debugger fragment and defers everywhere else): the platform would
 * otherwise allow auto-popup in
 * any string carrying references at the caret — and the path-completion
 * references attach to every clean constant string — so plain typing would
 * pop suggestions in ordinary prose. Instead the popup is allowed EXACTLY
 * while something real is being typed: a path fragment (a separator before
 * the caret), or a qualified name with TWO dots whose prefix the project
 * knows — a single dot is everyday prose ("etc." / "e.g"), two dots into a
 * known package/class is intent. Suppressed otherwise; explicit ctrl-space
 * is unaffected either way. The popup on the separator characters
 * themselves comes from the typed handler.
 */
public class HaxeStringLinkCompletionConfidence extends CompletionConfidence {

  // a qualified name with at least two dots, the segment under the caret
  // possibly still empty ("com.package." / "com.package.Cla")
  private static final Pattern QNAME_TWO_DOTS = Pattern.compile("[A-Za-z_]\\w*\\.\\w+\\.[\\w.]*");

  @Override
  public @NotNull ThreeState shouldSkipAutopopup(@NotNull Editor editor,
                                                 @NotNull PsiElement contextElement,
                                                 @NotNull PsiFile psiFile,
                                                 int offset) {
    HaxeStringLiteralExpression literal = PsiTreeUtil.getParentOfType(contextElement, HaxeStringLiteralExpression.class, false);
    if (literal == null) return ThreeState.UNSURE;
    int caretInLiteral = offset - literal.getTextRange().getStartOffset();
    if (caretInLiteral <= 1 || caretInLiteral > literal.getTextLength()) return ThreeState.YES;
    String beforeCaret = literal.getText().substring(1, caretInLiteral);
    if (beforeCaret.indexOf('/') >= 0) return ThreeState.NO;
    if (QNAME_TWO_DOTS.matcher(beforeCaret).matches()) {
      String parent = beforeCaret.substring(0, beforeCaret.lastIndexOf('.'));
      if (HaxeQnameResolveUtil.isKnownQnamePrefix(parent, psiFile.getProject())) return ThreeState.NO;
    }
    return ThreeState.YES;
  }
}
