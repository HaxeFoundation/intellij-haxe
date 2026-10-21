/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2020 Eric Bishton
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
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.ide.formatter.HaxeFormatterNodes.MetadataRun;
import com.intellij.plugins.haxe.ide.formatter.settings.HaxeCodeStyleSettings;
import com.intellij.plugins.haxe.ide.formatter.wrapping.HaxeCallArgumentFill;
import com.intellij.plugins.haxe.ide.formatter.wrapping.HaxeOperatorChainRules;
import com.intellij.plugins.haxe.ide.formatter.wrapping.HaxeOperatorChainRules.Kind;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeInactiveBody;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.formatter.FormatterUtil;
import com.intellij.psi.formatter.common.AbstractBlock;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import static com.intellij.plugins.haxe.ide.formatter.HaxeFormatterNodes.sameLineMetadataRun;
import static com.intellij.plugins.haxe.ide.formatter.HaxeFormatterTokenSets.PARAMETER_AND_ARGUMENT_LISTS;
import static com.intellij.plugins.haxe.ide.formatter.HaxeFormatterTokenSets.INDENTED_CONTAINERS;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.*;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeBlock extends AbstractBlock implements BlockWithParent {
  // A new line after one of these sits one step in: an opening bracket, a
  // type body, a directive or a comment.
  private static final TokenSet INDENT_OPENERS = TokenSet.orSet(
    TokenSet.create(PLPAREN, PLCURLY, CONDITIONAL_STATEMENT_ID, PPELSE, PPEND, PPELSEIF), CLASS_BODY_TYPES, ONLY_COMMENTS);

  private final HaxeIndentProcessor myIndentProcessor;
  private final HaxeSpacingProcessor mySpacingProcessor;
  private final HaxeWrappingProcessor myWrappingProcessor;
  private final HaxeAlignmentProcessor myAlignmentProcessor;
  // the wrap of this assignment's sign once it is built; the call arguments after it become its child wraps
  private Wrap myAssignmentSignWrap = null;
  private final Indent myIndent;
  private final CodeStyleSettings mySettings;
  private final CommonCodeStyleSettings myCommon;
  private final HaxeCodeStyleSettings myHaxe;
  // the alignment a call's continuation lines share (see continuationAlignmentFor), created on first use
  private Alignment myContinuationAlignment;
  // The metadata written before the node on its line. When there is any, this
  // block is a "metadata span" that covers the metadata too. The formatting
  // engine indents a block's wrapped lines from the block that starts the
  // line. In the PSI, metadata is a sibling of its declaration, so as a block
  // of its own it would start the line, and the declaration's wrapped parts
  // would indent from the enclosing body instead.
  private final List<ASTNode> myLeadingMetadata;
  private BlockWithParent myParent;

  protected HaxeBlock(ASTNode node,
                      Wrap wrap,
                      Alignment alignment,
                      CodeStyleSettings settings) {
    this(node, List.of(), wrap, alignment, settings, null);
  }

  /** A non-null {@code indent} replaces the one the indent processor would compute. */
  private HaxeBlock(ASTNode node,
                    List<ASTNode> leadingMetadata,
                    Wrap wrap,
                    Alignment alignment,
                    CodeStyleSettings settings,
                    @Nullable Indent indent) {
    super(node, wrap, alignment);
    mySettings = settings;
    myLeadingMetadata = leadingMetadata;
    myCommon = settings.getCommonSettings(HaxeLanguage.INSTANCE);
    myHaxe = settings.getCustomSettings(HaxeCodeStyleSettings.class);
    myIndentProcessor = new HaxeIndentProcessor(myCommon, myHaxe);
    mySpacingProcessor = new HaxeSpacingProcessor(node, myCommon, myHaxe);
    myWrappingProcessor = new HaxeWrappingProcessor(node, myCommon, myHaxe);
    myAlignmentProcessor = new HaxeAlignmentProcessor(node, myCommon);
    myIndent = indent != null ? indent : myIndentProcessor.getChildIndent(myNode);
  }

  @Nullable
  @Override
  public String getDebugName() {
    return getClass().getSimpleName() + "(" + myNode.getElementType() + ")";
  }

  /**
   * The node's range, with two exceptions. A metadata span starts at its
   * first metadata. An inactive branch (PPBODY) spans the whole region
   * between two directives, including the whitespace at both edges. That
   * whitespace would lie inside the block, where spacing rules cannot reach
   * it, so blank lines around the branch would never change. The range is
   * therefore trimmed to the branch's content.
   */
  @Override
  public @NotNull TextRange getTextRange() {
    if (!myLeadingMetadata.isEmpty()) {
      return new TextRange(myLeadingMetadata.getFirst().getStartOffset(), myNode.getTextRange().getEndOffset());
    }
    if (myNode.getElementType() != PPBODY) return super.getTextRange();
    // trim by the same child walk that builds the sub-blocks, so every
    // child block stays inside the reported range
    ASTNode first = myNode.getFirstChildNode();
    while (first != null && FormatterUtil.containsWhiteSpacesOnly(first)) first = first.getTreeNext();
    ASTNode last = myNode.getLastChildNode();
    while (last != null && FormatterUtil.containsWhiteSpacesOnly(last)) last = last.getTreePrev();
    if (first == null || last == null) return super.getTextRange();  // a whitespace-only branch stays as written
    return new TextRange(first.getStartOffset(), last.getTextRange().getEndOffset());
  }

  @Override
  public Indent getIndent() {
    return myIndent;
  }

  @Override
  public Spacing getSpacing(Block child1, @NotNull Block child2) {
    // The pairs inside a metadata span (metadata to metadata, metadata to
    // declaration) belong to the enclosing body and follow its rules.
    HaxeBlock owner = myLeadingMetadata.isEmpty() ? this : (HaxeBlock)myParent;
    return owner.mySpacingProcessor.getSpacing(child1, child2);
  }

  @Override
  protected List<Block> buildChildren() {
    if (!myLeadingMetadata.isEmpty()) return buildMetadataSpanChildren();
    IElementType type = getNode().getElementType();
    if (type == DOC_COMMENT) return buildDocCommentChildren();
    if (type == PPBODY) return buildInactiveBranchChildren();
    return childBlocks((child, metadata) -> childBlock(child, metadata, createChildWrap(child), createChildAlignment(child), true));
  }

  /** A block for each metadata, then one for the node, all without indent: the span starts the line and carries the indent. */
  private List<Block> buildMetadataSpanChildren() {
    List<Block> children = new ArrayList<>();
    for (ASTNode metadata : myLeadingMetadata) children.add(spanMemberBlock(metadata));
    children.add(spanMemberBlock(myNode));
    return children;
  }

  /**
   * Blocks over the lazily parsed doc comment tree. Only the whitespace at
   * the start of each line is formatted, per the doc comment indent rules;
   * wraps and alignments never apply inside a comment. With
   * FORMAT_DOC_COMMENTS off the comment stays a single block.
   */
  private List<Block> buildDocCommentChildren() {
    if (!mySettings.getCustomSettings(HaxeCodeStyleSettings.class).FORMAT_DOC_COMMENTS) {
      return EMPTY;
    }
    return childBlocks((child, metadata) -> childBlock(child, metadata, Wrap.createWrap(WrapType.NONE, false), null, false));
  }

  /**
   * Blocks over the lazily parsed tree of an inactive conditional branch. The
   * children are ordinary Haxe PSI, so the normal indent and spacing rules
   * apply inside. A branch without a clean parse stays a single block and is
   * preserved verbatim, as haxe-formatter does in that case.
   */
  private List<Block> buildInactiveBranchChildren() {
    HaxeCodeStyleSettings haxe = mySettings.getCustomSettings(HaxeCodeStyleSettings.class);
    if (!(getNode().getPsi() instanceof HaxeInactiveBody body) || HaxeInactiveBranches.isPreservedVerbatim(body, haxe)) {
      return EMPTY;
    }
    return childBlocks((child, metadata) -> childBlock(child, metadata, createChildWrap(child), null, true));
  }

  /**
   * One block per non-whitespace child, in order. A child preceded by
   * metadata on the same line takes that metadata into its block; the
   * factory receives it as the second argument, which is empty for every
   * other child.
   */
  private List<Block> childBlocks(BiFunction<ASTNode, List<ASTNode>, HaxeBlock> blockOf) {
    List<Block> children = new ArrayList<>();
    for (ASTNode child = getNode().getFirstChildNode(); child != null; child = child.getTreeNext()) {
      if (FormatterUtil.containsWhiteSpacesOnly(child)) continue;
      MetadataRun run = sameLineMetadataRun(child);
      if (run == null) {
        children.add(blockOf.apply(child, List.of()));
        continue;
      }
      children.add(blockOf.apply(run.decorated(), run.metadata()));
      child = run.decorated();
    }
    return children;
  }

  private HaxeBlock childBlock(ASTNode child, List<ASTNode> leadingMetadata, Wrap wrap, @Nullable Alignment alignment, boolean linkWrapping) {
    HaxeBlock block = new HaxeBlock(child, leadingMetadata, wrap, alignment, mySettings, null);
    return linked(block, linkWrapping);
  }

  /** A child of a metadata span: no indent, wrap or alignment of its own, since the span carries the node's. */
  private HaxeBlock spanMemberBlock(ASTNode node) {
    HaxeBlock block = new HaxeBlock(node, List.of(), Wrap.createWrap(WrapType.NONE, false), null, mySettings, Indent.getNoneIndent());
    return linked(block, true);
  }

  private HaxeBlock linked(HaxeBlock block, boolean linkWrapping) {
    block.setParent(this);
    // A shared wrap can span tree levels: a literal's items chop together with
    // its closing bracket, and a call chain's dots chop together. The child's
    // processor reaches the enclosing processors through this link.
    if (linkWrapping) block.myWrappingProcessor.setParentProcessor(myWrappingProcessor);
    return block;
  }

  private Wrap createChildWrap(ASTNode child) {
    Wrap wrap = myWrappingProcessor.createChildWrap(child, myAssignmentSignWrap);
    if (child.getElementType() == ASSIGN_OPERATION) {
      myAssignmentSignWrap = wrap;
    }
    return wrap;
  }

  @Nullable
  protected Alignment createChildAlignment(ASTNode child) {
    Alignment continuation = continuationAlignmentFor(child);
    if (continuation != null) return continuation;
    if (child.getElementType() != PLPAREN && child.getElementType() != BLOCK_STATEMENT) {
      return myAlignmentProcessor.createChildAlignment();
    }
    return null;
  }

  /**
   * The alignment for a continuation line of a call, or null.
   * <p>
   * haxe-formatter indents every continuation line of a call one step from
   * the line the call starts on. This holds for an argument that the fill
   * moves to a new line and for the lines of an operator chain among the
   * arguments, however deeply the chain nests. The formatting engine indents
   * a wrapped line from the first block on its parent's first line, which
   * would add the chain's step on top of the moved argument's step. These
   * lines therefore share one alignment on the argument list: the first of
   * them takes the step, and the rest align to its column.
   */
  @Nullable
  private Alignment continuationAlignmentFor(ASTNode child) {
    if (!myHaxe.INDENT_WRAPPED_OPERATOR_CHAINS) return null;
    if (isMovedArgument(child)) return continuationAlignment();
    Kind kind = Kind.ofChainLevel(myNode);
    boolean breakingOperator = kind != null && kind.isOperator(child) && HaxeOperatorChainRules.breaksBefore(kind, child, myCommon, myHaxe);
    if (!breakingOperator) return null;
    HaxeBlock list = enclosingArgumentList();
    return list == null ? null : list.continuationAlignment();
  }

  /** Whether the child is an argument of this call's list that the fill moves to a new line. */
  private boolean isMovedArgument(ASTNode child) {
    if (!PARAMETER_AND_ARGUMENT_LISTS.contains(myNode.getElementType()) || myNode.getTreeParent() == null) return false;
    IElementType ownerType = myNode.getTreeParent().getElementType();
    boolean call = ownerType == CALL_EXPRESSION || ownerType == NEW_EXPRESSION;
    return call && HaxeCallArgumentFill.movedArguments(myNode, myCommon, myHaxe).contains(child);
  }

  private Alignment continuationAlignment() {
    if (myContinuationAlignment == null) myContinuationAlignment = Alignment.createAlignment();
    return myContinuationAlignment;
  }

  /** The nearest enclosing call argument list block, looking no further than the nearest body or literal; null when there is none. */
  @Nullable
  private HaxeBlock enclosingArgumentList() {
    for (BlockWithParent block = myParent; block instanceof HaxeBlock haxeBlock; block = haxeBlock.myParent) {
      IElementType type = haxeBlock.myNode.getElementType();
      if (INDENTED_CONTAINERS.contains(type)) return null;
      ASTNode owner = haxeBlock.myNode.getTreeParent();
      boolean callList = PARAMETER_AND_ARGUMENT_LISTS.contains(type) && owner != null
                         && (owner.getElementType() == CALL_EXPRESSION || owner.getElementType() == NEW_EXPRESSION);
      if (callList) return haxeBlock;
    }
    return null;
  }

  @NotNull
  @Override
  public ChildAttributes getChildAttributes(final int newIndex) {
    int index = newIndex;
    ASTBlock prev = null;
    while (index > 0) {
      index--;
      prev = (ASTBlock)getSubBlocks().get(index);
      IElementType type = prev.getNode().getElementType();
      if (type != OSEMI && !WHITESPACES.contains(type)) break;
    }

    IElementType elementType = myNode.getElementType();
    IElementType prevType = prev == null ? null : prev.getNode().getElementType();
    if (opensIndentedRegion(elementType, prevType)) {
      return new ChildAttributes(Indent.getNormalIndent(), null);
    }
    if (index == 0) {
      return new ChildAttributes(Indent.getNoneIndent(), null);
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

  /** Whether a new line after the previous child sits one step in: after an indent opener, or after the ')' of an if, for or while head. */
  private static boolean opensIndentedRegion(IElementType elementType, @Nullable IElementType prevType) {
    return INDENT_OPENERS.contains(prevType) || followsStatementHeadParen(elementType, prevType);
  }

  private static boolean followsStatementHeadParen(IElementType elementType, @Nullable IElementType prevType) {
    return prevType == PRPAREN &&
           (elementType == IF_STATEMENT ||
            elementType == FOR_STATEMENT ||
            elementType == WHILE_STATEMENT);
  }
}
