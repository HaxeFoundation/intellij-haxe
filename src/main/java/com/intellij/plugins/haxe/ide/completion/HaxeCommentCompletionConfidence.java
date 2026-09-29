package com.intellij.plugins.haxe.ide.completion;

import com.intellij.codeInsight.completion.CompletionConfidence;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ThreeState;
import org.jetbrains.annotations.NotNull;

import static com.intellij.plugins.haxe.ide.completion.HaxeCommonCompletionPattern.inComment;

/** No completion auto-popup in comment. Explicit completion is unaffected. */
public class HaxeCommentCompletionConfidence extends CompletionConfidence {

  @Override
  public @NotNull ThreeState shouldSkipAutopopup(@NotNull Editor editor,
                                                 @NotNull PsiElement contextElement,
                                                 @NotNull PsiFile psiFile,
                                                 int offset) {
    return inComment.accepts(contextElement) ? ThreeState.YES : ThreeState.UNSURE;
  }
}
