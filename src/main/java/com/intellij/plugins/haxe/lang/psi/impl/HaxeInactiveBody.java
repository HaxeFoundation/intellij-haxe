package com.intellij.plugins.haxe.lang.psi.impl;

import com.intellij.plugins.haxe.lang.lexer.HaxeInactiveBodyElementType;
import com.intellij.psi.PsiComment;
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An inactive conditional-compilation branch: still a PsiComment to every
 * existing consumer (dead code stays comment-like), but lazily parseable into
 * a real sub-tree for formatting, highlighting, references and completion.
 * Content that cannot parse cleanly keeps the parser's error recovery, so
 * only the broken spot loses structure - never the whole branch.
 */
public class HaxeInactiveBody extends LazyParseablePsiElement implements PsiComment {

  public HaxeInactiveBody(@NotNull IElementType type, @Nullable CharSequence text) {
    super(type, text);
  }

  @Override
  public @NotNull IElementType getTokenType() {
    return getElementType();
  }

  // the label PsiCommentImpl used - keeps parse-tree dumps (and their test goldens) stable
  @Override
  public String toString() {
    return "PsiComment(" + getElementType() + ")";
  }

  /**
   * Whether some parser entry parsed the branch without errors. Formatting
   * rebuilds only clean branches; a recovered (error-carrying) parse is
   * preserved verbatim, like the reference formatter's own fallback. Touching
   * the first child forces the lazy parse that records the entry.
   */
  public boolean hasCleanParse() {
    return getNode().getFirstChildNode() != null
           && getNode().getUserData(HaxeInactiveBodyElementType.CLEAN_PARSE_ENTRY) != null;
  }
}
