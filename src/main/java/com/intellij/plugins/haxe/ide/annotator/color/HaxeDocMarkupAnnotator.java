package com.intellij.plugins.haxe.ide.annotator.color;

import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.ide.annotator.semantics.AnnotatorUtil;
import com.intellij.plugins.haxe.ide.documentation.settings.HaxeDocSettings;
import com.intellij.plugins.haxe.ide.highlight.HaxeSyntaxHighlighterColors;
import com.intellij.plugins.haxe.lang.lexer.HaxeDocTokenTypes;
import com.intellij.plugins.haxe.lang.parser.HaxeDocMarkdown;
import com.intellij.plugins.haxe.lang.parser.HaxePsiDocCommentImpl;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Doc comment markup coloring: haxedoc tags (@param, @see...) get the doc-tag
 * attribute, inline markdown code spans the doc-code attribute. Fenced code
 * blocks are handled by language injection instead (see HaxeDocFenceInjector),
 * so this only touches prose lines.
 */
public class HaxeDocMarkupAnnotator implements Annotator, DumbAware {

  // an inline code span on one line: `content` without inner backticks
  private static final Pattern INLINE_SPAN = Pattern.compile("`[^`\r\n]+`");

  private static final TokenSet DOC_TAG_TOKEN = TokenSet.create(HaxeDocTokenTypes.DOC_TAG_NAME);

  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (!(element instanceof HaxePsiDocCommentImpl docComment)) return;
    if (!HaxeDocSettings.getInstance().getState().highlightDocMarkup) return;
    // a doc comment in a dead branch keeps its uniform dimmed doc color
    // (HaxeInactiveCodeDimAnnotator) - markup accents would punch through it
    if (AnnotatorUtil.isInInactiveBranch(docComment)) return;

    for (ASTNode tag : docComment.getNode().getChildren(DOC_TAG_TOKEN)) {
      annotate(holder, tag.getTextRange(), HaxeSyntaxHighlighterColors.DOC_TAG);
    }
    for (HaxeDocMarkdown.DocLine line : HaxeDocMarkdown.scan(docComment).proseLines()) {
      Matcher matcher = INLINE_SPAN.matcher(line.text());
      while (matcher.find()) {
        TextRange range = TextRange.create(line.startOffset() + matcher.start(), line.startOffset() + matcher.end());
        annotate(holder, range, HaxeSyntaxHighlighterColors.DOC_CODE);
      }
    }
  }

  private static void annotate(@NotNull AnnotationHolder holder, @NotNull TextRange range, @NotNull TextAttributesKey key) {
    holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
      .range(range)
      .textAttributes(key)
      .create();
  }
}
