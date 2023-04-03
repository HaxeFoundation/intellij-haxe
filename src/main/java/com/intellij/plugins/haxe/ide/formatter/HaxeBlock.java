/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
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
package com.intellij.plugins.haxe.ide.formatter;

import com.intellij.formatting.*;
import com.intellij.formatting.templateLanguages.BlockWithParent;
import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.ide.formatter.settings.HaxeCodeStyleSettings;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.formatter.FormatterUtil;
import com.intellij.psi.formatter.common.AbstractBlock;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeBlock extends AbstractBlock implements BlockWithParent {
  private final HaxeIndentProcessor myIndentProcessor;
  private final HaxeSpacingProcessor mySpacingProcessor;
  private final HaxeWrappingProcessor myWrappingProcessor;
  private final HaxeAlignmentProcessor myAlignmentProcessor;
  private Wrap myChildWrap = null;
  private final Indent myIndent;
  private final CodeStyleSettings mySettings;
  private boolean myChildrenBuilt = false;
  private BlockWithParent myParent;

  protected HaxeBlock(ASTNode node,
                      Wrap wrap,
                      Alignment alignment,
                      CodeStyleSettings settings) {
    super(node, wrap, alignment);
    mySettings = settings;
    final HaxeCodeStyleSettings haxeCodeStyleSettings = mySettings.getCustomSettings(HaxeCodeStyleSettings.class);
    myIndentProcessor = new HaxeIndentProcessor(mySettings.getCommonSettings(HaxeLanguage.INSTANCE));
    mySpacingProcessor = new HaxeSpacingProcessor(node, mySettings.getCommonSettings(HaxeLanguage.INSTANCE), haxeCodeStyleSettings);
    myWrappingProcessor = new HaxeWrappingProcessor(node, mySettings.getCommonSettings(HaxeLanguage.INSTANCE));
    myAlignmentProcessor = new HaxeAlignmentProcessor(node, mySettings.getCommonSettings(HaxeLanguage.INSTANCE));
    myIndent = myIndentProcessor.getChildIndent(myNode);
  }

  @Nullable
  // @Override // - Doesn't override until 2018.3.
  public String getDebugName() {
    return getClass().getSimpleName() + "(" + myNode.getElementType() + ")";
  }

  @Override
  public Indent getIndent() {
    return myIndent;
  }

  @Override
  public Spacing getSpacing(Block child1, @NotNull Block child2) {
    return mySpacingProcessor.getSpacing(child1, child2);
  }

  @Override
  protected List<Block> buildChildren() {
    myChildrenBuilt = true;
    if (isLeaf()) {
      return EMPTY;
    }
    final ArrayList<Block> tlChildren = new ArrayList<Block>();
    for (ASTNode childNode = getNode().getFirstChildNode(); childNode != null; childNode = childNode.getTreeNext()) {
      if (FormatterUtil.containsWhiteSpacesOnly(childNode)) continue;
      final HaxeBlock childBlock = new HaxeBlock(childNode, createChildWrap(childNode), createChildAlignment(childNode), mySettings);
      childBlock.setParent(this);
      tlChildren.add(childBlock);
    }
    return tlChildren;
  }

  public Wrap createChildWrap(ASTNode child) {
    final IElementType childType = child.getElementType();
    final Wrap wrap = myWrappingProcessor.createChildWrap(child, Wrap.createWrap(WrapType.NONE, false), myChildWrap);

    if (childType == HaxeTokenTypes.ASSIGN_OPERATION) {
      myChildWrap = wrap;
    }
    return wrap;
  }

  @Nullable
  protected Alignment createChildAlignment(ASTNode child) {
    if (child.getElementType() != HaxeTokenTypes.PLPAREN && child.getElementType() != HaxeTokenTypes.BLOCK_STATEMENT) {
      return myAlignmentProcessor.createChildAlignment();
    }
    return null;
  }

  @NotNull
  @Override
  public ChildAttributes getChildAttributes(final int newIndex) {
    int index = newIndex;
    ASTBlock prev = null;
    do {
      if (index == 0) {
        break;
      }
      prev = (ASTBlock)getSubBlocks().get(index - 1);
      index--;
    }
    while (prev.getNode().getElementType() == HaxeTokenTypes.OSEMI || prev.getNode() instanceof PsiWhiteSpace);

    final IElementType elementType = myNode.getElementType();
    final IElementType prevType = prev == null ? null : prev.getNode().getElementType();
    if (prevType == HaxeTokenTypes.PLPAREN) {
      return new ChildAttributes(Indent.getNormalIndent(), null);
    }
    if (prevType == HaxeTokenTypes.PLCURLY) {
      return new ChildAttributes(Indent.getNormalIndent(), null);
    }
    if (isTypeBody(prevType)) {
      return new ChildAttributes(Indent.getNormalIndent(), null);
    }
    if (isEndsWithRPAREN(elementType, prevType)) {
      return new ChildAttributes(Indent.getNormalIndent(), null);
    }
    if (index == 0) {
      return new ChildAttributes(Indent.getNoneIndent(), null);
    }
    if (prevType == HaxeTokenTypes.CONDITIONAL_STATEMENT_ID || prevType == HaxeTokenTypes.PPELSE || prevType == HaxeTokenTypes.PPEND || prevType == HaxeTokenTypes.PPELSEIF) {
      return new ChildAttributes(Indent.getNormalIndent(), null);
    }
    if (prevType.toString().equals("MSL_COMMENT") || prevType.toString().equals("MML_COMMENT") || prevType.toString().equals("DOC_COMMENT")) {
      return new ChildAttributes(Indent.getNormalIndent(), null);
    }
    return new ChildAttributes(prev.getIndent(), prev.getAlignment());
  }

  @Override
  public boolean isLeaf() {
    return false;
  }

  @Override
  public BlockWithParent getParent() {
    return myParent;
  }

  @Override
  public void setParent(BlockWithParent newParent) {
    myParent = newParent;
  }

  private static boolean isEndsWithRPAREN(IElementType elementType, IElementType prevType) {
    return prevType == HaxeTokenTypes.PRPAREN &&
           (elementType == HaxeTokenTypes.IF_STATEMENT ||
            elementType == HaxeTokenTypes.FOR_STATEMENT ||
            elementType == HaxeTokenTypes.WHILE_STATEMENT);
  }

  private static boolean isTypeBody(IElementType elementType) {
    return elementType == HaxeTokenTypes.CLASS_BODY ||
           elementType == HaxeTokenTypes.ABSTRACT_BODY ||
           elementType == HaxeTokenTypes.INTERFACE_BODY ||
           elementType == HaxeTokenTypes.ENUM_BODY ||
           elementType == HaxeTokenTypes.EXTERN_CLASS_DECLARATION_BODY ||
           elementType == HaxeTokenTypes.ANONYMOUS_TYPE_BODY;
  }
}
