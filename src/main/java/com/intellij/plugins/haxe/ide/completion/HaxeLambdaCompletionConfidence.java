package com.intellij.plugins.haxe.ide.completion;

import com.intellij.codeInsight.completion.CompletionConfidence;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ThreeState;
import org.jetbrains.annotations.NotNull;

/**
 * No completion auto-popup while a lambda template's parameter stop is
 * being filled in: the stop has its own lookup of name suggestions, and code
 * completion there would offer the code's symbols, the lambda included, to a
 * user who is typing a name. Explicit completion is unaffected.
 */
public class HaxeLambdaCompletionConfidence extends CompletionConfidence {

  @Override
  public @NotNull ThreeState shouldSkipAutopopup(@NotNull Editor editor,
                                                 @NotNull PsiElement contextElement,
                                                 @NotNull PsiFile psiFile,
                                                 int offset) {
    boolean fillingIn = HaxeLambdaLookups.isLambdaTemplateActive(psiFile.getProject(), editor.getDocument());
    return fillingIn ? ThreeState.YES : ThreeState.UNSURE;
  }
}
