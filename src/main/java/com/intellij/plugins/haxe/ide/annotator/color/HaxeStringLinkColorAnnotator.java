package com.intellij.plugins.haxe.ide.annotator.color;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.plugins.haxe.ide.highlight.HaxeSyntaxHighlighterColors;
import com.intellij.plugins.haxe.ide.references.HaxeStringFilePathReference;
import com.intellij.plugins.haxe.ide.references.HaxeStringQnameReference;
import com.intellij.plugins.haxe.lang.psi.HaxeStringLiteralExpression;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Paints string-literal navigation links (file paths, qualified names) with
 * their own configurable attributes (Settings | Editor | Color Scheme |
 * Haxe). The references only EXIST when their target resolved — the
 * contributor pre-checks — so presence is the whole signal; no resolution
 * happens here beyond what reference collection already did.
 */
public class HaxeStringLinkColorAnnotator implements Annotator {

  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (!(element instanceof HaxeStringLiteralExpression literal)) return;
    FileReference lastSegment = null;
    for (PsiReference reference : literal.getReferences()) {
      // relative paths come as per-segment FileReferences (attached on
      // SHAPE for completion) - only a fully resolved path paints as a link,
      // and the last segment's resolution decides that
      if (reference instanceof FileReference fileReference) {
        lastSegment = fileReference;
        continue;
      }
      TextAttributesKey key = keyFor(reference);
      if (key == null) continue;
      paint(holder, literal, reference.getRangeInElement(), key);
    }
    // segment references attach to every clean constant string so explicit
    // completion works from the first segment - painting stays reserved for
    // strings that READ as paths, so a bare word matching a file never
    // lights up
    boolean paintable = lastSegment != null
                        && HaxeStringFilePathReference.looksLikePath(contentOf(literal))
                        && lastSegment.resolve() != null;
    if (paintable) {
      TextRange span = new TextRange(1, lastSegment.getRangeInElement().getEndOffset());
      paint(holder, literal, span, HaxeSyntaxHighlighterColors.STRING_FILE_LINK);
    }
  }

  /** The literal's text between the quotes. */
  @NotNull
  private static String contentOf(@NotNull HaxeStringLiteralExpression literal) {
    String text = literal.getText();
    return text.length() >= 2 ? text.substring(1, text.length() - 1) : "";
  }

  private static void paint(@NotNull AnnotationHolder holder,
                            @NotNull HaxeStringLiteralExpression literal,
                            @NotNull TextRange rangeInLiteral,
                            @NotNull TextAttributesKey key) {
    holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
      .range(rangeInLiteral.shiftRight(literal.getTextRange().getStartOffset()))
      .textAttributes(key)
      .create();
  }

  @Nullable
  private static TextAttributesKey keyFor(@NotNull PsiReference reference) {
    if (reference instanceof HaxeStringFilePathReference) return HaxeSyntaxHighlighterColors.STRING_FILE_LINK;
    if (reference instanceof HaxeStringQnameReference) return HaxeSyntaxHighlighterColors.STRING_CODE_LINK;
    return null;
  }
}
