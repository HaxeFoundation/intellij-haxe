package com.intellij.plugins.haxe.ide.annotator.color;

import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.lang.parser.GeneratedParserUtilBase;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.config.HaxeProjectSettings;
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

import java.util.Set;

import static com.intellij.plugins.haxe.ide.annotator.color.HaxeColorAnnotatorUtil.colorizeKeyword;
import static com.intellij.plugins.haxe.ide.annotator.color.HaxeColorAnnotatorUtil.getAttributeByType;


/**
 *  DumbAware Color annnotator that only looks on types and won't try to resolve anything.
 *  DumbAware will add color attributes even if  project is being indexed
 *
 *  - handles keywords
 *  - operators
 *  - comments and preprosessor / conditional compilation
 */
public class HaxeFastColorAnnotator implements Annotator , DumbAware {
  @Override
  public void annotate(@NotNull PsiElement node, @NotNull AnnotationHolder holder) {
    if (node instanceof PsiWhiteSpace) return;

    if (node instanceof HaxePsiToken token) {

      if (isNewOperator(token) || isIsOperator(token)) {
        colorizeKeyword(holder, token);
      }

      if (isKeyword(token)) {
        colorizeKeyword(holder, token);
      }

    }else if (node instanceof PsiComment) {
      ppelements(node, holder);
    }else if (node instanceof HaxeComponentName componentName) {
      checkComponentName(componentName, holder);
    }
  }

  private static void ppelements(@NotNull PsiElement node, @NotNull AnnotationHolder holder) {
    final ASTNode astNode = node.getNode();
    if (astNode == null) return;
    IElementType tt = astNode.getElementType();

    if (tt == HaxeTokenTypeSets.PPEXPRESSION) {
      annotateCompilationExpression(node, holder);
    }
    else if (tt == HaxeTokenTypeSets.PPBODY) {
      holder.newSilentAnnotation(HighlightSeverity.INFORMATION).range(node)
        .textAttributes(HaxeSyntaxHighlighterColors.CONDITIONALLY_NOT_COMPILED).create();
    }
    else if (tt == GeneratedParserUtilBase.DUMMY_BLOCK) {
      holder.newAnnotation(HighlightSeverity.INFORMATION, "Unparseable data").range(node)
        .textAttributes(HaxeSyntaxHighlighterColors.UNPARSEABLE_DATA).create();
    }
  }

  private static void annotateCompilationExpression(PsiElement node, AnnotationHolder holder) {
    final Set<String> definitions = HaxeProjectSettings.getInstance(node.getProject()).getUserCompilerDefinitionsAsSet();
    final String nodeText = node.getText();
    for (Pair<String, Integer> pair : HaxeStringUtil.getWordsWithOffset(nodeText)) {
      final String word = pair.getFirst();
      final int offset = pair.getSecond();
      final int absoluteOffset = node.getTextOffset() + offset;
      final TextRange range = new TextRange(absoluteOffset, absoluteOffset + word.length());

      final String attributeName = definitions.contains(word) ? HaxeSyntaxHighlighterColors.HAXE_DEFINED_VAR
                                                              : HaxeSyntaxHighlighterColors.HAXE_UNDEFINED_VAR;

      TextAttributesKey attributes = TextAttributesKey.find(attributeName);

      HaxeColorAnnotatorUtil.annotationBuilder(holder, range, attributes)
        .withFix(new HaxeDefineIntention(word, definitions.contains(word)))
        .create();
    }
  }



  private static boolean isNewOperator(HaxePsiToken token) {
    if(token.getParent() instanceof HaxeNewExpression) {
      return token.getTokenType() ==  HaxeTokenTypes.ONEW;
    }
    return false;
  }

  private static boolean isIsOperator(HaxePsiToken token) {
    if(token.getParent() instanceof HaxeIsTypeExpression) {
      return  token.getTokenType() == HaxeTokenTypes.OBIT_OR_ASSIGN;
    }
    return false;
  }

  /**
   * Checks for keywords that are NOT PsiStatements; those are handled by IDEA.
   */
  private static boolean isKeyword(HaxePsiToken token) {
      PsiElement parent = token.getParent();
      if (parent instanceof HaxeForStatement) {
        return token.getTokenType() == HaxeTokenTypes.OIN;
      }
      else if (parent instanceof HaxeAbstractFromType) {
        return token.getTokenType() == HaxeTokenTypes.KFROM;
      }
      else if (parent instanceof HaxeAbstractToType) {
        return token.getTokenType() == HaxeTokenTypes.KTO;
      }
      else if (parent instanceof HaxeImportStatement importStatement && importStatement.getAlias() != null) {
        return token.textMatches("in") || token.textMatches("as");
      }
    return false;
  }


  private static boolean checkStatic(PsiElement parent) {
    return (parent instanceof HaxeNamedComponent component) && component.isStatic();
  }

  private static void checkComponentName(@NotNull HaxeComponentName node, @NotNull AnnotationHolder holder) {
    PsiElement element = node;
    final boolean isStatic = PsiTreeUtil.getParentOfType(node, HaxeImportStatement.class) == null && checkStatic(element.getParent());
    final TextAttributesKey attribute = getAttributeByType(HaxeComponentType.typeOf(element.getParent()), isStatic);
    if (attribute != null) {
      if (node instanceof HaxeReference reference) {
        element = reference.getReferenceNameElement();
        if (element instanceof HaxeComponentName name) node = name;
      }
      holder.newSilentAnnotation(HighlightSeverity.INFORMATION).range(node).textAttributes(attribute).create();
    }
  }
}
