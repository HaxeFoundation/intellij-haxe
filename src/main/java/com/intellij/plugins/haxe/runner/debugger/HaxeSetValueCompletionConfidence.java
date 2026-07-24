package com.intellij.plugins.haxe.runner.debugger;

import com.intellij.codeInsight.completion.CompletionConfidence;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ThreeState;
import org.jetbrains.annotations.NotNull;

/**
 * Keeps completion from auto-popping in the debugger's Set Value editor (a
 * fragment tagged {@link HaxeDebuggerEditorsProvider#LITERAL_VALUE_INPUT}):
 * the expected input is a plain value, and an uninvited lookup steals the
 * Enter that should submit it. Explicit completion (Ctrl+Space) is untouched,
 * as are the evaluate/watches editors.
 */
public class HaxeSetValueCompletionConfidence extends CompletionConfidence {
  @NotNull
  @Override
  public ThreeState shouldSkipAutopopup(@NotNull Editor editor, @NotNull PsiElement contextElement,
                                        @NotNull PsiFile psiFile, int offset) {
    return Boolean.TRUE.equals(psiFile.getUserData(HaxeDebuggerEditorsProvider.LITERAL_VALUE_INPUT))
           ? ThreeState.YES : ThreeState.UNSURE;
  }
}
