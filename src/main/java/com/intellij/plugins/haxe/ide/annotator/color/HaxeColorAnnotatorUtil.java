package com.intellij.plugins.haxe.ide.annotator.color;

import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.ide.highlight.HaxeSyntaxHighlighterColors;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HaxeColorAnnotatorUtil {

  public static void annotate(AnnotationHolder holder, PsiElement node, TextAttributesKey textAttributesKey) {
    holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
      .textAttributes(textAttributesKey)
      .range(node)
      .create();
  }
  public static void annotate(AnnotationHolder holder, PsiElement node, TextAttributesKey textAttributesKey, String text) {
    holder.newAnnotation(HighlightSeverity.INFORMATION, text)
      .textAttributes(textAttributesKey)
      .range(node)
      .create();
  }

  public static @NotNull AnnotationBuilder annotationBuilder(AnnotationHolder holder, TextRange range, TextAttributesKey textAttributesKey) {
    return holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
      .textAttributes(textAttributesKey)
      .range(range);

  }

  public static void colorizeKeyword(AnnotationHolder holder, PsiElement element) {
    TextAttributesKey attributesKey = TextAttributesKey.find(HaxeSyntaxHighlighterColors.HAXE_KEYWORD);
    holder.newSilentAnnotation(HighlightSeverity.INFORMATION).range(element).textAttributes(attributesKey).create();
  }

  @Nullable
  public static TextAttributesKey getAttributeByType(@Nullable HaxeComponentType type, final boolean isStatic) {
    if (type == null) {
      return null;
    }
    return switch (type) {
      case TYPE_PARAMETER -> TextAttributesKey.find(HaxeSyntaxHighlighterColors.HAXE_TYPE_PARAMETER);
      case CLASS, ENUM, TYPEDEF -> TextAttributesKey.find(HaxeSyntaxHighlighterColors.HAXE_CLASS);
      case INTERFACE -> TextAttributesKey.find(HaxeSyntaxHighlighterColors.HAXE_INTERFACE);
      case PARAMETER -> TextAttributesKey.find(HaxeSyntaxHighlighterColors.HAXE_PARAMETER);
      case VARIABLE -> TextAttributesKey.find(HaxeSyntaxHighlighterColors.HAXE_LOCAL_VARIABLE);
      case FIELD -> {
        if (isStatic) yield  TextAttributesKey.find(HaxeSyntaxHighlighterColors.HAXE_STATIC_MEMBER_VARIABLE);
        yield  TextAttributesKey.find(HaxeSyntaxHighlighterColors.HAXE_INSTANCE_MEMBER_VARIABLE);
      }
      case METHOD -> {
        if (isStatic) yield  TextAttributesKey.find(HaxeSyntaxHighlighterColors.HAXE_STATIC_MEMBER_FUNCTION);
        yield  TextAttributesKey.find(HaxeSyntaxHighlighterColors.HAXE_INSTANCE_MEMBER_FUNCTION);
      }
      default -> null;
    };
  }

}
