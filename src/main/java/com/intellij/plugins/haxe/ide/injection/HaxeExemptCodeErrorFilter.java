package com.intellij.plugins.haxe.ide.injection;

import com.intellij.codeInsight.highlighting.HighlightErrorFilter;
import com.intellij.plugins.haxe.ide.annotator.semantics.AnnotatorUtil;
import com.intellij.psi.PsiErrorElement;
import org.jetbrains.annotations.NotNull;

/**
 * Analysis-exempt code (doc-comment fences, inactive conditional branches)
 * shows no parse errors: incomplete samples and other-target code are normal
 * there.
 */
public class HaxeExemptCodeErrorFilter extends HighlightErrorFilter {

  @Override
  public boolean shouldHighlightErrorElement(@NotNull PsiErrorElement element) {
    return !AnnotatorUtil.isInAnalysisExemptCode(element);
  }
}
