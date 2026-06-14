package com.intellij.plugins.haxe.ide.annotator.color;

import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.lang.parser.GeneratedParserUtilBase;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.haxelib.definitions.HaxeDefineDetectionManager;
import com.intellij.plugins.haxe.ide.highlight.HaxeSyntaxHighlighterColors;
import com.intellij.plugins.haxe.ide.intention.HaxeDefineIntention;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxeStringUtil;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

import static com.intellij.plugins.haxe.ide.annotator.color.HaxeColorAnnotatorUtil.colorizeKeyword;
import static com.intellij.plugins.haxe.ide.annotator.color.HaxeColorAnnotatorUtil.getAttributeByType;



public class HaxeInvalidEscapeAnnotator implements Annotator , DumbAware {

  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if(!element.isValid()) return;

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
