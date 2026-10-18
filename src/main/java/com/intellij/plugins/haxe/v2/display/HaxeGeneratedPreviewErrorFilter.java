package com.intellij.plugins.haxe.v2.display;

import com.intellij.codeInsight.highlighting.HighlightErrorFilter;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * Generated-code previews show no parse errors: the compiler's dump printer
 * writes a few constructs no Haxe parser accepts (a comma between catch
 * clauses, a case body without its semicolon), and nobody can fix a
 * read-only reconstruction anyway.
 */
public final class HaxeGeneratedPreviewErrorFilter extends HighlightErrorFilter {

  @Override
  public boolean shouldHighlightErrorElement(@NotNull PsiErrorElement element) {
    PsiFile file = element.getContainingFile();
    return file == null || !HaxeGeneratedCodePreview.isPreview(file.getVirtualFile());
  }
}
