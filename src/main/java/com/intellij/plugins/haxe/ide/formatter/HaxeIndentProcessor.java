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

import com.intellij.formatting.Indent;
import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.ide.formatter.settings.HaxeCodeStyleSettings;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.Nullable;

import static com.intellij.plugins.haxe.ide.formatter.HaxeFormatterNodes.*;
import static com.intellij.plugins.haxe.ide.formatter.HaxeFormatterTokenSets.*;
import static com.intellij.plugins.haxe.lang.lexer.HaxeDocTokenTypes.*;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.*;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeIndentProcessor {
  private static final TokenSet ADDITIVE_CHAIN_LEVELS = TokenSet.create(ADDITIVE_EXPRESSION);

  private final CommonCodeStyleSettings settings;
  private final HaxeCodeStyleSettings haxeSettings;

  public HaxeIndentProcessor(CommonCodeStyleSettings settings, HaxeCodeStyleSettings haxeSettings) {
    this.settings = settings;
    this.haxeSettings = haxeSettings;
  }

  /** The node's indent relative to its parent. The rules run in precedence order, and the first that answers decides. */
  public Indent getChildIndent(ASTNode node) {
    ASTNode parent = node.getTreeParent();
    if (parent == null || parent.getTreeParent() == null) return Indent.getNoneIndent();
    Site site = Site.of(node);

    Indent indent = inactiveBranchIndent(site);
    if (indent == null) indent = docCommentLineIndent(site);
    if (indent == null) indent = commentIndent(site);
    if (indent == null) indent = braceIndent(site);
    if (indent == null) indent = parenthesizedIndent(site);
    if (indent == null) indent = wrappedOperatorChainIndent(site);
    if (indent == null) indent = containerContentIndent(site);
    if (indent == null) indent = listItemIndent(site);
    if (indent == null) indent = statementBodyIndent(site);
    if (indent == null) indent = wrappedContinuationIndent(site);
    return indent != null ? indent : Indent.getNoneIndent();
  }

  /**
   * A direct child of an inactive branch or of the lists wrapping its content.
   * The branch node is a comment in the PSI and already sits at its scope's
   * indent. The layers below it add no indent, so the content aligns with the
   * directives, and deeper elements follow the normal rules.
   */
  @Nullable
  private static Indent inactiveBranchIndent(Site site) {
    return INACTIVE_BRANCH_LAYERS.contains(site.parentType()) ? Indent.getNoneIndent() : null;
  }

  /** A part of a doc comment: its opener, a leading star, its closer, or a body line. */
  @Nullable
  private static Indent docCommentLineIndent(Site site) {
    if (site.parentType() != DOC_COMMENT) return null;
    IElementType type = site.type();
    if (type == DOC_LEADING_ASTERISK) {
      // javadoc-style stars align under the first star of the opening /**
      return Indent.getSpaceIndent(1);
    }
    if (type == DOC_END) {
      // With leading stars the closer aligns under them too. Haxedoc style,
      // without stars, puts **/ back at the comment's own indent.
      boolean starredStyle = site.parent().findChildByType(DOC_LEADING_ASTERISK) != null;
      return starredStyle ? Indent.getSpaceIndent(1) : Indent.getNoneIndent();
    }
    if (type == DOC_START) return Indent.getNoneIndent();
    // Body lines sit one level inside the comment. Any deeper indent the author
    // wrote (markdown nesting, for example) is part of the token text and stays.
    return Indent.getNormalIndent();
  }

  /** A comment or directive: at its scope's level, except where it is kept at the first column. */
  @Nullable
  private Indent commentIndent(Site site) {
    IElementType type = site.type();
    if (!COMMENTS.contains(type)) return null;
    // Keeping comments at the first column protects code disabled with //.
    // A doc comment belongs to its member and always follows its scope, as javadoc does.
    if (type != DOC_COMMENT && settings.KEEP_FIRST_COLUMN_COMMENT && isAtFirstColumn(site.node())) {
      return Indent.getAbsoluteNoneIndent();
    }
    // module-level comments sit at the file margin like their sibling declarations
    if (site.parentType() == MODULE) return Indent.getNoneIndent();
    if (site.parentType() == SWITCH_BLOCK) return switchBlockCommentIndent(site);
    return Indent.getNormalIndent();
  }

  /**
   * A comment that is a direct child of the switch block. After a case
   * without statements it reads as that case's body (a lone "// TODO") and
   * sits two steps in, which is the continuation indent at the standard
   * ratio of two. After a case with a body, or at the start of the block,
   * it reads as a heading for the next case and sits at case level.
   */
  private static Indent switchBlockCommentIndent(Site site) {
    IElementType prevSiblingType = site.prevSiblingType();
    boolean emptyCaseBody = (prevSiblingType == SWITCH_CASE || prevSiblingType == DEFAULT_CASE)
                            && caseBodyIsEmpty(site.prevSibling());
    // A directive after the last statement of a case body also lands here,
    // because the parser closes the body before it. One that closes a region
    // opened inside that body still aligns with the body, like its #if.
    boolean bodyLevel = emptyCaseBody || closesRegionOpenedInCaseBody(site.node());
    return bodyLevel ? Indent.getContinuationIndent() : Indent.getNormalIndent();
  }

  /** A '{' or '}': one step in under the shifted brace styles, otherwise at its owner's level. */
  @Nullable
  private Indent braceIndent(Site site) {
    if (site.type() != PLCURLY && site.type() != PRCURLY) return null;
    int braceStyle = FUNCTION_LIKE_OWNERS.contains(site.grandparentType()) ? settings.METHOD_BRACE_STYLE : settings.BRACE_STYLE;
    return switch (braceStyle) {
      case CommonCodeStyleSettings.NEXT_LINE_SHIFTED, CommonCodeStyleSettings.NEXT_LINE_SHIFTED2 -> Indent.getNormalIndent();
      default -> Indent.getNoneIndent();
    };
  }

  /** Inside parentheses: the parens stay at their owner's level, the content steps in. */
  @Nullable
  private static Indent parenthesizedIndent(Site site) {
    if (site.parentType() != PARENTHESIZED_EXPRESSION) return null;
    boolean paren = site.type() == PLPAREN || site.type() == PRPAREN;
    return paren ? Indent.getNoneIndent() : Indent.getNormalIndent();
  }

  /** A wrapped line of a boolean ({@code &&}, {@code ||}) or additive (+, -) chain, when INDENT_WRAPPED_OPERATOR_CHAINS is on. */
  @Nullable
  private Indent wrappedOperatorChainIndent(Site site) {
    if (!haxeSettings.INDENT_WRAPPED_OPERATOR_CHAINS) return null;
    IElementType parentType = site.parentType();
    // A boolean chain's wrapped lines sit one step in from the line the chain
    // starts on. The steps of nested levels do not add up: the left-nested
    // levels of one chain all start on the chain's first line, so every
    // operator line lands exactly one step in. A parenthesized inner chain
    // starts on the line of its '(' and steps once from there.
    if (parentType == LOGIC_AND_EXPRESSION || parentType == LOGIC_OR_EXPRESSION) return Indent.getNormalIndent();
    // An additive chain's wrapped lines stay at the surrounding wrap step,
    // since haxe-formatter never adds a step for them. A chain that starts its
    // line is itself the indent base, so it takes no step. A chain that starts
    // mid-line has the call or statement line as its base and needs one step.
    if (parentType == ADDITIVE_EXPRESSION) {
      return additiveChainBeginsItsLine(site.parent()) ? Indent.getNoneIndent() : Indent.getNormalIndent();
    }
    return null;
  }

  /** Content of a block, type body, literal or switch: one step in, except directly under the file. */
  @Nullable
  private static Indent containerContentIndent(Site site) {
    if (!indentsChildren(site.parentType(), site.type())) return null;
    return site.node().getPsi().getParent() instanceof PsiFile ? Indent.getNoneIndent() : Indent.getNormalIndent();
  }

  /** Whether the parent indents this child: an indented container indents all children, a bracket literal all but its brackets. */
  private static boolean indentsChildren(IElementType parentType, IElementType childType) {
    if (INDENTED_CONTAINERS.contains(parentType)) return true;
    return BRACKET_LITERALS.contains(parentType) && childType != PLBRACK && childType != PRBRACK;
  }

  /** A wrapped item of a parameter or argument list, a multi-var declarator or a {@code new} argument. */
  @Nullable
  private Indent listItemIndent(Site site) {
    IElementType type = site.type();
    IElementType parentType = site.parentType();
    // A parameter or argument list has no indent of its own; only its items
    // do (below). A chopped-down list, which breaks right after the paren,
    // and a wrap in the middle of the list then land at the same depth.
    boolean listOwner = FUNCTION_LIKE_OWNERS.contains(parentType) || parentType == CALL_EXPRESSION;
    if (listOwner && PARAMETER_AND_ARGUMENT_LISTS.contains(type)) return Indent.getNoneIndent();
    // An array literal's list is indented like a block (containerContentIndent),
    // and its items sit at the list's level.
    if (PARAMETER_AND_ARGUMENT_LISTS.contains(parentType) && site.grandparentType() != ARRAY_LITERAL) return argumentListItemIndent(site);
    // A multi-var declarator wrapped onto its own line sits one step in from
    // the "var" line, which the first declarator shares.
    if (type == LOCAL_VAR_DECLARATION && parentType == LOCAL_VAR_DECLARATION_LIST) return Indent.getNormalIndent();
    // `new T(a, b)` has its arguments as direct children, with no list node.
    // An argument follows the paren or a comma.
    boolean afterListOpener = site.prevSiblingType() == PLPAREN || site.prevSiblingType() == OCOMMA;
    if (parentType == NEW_EXPRESSION && afterListOpener && type != PRPAREN) return Indent.getNormalIndent();
    return null;
  }

  /**
   * A wrapped list item, indented from the line that opened the list. Call
   * arguments and enum constructor parameters take one step. A function
   * signature's parameters take two, so they stand apart from the body,
   * which sits one step in. A function without a body, or with an empty {},
   * leaves its parameters at one step.
   */
  private static Indent argumentListItemIndent(Site site) {
    if (LIST_PUNCTUATION.contains(site.type())) return Indent.getNoneIndent();
    boolean signature = site.parentType() == PARAMETER_LIST && FUNCTION_LIKE_OWNERS.contains(site.grandparentType());
    return signature && hasStatementBody(site.grandparent()) ? Indent.getContinuationIndent() : Indent.getNormalIndent();
  }

  /** A statement's non-block body on its own line, and a value block under next-line braces. */
  @Nullable
  private static Indent statementBodyIndent(Site site) {
    IElementType type = site.type();
    IElementType parentType = site.parentType();
    IElementType prevSiblingType = site.prevSiblingType();
    // A function's non-block body on its own line sits one step in. The
    // header's own trailing parts can also follow a header end; they stay unindented.
    boolean functionBody = FUNCTION_LIKE_OWNERS.contains(parentType)
                           && FUNCTION_HEADER_END.contains(prevSiblingType)
                           && !FUNCTION_HEADER_TRAILERS.contains(type);
    if (functionBody) return Indent.getNormalIndent();
    if (parentType == FOR_STATEMENT && prevSiblingType == PRPAREN && type != BLOCK_STATEMENT) return Indent.getNormalIndent();
    boolean tryBody = parentType == TRY_STATEMENT && prevSiblingType == KTRY && type != BLOCK_STATEMENT && type != CATCH_STATEMENT;
    if (tryBody) return Indent.getNormalIndent();
    if (parentType == CATCH_STATEMENT && prevSiblingType == PRPAREN && type != BLOCK_STATEMENT) return Indent.getNormalIndent();
    boolean loopBody = type == DO_WHILE_BODY && site.firstChildType() != BLOCK_STATEMENT;
    if (parentType == WHILE_STATEMENT && prevSiblingType == PRPAREN && loopBody) return Indent.getNormalIndent();
    if (parentType == DO_WHILE_STATEMENT && prevSiblingType == KDO && loopBody) return Indent.getNormalIndent();
    if (parentType == RETURN_STATEMENT && prevSiblingType == KRETURN && type != BLOCK_STATEMENT) return Indent.getNormalIndent();
    // A block used as a value sits one step in from its declaration when the
    // brace style puts its { on the next line; haxe-formatter indents such a
    // brace one level, and the contents step from it. When the { stays on the
    // = line the step has no effect, because a block's indent only counts
    // where the block starts a line.
    if (type == VALUE_INIT_BLOCK && (parentType == VAR_INIT || parentType == ASSIGN_EXPRESSION)) return Indent.getNormalIndent();
    // the non-block body of an if or else
    boolean guardedBody = (parentType == GUARDED_STATEMENT || parentType == ELSE_STATEMENT)
                          && type != BLOCK_STATEMENT && type != KELSE && type != IF_STATEMENT;
    if (guardedBody) return Indent.getNormalIndent();
    return null;
  }

  /** A wrapped continuation of a declaration or expression: a type hint, a chain link, an inherit clause or a ternary part. */
  @Nullable
  private static Indent wrappedContinuationIndent(Site site) {
    IElementType type = site.type();
    IElementType parentType = site.parentType();
    // An anonymous type that opens on the line after its type hint's colon
    // (next-line braces) sits one step in. On the hint's line the step has no effect.
    if (parentType == TYPE_TAG && type == TYPE_OR_ANONYMOUS) return Indent.getNormalIndent();
    // A wrapped chain link (.map(...) on its own line) sits one step in from
    // the chain's first line. The continuation indent would be the double
    // step used for declarations.
    if (type != CALL_EXPRESSION && isChainLink(site.parent())) return Indent.getNormalIndent();
    // a wrapped extends/implements clause continues the declaration header
    if (parentType == INHERIT_LIST) return Indent.getContinuationIndent();
    // wrapped ternary parts (the branches, or the signs before them) continue the condition's line
    if (parentType == TERNARY_EXPRESSION && site.prevSibling() != null) return Indent.getContinuationIndent();
    return null;
  }

  /** Whether the whole chain, from its outermost additive level, starts its line with only whitespace before it. */
  private static boolean additiveChainBeginsItsLine(ASTNode additive) {
    return beginsItsLine(outermostOfKind(additive, ADDITIVE_CHAIN_LEVELS));
  }

  /** Whether the node is a #else, #elseif or #end at switch-block level whose #if sits inside a case body. */
  private static boolean closesRegionOpenedInCaseBody(ASTNode directive) {
    IElementType type = directive.getElementType();
    if (type != PPEND && type != PPELSE && type != PPELSEIF) return false;
    ASTNode opener = regionOpener(directive).directive();
    return opener != null && opener.getTreeParent() != directive.getTreeParent();
  }

  /** Whether the case has no statements in its body; a comment after it then reads as its body. */
  private static boolean caseBodyIsEmpty(ASTNode switchCase) {
    ASTNode block = switchCase.findChildByType(SWITCH_CASE_BLOCK);
    if (block == null) return true;
    for (ASTNode child = block.getFirstChildNode(); child != null; child = child.getTreeNext()) {
      IElementType type = child.getElementType();
      if (!WHITESPACES.contains(type) && !COMMENTS.contains(type)) return false;
    }
    return true;
  }

  /** Whether the node starts at column 0 of its line. */
  private static boolean isAtFirstColumn(ASTNode node) {
    CharSequence text = fileText(node);
    return text != null && HaxeIndentText.lineStartOffset(text, node.getStartOffset()) == node.getStartOffset();
  }

  /** Whether only whitespace precedes the node on its line. */
  private static boolean beginsItsLine(ASTNode node) {
    CharSequence text = fileText(node);
    if (text == null) return false;
    int offset = node.getStartOffset();
    int indentEnd = HaxeIndentText.lineStartOffset(text, offset) + HaxeIndentText.lineIndentAt(text, offset).length();
    return indentEnd == offset;
  }

  /** The text of the node's file, null for a node outside any file. */
  @Nullable
  private static CharSequence fileText(ASTNode node) {
    PsiFile file = node.getPsi().getContainingFile();
    return file == null ? null : file.getViewProvider().getContents();
  }

  /** The node being indented and the neighbours the rules look at. Only built for a node that has a grandparent. */
  private record Site(ASTNode node, @Nullable ASTNode prevSibling, ASTNode parent, ASTNode grandparent) {

    static Site of(ASTNode node) {
      ASTNode prevSibling = UsefulPsiTreeUtil.getPrevSiblingSkipWhiteSpacesAndComments(node);
      ASTNode parent = node.getTreeParent();
      return new Site(node, prevSibling, parent, parent.getTreeParent());
    }

    IElementType type() {
      return node.getElementType();
    }

    @Nullable
    IElementType prevSiblingType() {
      return prevSibling == null ? null : prevSibling.getElementType();
    }

    IElementType parentType() {
      return parent.getElementType();
    }

    IElementType grandparentType() {
      return grandparent.getElementType();
    }

    @Nullable
    IElementType firstChildType() {
      ASTNode firstChild = node.getFirstChildNode();
      return firstChild == null ? null : firstChild.getElementType();
    }
  }
}
