/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2018 Ilya Malanin
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
package com.intellij.plugins.haxe.ide.folding;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilder;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.BODY_TYPES;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;
import static com.intellij.psi.TokenType.WHITE_SPACE;

public class HaxeFoldingBuilder implements FoldingBuilder {

  public static final String PLACEHOLDER_TEXT = "...";

  @NotNull
  @Override
  public FoldingDescriptor[] buildFoldRegions(@NotNull ASTNode node, @NotNull Document document) {
    List<FoldingDescriptor> list = new ArrayList<>();
    buildFolding(node, list);
    FoldingDescriptor[] descriptors = new FoldingDescriptor[list.size()];
    return list.toArray(descriptors);
  }

  private static void buildFolding(@NotNull ASTNode node, List<FoldingDescriptor> list) {
    final IElementType elementType = node.getElementType();

    FoldingDescriptor descriptor = null;
    if (isImportOrUsingStatement(elementType) && isFirstImportStatement(node)) {
      descriptor = buildImportsFolding(node);
    } else if (isCodeBlock(elementType)) {
      descriptor = buildCodeBlockFolding(node);
    } else if (isBodyBlock(elementType)) {
      descriptor = buildBodyBlockFolding(node);
    }

    if (descriptor != null) {
      list.add(descriptor);
    }

    for (ASTNode child : node.getChildren(null)) {
      buildFolding(child, list);
    }
  }

  private static boolean isCodeBlock(IElementType elementType) {
    return elementType == BLOCK_STATEMENT;
  }

  private static boolean isBodyBlock(IElementType elementType) {
    return BODY_TYPES.contains(elementType);
  }

  private static FoldingDescriptor buildCodeBlockFolding(@NotNull ASTNode node) {
    final ASTNode openBrace = node.getFirstChildNode();
    final ASTNode closeBrace = node.getLastChildNode();

    return buildBlockFolding(node, openBrace, closeBrace);
  }

  private static FoldingDescriptor buildBodyBlockFolding(@NotNull ASTNode node) {
    final ASTNode openBrace = UsefulPsiTreeUtil.getPrevSiblingSkipWhiteSpacesAndComments(node);
    final ASTNode closeBrace = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(node);

    return buildBlockFolding(node, openBrace, closeBrace);
  }

  private static FoldingDescriptor buildBlockFolding(@NotNull ASTNode node, ASTNode openBrace, ASTNode closeBrace) {
    TextRange textRange;
    if (openBrace != null && closeBrace != null && openBrace.getElementType() == PLCURLY && closeBrace.getElementType() == PRCURLY) {
      textRange = new TextRange(openBrace.getTextRange().getEndOffset(), closeBrace.getStartOffset());
    } else {
      textRange = node.getTextRange();
    }

    if (textRange.getLength() > PLACEHOLDER_TEXT.length()) {
      return new FoldingDescriptor(node, textRange);
    }

    return null;
  }

  private static FoldingDescriptor buildImportsFolding(@NotNull ASTNode node) {
    final ASTNode lastImportNode = findLastImportNode(node);
    if (!node.equals(lastImportNode)) {
      return new FoldingDescriptor(node, buildImportsFoldingTextRange(node, lastImportNode));
    }
    return null;
  }

  private static TextRange buildImportsFoldingTextRange(ASTNode firstNode, ASTNode lastNode) {
    ASTNode nodeStartFrom = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(firstNode.getFirstChildNode());
    if (nodeStartFrom == null) {
      nodeStartFrom = firstNode;
    }
    return new TextRange(nodeStartFrom.getStartOffset(), lastNode.getTextRange().getEndOffset());
  }

  private static boolean isFirstImportStatement(@NotNull ASTNode node) {
    ASTNode prevNode = node.getTreePrev();
    while (prevNode != null && prevNode.getElementType() == WHITE_SPACE) {
      prevNode = prevNode.getTreePrev();
    }
    return prevNode == null || !isImportOrUsingStatement(prevNode.getElementType());
  }

  private static ASTNode findLastImportNode(@NotNull ASTNode node) {
    ASTNode lastImportNode = node;
    ASTNode nextNode = lastImportNode.getTreeNext();
    while (nextNode != null && isApplicableForImportsRegion(nextNode.getElementType())) {
      lastImportNode = nextNode;
      nextNode = nextNode.getTreeNext();
    }
    if (lastImportNode.getElementType() == WHITE_SPACE) {
      lastImportNode = lastImportNode.getTreePrev();
    }
    return lastImportNode;
  }

  private static boolean isImportOrUsingStatement(IElementType type) {
    return type == IMPORT_STATEMENT || type == USING_STATEMENT;
  }

  private static boolean isApplicableForImportsRegion(IElementType type) {
    return isImportOrUsingStatement(type) || type == WHITE_SPACE;
  }

  @Nullable
  @Override
  public String getPlaceholderText(@NotNull ASTNode node) {
    return PLACEHOLDER_TEXT;
  }

  @Override
  public boolean isCollapsedByDefault(@NotNull ASTNode node) {
    return false;
  }
}
