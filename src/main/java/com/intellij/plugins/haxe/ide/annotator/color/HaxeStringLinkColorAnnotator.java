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

/**
 * Paints string-literal navigation links (file paths, qualified names) with
 * their own configurable attributes (Settings | Editor | Color Scheme |
 * Haxe). Qualified-name and absolute-path references exist only when their
 * target resolves (the contributor pre-checks), so their presence is the
 * signal; a qualified name paints as one link over all its segments.
 * Relative path segments attach to every constant string, so they paint
 * only when the string reads as a path and its last segment resolves.
 */
public class HaxeStringLinkColorAnnotator implements Annotator {

  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (!(element instanceof HaxeStringLiteralExpression literal)) return;
    FileReference lastPathSegment = null;
    HaxeStringQnameReference firstNameSegment = null;
    HaxeStringQnameReference lastNameSegment = null;
    for (PsiReference reference : literal.getReferences()) {
      // relative paths come as per-segment FileReferences (attached on
      // SHAPE for completion) - only a fully resolved path paints as a link,
      // and the last segment's resolution decides that
      if (reference instanceof FileReference fileReference) {
        lastPathSegment = fileReference;
      }
      else if (reference instanceof HaxeStringQnameReference nameSegment) {
        if (firstNameSegment == null) firstNameSegment = nameSegment;
        lastNameSegment = nameSegment;
      }
      else if (reference instanceof HaxeStringFilePathReference) {
        paint(holder, literal, reference.getRangeInElement(), HaxeSyntaxHighlighterColors.STRING_FILE_LINK);
      }
    }
    // a qualified name's segments are separate references; the link spans the whole name
    if (firstNameSegment != null) {
      int start = firstNameSegment.getRangeInElement().getStartOffset();
      TextRange span = new TextRange(start, lastNameSegment.getRangeInElement().getEndOffset());
      paint(holder, literal, span, HaxeSyntaxHighlighterColors.STRING_CODE_LINK);
    }
    // segment references attach to every clean constant string so explicit
    // completion works from the first segment - painting stays reserved for
    // strings that READ as paths, so a bare word matching a file never
    // lights up
    boolean paintable = lastPathSegment != null
                        && HaxeStringFilePathReference.looksLikePath(contentOf(literal))
                        && lastPathSegment.resolve() != null;
    if (paintable) {
      TextRange span = new TextRange(1, lastPathSegment.getRangeInElement().getEndOffset());
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
}
