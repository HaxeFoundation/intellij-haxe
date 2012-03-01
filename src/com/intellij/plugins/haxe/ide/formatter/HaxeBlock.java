package com.intellij.plugins.haxe.ide.formatter;

import com.intellij.formatting.*;
import com.intellij.formatting.templateLanguages.BlockWithParent;
import com.intellij.formatting.templateLanguages.DataLanguageBlockWrapper;
import com.intellij.formatting.templateLanguages.TemplateLanguageBlock;
import com.intellij.formatting.templateLanguages.TemplateLanguageBlockFactory;
import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
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
  }

  public Wrap getChildWrap() {
    return myChildWrap;
  }

  @Override
  protected IElementType getTemplateTextElementType() {
    return HaxeTokenTypes.LITSTRING;
  }

  @NotNull
  @Override
  public ChildAttributes getChildAttributes(int newChildIndex) {
    return new ChildAttributes(Indent.getNoneIndent(), null);
  }

  @Override
  @Nullable
  protected Alignment createChildAlignment(ASTNode child) {
    if (child.getElementType() != HaxeTokenTypes.PLPAREN && child.getElementType() != HaxeTokenTypes.HAXE_BLOCKSTATEMENT) {
      return myAlignmentProcessor.createChildAlignment();
    }
    return null;
  }

  @Override
  public Indent getIndent() {
    return muIndentProcessor.getChildIndent(myNode);
  }

  @Override
  public Spacing getSpacing(Block child1, Block child2) {
    return mySpacingProcessor.getSpacing(child1, child2);
  }

  @Override
  public Wrap createChildWrap(ASTNode child) {
    Wrap defaultWrap = super.createChildWrap(child);
    IElementType childType = child.getElementType();
    BlockWithParent parent = getParent();
    Wrap childWrap = parent instanceof HaxeBlock ? ((HaxeBlock)parent).getChildWrap() : null;
    Wrap wrap = myWrappingProcessor.createChildWrap(child, defaultWrap, childWrap);

    if (HaxeTokenTypeSets.ASSIGN_OPERATORS.contains(childType)) {
      myChildWrap = wrap;
    }
    return wrap;
  }
}
