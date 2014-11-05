/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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
import com.intellij.lang.FileASTNode;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeComponentType;
import com.intellij.plugins.haxe.config.HaxeProjectSettings;
import com.intellij.plugins.haxe.config.NMETarget;
import com.intellij.plugins.haxe.ide.highlight.HaxeSyntaxHighlighterColors;
import com.intellij.plugins.haxe.ide.intention.HaxeDefineIntention;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import com.intellij.plugins.haxe.ide.module.HaxeModuleType;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.plugins.haxe.util.HaxeStringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.impl.source.tree.TreeUtil;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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
    String elementText = element.getText();
    if (element instanceof HaxeIdentifier && (elementText.equals("from") || elementText.equals("to")) ) {
      if (element.getParent() instanceof HaxeAbstractClassDeclaration) {
        TextAttributesKey attributesKey = TextAttributesKey.find(HaxeSyntaxHighlighterColors.HAXE_KEYWORD);
        holder.createInfoAnnotation(node, null).setTextAttributes(attributesKey);
      }
    }

    FileASTNode node1 = node.getContainingFile().getNode();
    LeafElement leaf = TreeUtil.findFirstLeaf(node1);

    int conditionalCount = 0;

    List<ASTNode> nodes = new ArrayList<ASTNode>();

    ASTNode node2 = leaf;
    do {
      if (node2.getElementType().equals(HaxeTokenTypes.CONDITIONAL_STATEMENT_ID)) {
        if (node2.getText().startsWith("#if")) {
          conditionalCount++;
        }
        nodes.add(node2);
      }
      else if (node2.getElementType().equals(HaxeTokenTypes.PPEND)) {
        conditionalCount--;
        nodes.add(node2);
      }
      else if (node2.getElementType().equals(HaxeTokenTypes.PPELSE)) {
        nodes.add(node2);
      }
    }
    while ((node2 = TreeUtil.nextLeaf(node2)) != null);

    if (conditionalCount != 0) {
      for (int i = 0, size = nodes.size(); i < size; i++) {
        ASTNode astNode = nodes.get(i);

        if (astNode.getElementType().equals(HaxeTokenTypes.CONDITIONAL_STATEMENT_ID)) {
          holder.createInfoAnnotation(astNode, null).setTextAttributes(HaxeSyntaxHighlighterColors.BAD_CHARACTER);
        }
        else if (astNode.getElementType().equals(HaxeTokenTypes.PPEND) || node2.getElementType().equals(HaxeTokenTypes.PPELSE)) {
          holder.createInfoAnnotation(astNode, null).setTextAttributes(HaxeSyntaxHighlighterColors.BAD_CHARACTER);
        }
      }
    }

    final ASTNode astNode = node.getNode();
    if (astNode != null) {
      IElementType tt = astNode.getElementType();

      if (tt == HaxeTokenTypeSets.PPEXPRESSION) {
        annotateCompilationExpression(node, holder);
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

  private static void tryAnnotateQName(PsiElement node, AnnotationHolder holder) {
    // Maybe this is class name
    final HaxeClass resultClass = HaxeResolveUtil.tryResolveClassByQName(node);
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
