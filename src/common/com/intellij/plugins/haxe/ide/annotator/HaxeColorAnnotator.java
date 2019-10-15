/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2017 Ilya Malanin
 * Copyright 2019 Eric Bishton
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.ide.annotator;

import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.parser.GeneratedParserUtilBase;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.config.HaxeProjectSettings;
import com.intellij.plugins.haxe.ide.highlight.HaxeSyntaxHighlighterColors;
import com.intellij.plugins.haxe.ide.intention.HaxeDefineIntention;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.plugins.haxe.util.HaxeStringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaToken;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeColorAnnotator implements Annotator {
  @Override
  public void annotate(@NotNull PsiElement node, @NotNull AnnotationHolder holder) {
    if (isNewOperator(node)) {
      holder.createInfoAnnotation(node, null).setTextAttributes(TextAttributesKey.find(HaxeSyntaxHighlighterColors.HAXE_KEYWORD));
    }

    PsiElement element = node;
    if (element instanceof HaxeStringLiteralExpression) {
      return;
    }
    if (element instanceof HaxeReference) {
      final boolean chain = PsiTreeUtil.getChildOfType(element, HaxeReference.class) != null;
      if (chain) {
        if (tryAnnotateQName(node, holder)) return;
      }
      element = ((HaxeReference)element).resolveToComponentName();
    }
    if (element instanceof HaxeComponentName) {
      final boolean isStatic = PsiTreeUtil.getParentOfType(node, HaxeImportStatement.class) == null && checkStatic(element.getParent());
      final TextAttributesKey attribute = getAttributeByType(HaxeComponentType.typeOf(element.getParent()), isStatic);
      if (attribute != null) {
        if (node instanceof HaxeReference) {
          element = ((HaxeReference)node).getReferenceNameElement();
          if (element != null) node = element;
        }
        holder.createInfoAnnotation(node, null).setTextAttributes(attribute);
      }
    }
    if (isKeyword(element)) {
      TextAttributesKey attributesKey = TextAttributesKey.find(HaxeSyntaxHighlighterColors.HAXE_KEYWORD);
      holder.createInfoAnnotation(node, null).setTextAttributes(attributesKey);
    }

    final ASTNode astNode = node.getNode();
    if (astNode != null) {
      IElementType tt = astNode.getElementType();

      if (tt == HaxeTokenTypeSets.PPEXPRESSION) {
        //annotateCompilationExpression(node, holder);
        //FIXME Temporary override:
        holder.createInfoAnnotation(node, null).setTextAttributes(HaxeSyntaxHighlighterColors.DEFINED_VAR);
      }
      if (tt == HaxeTokenTypeSets.PPBODY) {
        holder.createInfoAnnotation(node, null).setTextAttributes(HaxeSyntaxHighlighterColors.CONDITIONALLY_NOT_COMPILED);
      }
      if (tt == GeneratedParserUtilBase.DUMMY_BLOCK) {
        holder.createInfoAnnotation(node, "Unparseable data").setTextAttributes(HaxeSyntaxHighlighterColors.UNPARSEABLE_DATA);
      }
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
      final Annotation annotation = holder.createInfoAnnotation(range, null);
      final String attributeName = definitions.contains(word) ? HaxeSyntaxHighlighterColors.HAXE_DEFINED_VAR
                                                              : HaxeSyntaxHighlighterColors.HAXE_UNDEFINED_VAR;
      annotation.setTextAttributes(TextAttributesKey.find(attributeName));
      annotation.registerFix(new HaxeDefineIntention(word, definitions.contains(word)), range);
    }
  }

  private static boolean isNewOperator(PsiElement element) {
    return HaxeTokenTypes.ONEW.toString().equals(element.getText()) &&
           element.getParent() instanceof HaxeNewExpression;
  }

  /** Checks for keywords that are NOT PsiStatements; those are handled by IDEA.
   */
  private static boolean isKeyword(PsiElement element) {
    boolean isKeyword = false;
    PsiElement parent = element != null ? element.getParent() : null;

    if (element instanceof PsiJavaToken) {
      if (parent instanceof HaxeForStatement) {
        isKeyword = "in".equals(element.getText());
      } else if (parent instanceof HaxeImportStatement && ((HaxeImportStatement)parent).getAlias() != null) {
        String elementText = element.getText();
        isKeyword = "in".equals(elementText) || "as".equals(elementText);
      }
    }
    else if (element instanceof HaxeIdentifier) {
      if (parent instanceof HaxeAbstractClassDeclaration) {
        String elementText = element.getText();
        isKeyword = "from".equals(elementText) || "to".equals(elementText);
      }
    }
    return isKeyword;
  }

  private static boolean tryAnnotateQName(PsiElement node, AnnotationHolder holder) {
    // Maybe this is class name
    final HaxeClass resultClass = HaxeResolveUtil.tryResolveClassByQName(node);
    if (resultClass != null) {
      final TextAttributesKey attribute = getAttributeByType(HaxeComponentType.typeOf(resultClass), false);
      if (attribute != null) {
        holder.createInfoAnnotation(node, null).setTextAttributes(attribute);
      }
      return true;
    }

    return false;
  }

  private static boolean checkStatic(PsiElement parent) {
    return parent instanceof HaxeNamedComponent && ((HaxeNamedComponent)parent).isStatic();
  }

  @Nullable
  private static TextAttributesKey getAttributeByType(@Nullable HaxeComponentType type, boolean isStatic) {
    if (type == null) {
      return null;
    }
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
