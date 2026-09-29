package com.intellij.plugins.haxe.ide.annotator.color;

import com.intellij.plugins.haxe.ide.annotator.semantics.AnnotatorUtil;
import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.project.DumbAware;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;


public class HaxeInvalidEscapeAnnotator implements Annotator , DumbAware {

  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (AnnotatorUtil.shouldSkipColorAnnotation(element)) return;

    if (element instanceof HaxeStringLiteralExpression literal) {
      ASTNode node = literal.getNode();
      ASTNode childByType = node.findChildByType(HaxeTokenTypes.STRING_INVALID_ESCAPE);
      while(childByType != null) {
        holder.newAnnotation(HighlightSeverity.ERROR, HaxeBundle.message("haxe.inspections.string.escape.illegal"))
                .range(childByType)
                .create();
        // find next
        childByType = node.findChildByType(HaxeTokenTypes.STRING_INVALID_ESCAPE, childByType.getTreeNext());
      }
    }



  }
}
