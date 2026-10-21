package com.intellij.plugins.haxe.ide.annotator.color;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.DumbAware;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.ide.conditional.settings.HaxeConditionalCompilationSettings;
import com.intellij.plugins.haxe.ide.highlight.HaxeSyntaxHighlighter;
import com.intellij.plugins.haxe.lang.psi.HaxeComponentName;
import com.intellij.plugins.haxe.lang.psi.HaxeIdentifier;
import com.intellij.plugins.haxe.lang.psi.HaxeNamedComponent;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeInactiveBody;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.ColorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;

/**
 * Dimmed syntax colors inside inactive conditional-compilation branches.
 * The highlighting lexer sees dead code as one PPBODY run (flat dead-code
 * color), so per-token colors can only come from the parsed chameleon PSI:
 * every leaf gets its normal color blended toward the editor background,
 * falling back to the default text color so nothing keeps the flat
 * dead-code color except whitespace. The strength comes from
 * {@link HaxeConditionalCompilationSettings}.
 */
public class HaxeInactiveCodeDimAnnotator implements Annotator, DumbAware {

  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (element instanceof PsiWhiteSpace || element instanceof HaxeInactiveBody) return;
    if (element.getTextLength() == 0) return;
    boolean leafOrComment = element.getFirstChild() == null || element instanceof PsiComment;
    if (!leafOrComment) return;

    // dim only content sitting DIRECTLY inside an inactive blob; anything
    // deeper inside a nested comment is covered by that comment's own dim
    PsiComment enclosing = PsiTreeUtil.getParentOfType(element, PsiComment.class);
    if (!(enclosing instanceof HaxeInactiveBody)) return;

    holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
      .range(element)
      .enforcedTextAttributes(dimmedAttributes(baseKey(element)))
      .create();
  }

  private static @Nullable TextAttributesKey baseKey(PsiElement element) {
    if (element instanceof PsiComment comment) {
      return HaxeSyntaxHighlighter.tokenAttributesKey(comment.getTokenType());
    }
    IElementType type = element.getNode().getElementType();
    TextAttributesKey key = HaxeSyntaxHighlighter.tokenAttributesKey(type);
    return key != null ? key : declarationKey(element);
  }

  /**
   * An identifier leaf under a declaration's component name gets that
   * declaration kind's color; everything else keeps plain (dimmed) text.
   */
  private static @Nullable TextAttributesKey declarationKey(PsiElement element) {
    if (!(element.getParent() instanceof HaxeIdentifier identifier)) return null;
    if (!(identifier.getParent() instanceof HaxeComponentName name)) return null;
    boolean isStatic = name.getParent() instanceof HaxeNamedComponent component && component.isStatic();
    return HaxeColorAnnotatorUtil.getAttributeByType(HaxeComponentType.typeOf(name.getParent()), isStatic);
  }

  private static TextAttributes dimmedAttributes(@Nullable TextAttributesKey key) {
    EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
    TextAttributes base = key != null ? scheme.getAttributes(key) : null;
    TextAttributes dimmed = base != null ? base.clone() : new TextAttributes();
    Color foreground = dimmed.getForegroundColor() != null ? dimmed.getForegroundColor() : scheme.getDefaultForeground();
    double dim = HaxeConditionalCompilationSettings.getInstance().getState().dimIntensityPercent / 100.0;
    dimmed.setForegroundColor(ColorUtil.mix(foreground, scheme.getDefaultBackground(), dim));
    dimmed.setBackgroundColor(null);
    dimmed.setErrorStripeColor(null);
    return dimmed;
  }
}
