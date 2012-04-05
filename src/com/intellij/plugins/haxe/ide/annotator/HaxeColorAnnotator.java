package com.intellij.plugins.haxe.ide.annotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.ide.highlight.HaxeSyntaxHighlighterColors;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeColorAnnotator implements Annotator {
  @Override
  public void annotate(@NotNull PsiElement node, @NotNull AnnotationHolder holder) {
    if (isNewOperator(node)) {
      holder.createInfoAnnotation(node, null).setTextAttributes(TextAttributesKey.find(HaxeSyntaxHighlighterColors.HAXE_KEYWORD));
    }

    if(ApplicationManager.getApplication().isInternal()){
      annotateInternal(node, holder);
    }
  }

  private void annotateInternal(PsiElement node, AnnotationHolder holder) {
    PsiElement element = node;
    if (element instanceof HaxeReference) {
      final boolean chain = PsiTreeUtil.getChildOfType(element, HaxeReference.class) != null;
      if (chain) {
        tryAnnotateQName(node, holder);
        return;
      }
      element = ((HaxeReference)element).resolve();
    }
    if (element instanceof HaxeComponentName) {
      final boolean isStatic = checkStatic(element.getParent());
      final TextAttributesKey attribute = getAttributeByType(HaxeComponentType.typeOf(element.getParent()), isStatic);
      if (attribute != null) {
        holder.createInfoAnnotation(node, null).setTextAttributes(attribute);
      }
    }
  }

  private static boolean isNewOperator(PsiElement element) {
    return HaxeTokenTypes.ONEW.toString().equals(element.getText()) &&
           element.getParent() instanceof HaxeNewExpression;
  }

  private static void tryAnnotateQName(PsiElement node, AnnotationHolder holder) {
    // Maybe this is class name
    final HaxeClass resultClass = HaxeResolveUtil.resolveClass(node);
    if (resultClass != null) {
      final TextAttributesKey attribute = getAttributeByType(HaxeComponentType.typeOf(resultClass), false);
      if (attribute != null) {
        holder.createInfoAnnotation(node, null).setTextAttributes(attribute);
      }
    }
  }

  private static boolean checkStatic(PsiElement parent) {
    if (parent instanceof HaxeNamedComponent) {
      return ((HaxeNamedComponent)parent).isStatic();
    }
    return false;
  }

  @Nullable
  private static TextAttributesKey getAttributeByType(HaxeComponentType type, boolean isStatic) {
    switch (type) {
      case CLASS:
      case ENUM:
      case TYPEDEF:
        return TextAttributesKey.find(HaxeSyntaxHighlighterColors.HAXE_CLASS);
      case INTERFACE:
        return TextAttributesKey.find(HaxeSyntaxHighlighterColors.HAXE_INTERFACE);
      case PARAMETER:
        return TextAttributesKey.find(HaxeSyntaxHighlighterColors.HAXE_PARAMETER);
      case VARIABLE:
        return TextAttributesKey.find(HaxeSyntaxHighlighterColors.HAXE_LOCAL_VARIABLE);
      case FIELD:
        if (isStatic) return TextAttributesKey.find(HaxeSyntaxHighlighterColors.HAXE_STATIC_MEMBER_VARIABLE);
        return TextAttributesKey.find(HaxeSyntaxHighlighterColors.HAXE_INSTANCE_MEMBER_VARIABLE);
      case METHOD:
        if (isStatic) return TextAttributesKey.find(HaxeSyntaxHighlighterColors.HAXE_STATIC_MEMBER_FUNCTION);
        return TextAttributesKey.find(HaxeSyntaxHighlighterColors.HAXE_INSTANCE_MEMBER_FUNCTION);
      default:
        return null;
    }
  }
}
