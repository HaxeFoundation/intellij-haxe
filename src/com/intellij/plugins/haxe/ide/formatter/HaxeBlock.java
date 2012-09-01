package com.intellij.plugins.haxe.ide.formatter;

import com.intellij.formatting.*;
import com.intellij.formatting.templateLanguages.BlockWithParent;
import com.intellij.formatting.templateLanguages.DataLanguageBlockWrapper;
import com.intellij.formatting.templateLanguages.TemplateLanguageBlock;
import com.intellij.formatting.templateLanguages.TemplateLanguageBlockFactory;
import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeBlock extends TemplateLanguageBlock {
  private final HaxeIndentProcessor muIndentProcessor;
  private final HaxeSpacingProcessor mySpacingProcessor;
  private final HaxeWrappingProcessor myWrappingProcessor;
  private final HaxeAlignmentProcessor myAlignmentProcessor;
  private Wrap myChildWrap = null;
  private final Indent myIndent;

  protected HaxeBlock(ASTNode node,
                      Wrap wrap,
                      Alignment alignment,
                      CodeStyleSettings settings,
                      @NotNull TemplateLanguageBlockFactory blockFactory,
                      @Nullable List<DataLanguageBlockWrapper> foreignChildren) {
    super(node, wrap, alignment, blockFactory, settings, foreignChildren);
    muIndentProcessor = new HaxeIndentProcessor(settings);
    mySpacingProcessor = new HaxeSpacingProcessor(node, settings);
    myWrappingProcessor = new HaxeWrappingProcessor(node, settings);
    myAlignmentProcessor = new HaxeAlignmentProcessor(node, settings);
    myIndent = muIndentProcessor.getChildIndent(myNode);
  }

  public Wrap getChildWrap() {
    return myChildWrap;
  }

  @Override
  protected IElementType getTemplateTextElementType() {
    return HaxeTokenTypes.LITSTRING;
  }

  @Override
  @Nullable
  protected Alignment createChildAlignment(ASTNode child) {
    if (child.getElementType() != HaxeTokenTypes.PLPAREN && child.getElementType() != HaxeTokenTypes.HAXE_BLOCK_STATEMENT) {
      return myAlignmentProcessor.createChildAlignment();
    }
    return null;
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
  public Wrap createChildWrap(ASTNode child) {
    final Wrap defaultWrap = super.createChildWrap(child);
    final IElementType childType = child.getElementType();
    final BlockWithParent parent = getParent();
    final Wrap childWrap = parent instanceof HaxeBlock ? ((HaxeBlock)parent).getChildWrap() : null;
    final Wrap wrap = myWrappingProcessor.createChildWrap(child, defaultWrap, childWrap);

    if (HaxeTokenTypeSets.ASSIGN_OPERATORS.contains(childType)) {
      myChildWrap = wrap;
    }
    return wrap;
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
    if (prevType == HaxeTokenTypes.PLCURLY) {
      return new ChildAttributes(Indent.getNormalIndent(), null);
    }
    if (isEndsWithRPAREN(elementType, prevType)) {
      return new ChildAttributes(Indent.getNormalIndent(), null);
    }
    if (index == 0) {
      return new ChildAttributes(Indent.getNoneIndent(), null);
    }
    return new ChildAttributes(prev.getIndent(), prev.getAlignment());
  }

  private static boolean isEndsWithRPAREN(IElementType elementType, IElementType prevType) {
    return prevType == HaxeTokenTypes.PRPAREN &&
           (elementType == HaxeTokenTypes.HAXE_IF_STATEMENT ||
            elementType == HaxeTokenTypes.HAXE_FOR_STATEMENT ||
            elementType == HaxeTokenTypes.HAXE_WHILE_STATEMENT);
  }
}
