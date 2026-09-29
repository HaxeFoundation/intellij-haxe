package com.intellij.plugins.haxe.ide.references;

import com.intellij.plugins.haxe.lang.psi.HaxeStringLiteralExpression;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Shared lookups for the string-link feature family. */
final class HaxeStringLiterals {

  private HaxeStringLiterals() {
  }

  /** The string literal whose content the caret sits in, or null. */
  @Nullable
  static HaxeStringLiteralExpression literalAtCaret(@NotNull PsiFile file, int caretOffset) {
    PsiElement beforeCaret = file.findElementAt(Math.max(caretOffset - 1, 0));
    return PsiTreeUtil.getParentOfType(beforeCaret, HaxeStringLiteralExpression.class, false);
  }

  /** The literal's text from just after the opening quote up to the caret, or null when the caret precedes it. */
  @Nullable
  static String contentBeforeCaret(@NotNull HaxeStringLiteralExpression literal, @NotNull PsiFile file, int caretOffset) {
    int contentStart = literal.getTextRange().getStartOffset() + 1;
    if (caretOffset < contentStart) return null;
    return file.getText().substring(contentStart, caretOffset);
  }
}
