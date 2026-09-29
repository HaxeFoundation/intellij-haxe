package com.intellij.plugins.haxe.ide.injection;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.plugins.haxe.ide.annotator.semantics.AnnotatorUtil;
import org.jetbrains.annotations.NotNull;

/**
 * No completion runs inside a doc comment's injected code fences - they are
 * highlight-only (item insertion through the fragment's DocumentWindow is
 * not supported). Inactive conditional branches are NOT blocked: their
 * parsed PSI takes best-effort completion under the current defines - the
 * compiler arbitrates when the branch goes live.
 */
public class HaxeDocFenceCompletionBlocker extends CompletionContributor {

  @Override
  public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
    if (AnnotatorUtil.isInDocCodeFragment(parameters.getPosition())) {
      result.stopHere();
    }
  }
}
