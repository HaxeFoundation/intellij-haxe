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
package com.intellij.plugins.haxe.ide.formatter;

import com.intellij.formatting.Wrap;
import com.intellij.formatting.WrapType;
import com.intellij.lang.ASTNode;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.formatter.FormatterUtil;
import com.intellij.psi.formatter.WrappingUtil;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.Nullable;

import com.intellij.plugins.haxe.ide.formatter.settings.HaxeCodeStyleSettings;
import com.intellij.plugins.haxe.ide.formatter.wrapping.HaxeLiteralItemRules;
import com.intellij.plugins.haxe.ide.formatter.wrapping.HaxeLiteralItemRules.Decision;

import static com.intellij.plugins.haxe.ide.formatter.HaxeFormatterNodes.isChainLink;
import static com.intellij.plugins.haxe.ide.formatter.HaxeFormatterTokenSets.BRACKET_LITERALS;
import static com.intellij.plugins.haxe.ide.formatter.HaxeFormatterTokenSets.FUNCTION_LIKE_OWNERS;
import static com.intellij.plugins.haxe.ide.formatter.HaxeFormatterTokenSets.PARAMETER_AND_ARGUMENT_LISTS;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.*;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;


/**
 * @author: Fedor.Korotkov
 */
public class HaxeWrappingProcessor {
  private final ASTNode myNode;
  private final CommonCodeStyleSettings mySettings;
  private final HaxeCodeStyleSettings myHaxe;
  // Chop-down wrapping breaks all elements together only when they share one
  // Wrap object. The construct's own processor owns these shared wraps, and
  // nested levels reach them through the parent link.
  private Wrap sharedItemWrap;
  private Wrap sharedChainWrap;
  private HaxeWrappingProcessor parentProcessor;

  public HaxeWrappingProcessor(ASTNode node, CommonCodeStyleSettings settings, HaxeCodeStyleSettings haxe) {
    myNode = node;
    mySettings = settings;
    myHaxe = haxe;
  }

  void setParentProcessor(@Nullable HaxeWrappingProcessor parent) {
    parentProcessor = parent;
  }

  /**
   * The wrap for a child of this processor's node. The rules are tried in
   * precedence order. {@code assignmentSignWrap} is the wrap of the enclosing
   * assignment's sign; call arguments under the assignment become its child wraps.
   */
  Wrap createChildWrap(ASTNode child, @Nullable Wrap assignmentSignWrap) {
    IElementType childType = child.getElementType();
    if (childType == OCOMMA || childType == OSEMI) return noWrap();

    Wrap wrap = literalWrap(child);
    if (wrap == null) wrap = chainLinkWrap(child);
    if (wrap == null) wrap = inheritClauseWrap(child);
    if (wrap == null) wrap = argumentListWrap(child, assignmentSignWrap);
    if (wrap == null) wrap = newArgumentWrap(child);
    if (wrap == null) wrap = elseKeywordWrap(child);
    if (wrap == null) wrap = binaryOperandWrap(child);
    if (wrap == null) wrap = assignmentWrap(child);
    if (wrap == null) wrap = ternaryWrap(child);
    return wrap != null ? wrap : noWrap();
  }

  /**
   * Array, map and object literals. The items and the closing bracket share
   * one chop-down wrap, owned by the literal's processor, so a literal that
   * passes the right margin breaks into one item per line with the bracket
   * on its own line. The item list itself never wraps; the breaks come from
   * its items.
   */
  @Nullable
  private Wrap literalWrap(ASTNode child) {
    if (mySettings.ARRAY_INITIALIZER_WRAP == CommonCodeStyleSettings.DO_NOT_WRAP) return null;
    IElementType elementType = myNode.getElementType();
    IElementType childType = child.getElementType();
    ASTNode listParent = myNode.getTreeParent();
    IElementType listParentType = listParent == null ? null : listParent.getElementType();
    boolean literalItems = (elementType == EXPRESSION_LIST && listParentType == ARRAY_LITERAL)
                           || elementType == MAP_INITIALIZER_EXPRESSION_LIST;
    if (literalItems) {
      HaxeWrappingProcessor literalProcessor = parentProcessor != null ? parentProcessor : this;
      return literalProcessor.sharedItemWrap(literalItemWrapSetting(listParent));
    }
    boolean bracketLiteral = BRACKET_LITERALS.contains(elementType);
    boolean objectItem = elementType == OBJECT_LITERAL && childType == OBJECT_LITERAL_ELEMENT;
    boolean literalCloser = (bracketLiteral && childType == PRBRACK) || (elementType == OBJECT_LITERAL && childType == PRCURLY);
    if (objectItem || literalCloser) return sharedItemWrap(literalItemWrapSetting(myNode));
    boolean itemList = childType == EXPRESSION_LIST || childType == MAP_INITIALIZER_EXPRESSION_LIST;
    if (bracketLiteral && itemList) return Wrap.createWrap(WrapType.NONE, true);
    return null;
  }

  /**
   * The wrap setting a literal's items and closing bracket share. When the
   * item rules fill the literal after a leading break, the items wrap as
   * needed, so the fill breaks where the margin demands. Any other literal
   * follows the array wrap setting.
   */
  private int literalItemWrapSetting(@Nullable ASTNode literal) {
    boolean fills = literal != null && HaxeLiteralItemRules.decide(literal, mySettings, myHaxe) == Decision.FILL_AFTER_LEADING_BREAK;
    return fills ? CommonCodeStyleSettings.WRAP_AS_NEEDED : mySettings.ARRAY_INITIALIZER_WRAP;
  }

  /**
   * Method chains. Every link whose receiver is a call can break before its
   * dot. All links share the chain's one wrap, so chopping breaks the whole
   * chain. The first link's receiver is a plain reference, so the first link
   * stays on the receiver's line (haxe-formatter's OnePerLineAfterFirst).
   */
  @Nullable
  private Wrap chainLinkWrap(ASTNode child) {
    if (mySettings.METHOD_CALL_CHAIN_WRAP == CommonCodeStyleSettings.DO_NOT_WRAP) return null;
    boolean linkDot = child.getElementType() == ODOT && isChainLink(myNode);
    return linkDot ? chainItemWrap(mySettings.METHOD_CALL_CHAIN_WRAP) : null;
  }

  /** An extends or implements clause after the first. The clauses share the list's one wrap. */
  @Nullable
  private Wrap inheritClauseWrap(ASTNode child) {
    if (mySettings.EXTENDS_LIST_WRAP == CommonCodeStyleSettings.DO_NOT_WRAP) return null;
    IElementType childType = child.getElementType();
    boolean clause = myNode.getElementType() == INHERIT_LIST
                     && (childType == EXTENDS_DECLARATION || childType == IMPLEMENTS_DECLARATION);
    if (!clause || child == myNode.getFirstChildNode()) return null;
    // "Chop down if long" (stored as WRAP_ON_EVERY_ITEM | WRAP_AS_NEEDED) and
    // "wrap always" must also wrap the first participating clause. Only
    // "wrap if long" leaves it alone, so the break lands where the line
    // overflows instead of at the first clause.
    boolean wrapFirst = (mySettings.EXTENDS_LIST_WRAP & CommonCodeStyleSettings.WRAP_ON_EVERY_ITEM) != 0
                        || mySettings.EXTENDS_LIST_WRAP == CommonCodeStyleSettings.WRAP_ALWAYS;
    return sharedItemWrap(mySettings.EXTENDS_LIST_WRAP, wrapFirst);
  }

  /** A parameter/argument list's parens and items, and a call's closing paren. */
  @Nullable
  private Wrap argumentListWrap(ASTNode child, @Nullable Wrap assignmentSignWrap) {
    IElementType elementType = myNode.getElementType();
    boolean callsWrap = mySettings.CALL_PARAMETERS_WRAP != CommonCodeStyleSettings.DO_NOT_WRAP;
    boolean callCloser = elementType == CALL_EXPRESSION && child.getElementType() == PRPAREN;
    if (callCloser && callsWrap) return wrapIf(mySettings.CALL_PARAMETERS_RPAREN_ON_NEXT_LINE);
    if (!PARAMETER_AND_ARGUMENT_LISTS.contains(elementType)) return null;
    ASTNode parent = myNode.getTreeParent();
    IElementType parentType = parent == null ? null : parent.getElementType();
    if (parentType == CALL_EXPRESSION && callsWrap) return callArgumentWrap(child, assignmentSignWrap);
    // an enum constructor's parameters wrap like a signature's
    boolean signature = FUNCTION_LIKE_OWNERS.contains(parentType) || parentType == ENUM_VALUE_DECLARATION_CONSTRUCTOR;
    boolean signaturesWrap = mySettings.METHOD_PARAMETERS_WRAP != CommonCodeStyleSettings.DO_NOT_WRAP;
    return signature && signaturesWrap ? parameterWrap(child) : null;
  }

  /**
   * A child of a call's argument list. The opening paren follows its own
   * setting. An argument under an assignment becomes a child of the sign's
   * wrap, unless PREFER_PARAMETERS_WRAP gives the arguments precedence.
   */
  private Wrap callArgumentWrap(ASTNode child, @Nullable Wrap assignmentSignWrap) {
    if (myNode.getFirstChildNode() == child) return wrapIf(mySettings.CALL_PARAMETERS_LPAREN_ON_NEXT_LINE);
    WrapType wrapType = WrappingUtil.getWrapType(mySettings.CALL_PARAMETERS_WRAP);
    boolean underAssignment = !mySettings.PREFER_PARAMETERS_WRAP && assignmentSignWrap != null;
    return underAssignment ? Wrap.createChildWrap(assignmentSignWrap, wrapType, true) : Wrap.createWrap(wrapType, true);
  }

  /** A child of a signature's parameter list: the parens per their own settings, a parameter per METHOD_PARAMETERS_WRAP. */
  private Wrap parameterWrap(ASTNode child) {
    if (myNode.getFirstChildNode() == child) return wrapIf(mySettings.METHOD_PARAMETERS_LPAREN_ON_NEXT_LINE);
    if (child.getElementType() == PRPAREN) return wrapIf(mySettings.METHOD_PARAMETERS_RPAREN_ON_NEXT_LINE);
    return Wrap.createWrap(WrappingUtil.getWrapType(mySettings.METHOD_PARAMETERS_WRAP), true);
  }

  /** An argument of {@code new T(a, b)}. These arguments are direct children, with no list node, and wrap like call arguments. */
  @Nullable
  private Wrap newArgumentWrap(ASTNode child) {
    if (mySettings.CALL_PARAMETERS_WRAP == CommonCodeStyleSettings.DO_NOT_WRAP) return null;
    boolean newArgument = myNode.getElementType() == NEW_EXPRESSION && isNewArgument(child);
    return newArgument ? Wrap.createWrap(WrappingUtil.getWrapType(mySettings.CALL_PARAMETERS_WRAP), true) : null;
  }

  /** The {@code else} keyword per ELSE_ON_NEW_LINE. */
  @Nullable
  private Wrap elseKeywordWrap(ASTNode child) {
    boolean elseKeyword = myNode.getElementType() == IF_STATEMENT && child.getElementType() == KELSE;
    return elseKeyword ? wrapIf(mySettings.ELSE_ON_NEW_LINE) : null;
  }

  /** The part of a binary expression that wraps: the operator when the sign goes on the next line, else the right operand. */
  @Nullable
  private Wrap binaryOperandWrap(ASTNode child) {
    if (mySettings.BINARY_OPERATION_WRAP == CommonCodeStyleSettings.DO_NOT_WRAP) return null;
    if (!BINARY_EXPRESSIONS.contains(myNode.getElementType())) return null;
    boolean wraps = mySettings.BINARY_OPERATION_SIGN_ON_NEXT_LINE
                    ? BINARY_OPERATORS.contains(child.getElementType())
                    : isRightOperand(child);
    return wraps ? Wrap.createWrap(WrappingUtil.getWrapType(mySettings.BINARY_OPERATION_WRAP), true) : null;
  }

  /**
   * An assignment's parts. The sign wraps per PLACE_ASSIGNMENT_SIGN_ON_NEXT_LINE.
   * The value wraps per ASSIGNMENT_WRAP, except right after a sign that wraps itself.
   */
  @Nullable
  private Wrap assignmentWrap(ASTNode child) {
    if (mySettings.ASSIGNMENT_WRAP == CommonCodeStyleSettings.DO_NOT_WRAP) return null;
    if (myNode.getElementType() != ASSIGN_EXPRESSION) return null;
    boolean signOnNextLine = mySettings.PLACE_ASSIGNMENT_SIGN_ON_NEXT_LINE;
    if (child.getElementType() == ASSIGN_OPERATION) return signOnNextLine ? Wrap.createWrap(WrapType.NORMAL, true) : null;
    if (signOnNextLine && FormatterUtil.isPrecededBy(child, ASSIGN_OPERATION)) return Wrap.createWrap(WrapType.NONE, true);
    return Wrap.createWrap(WrappingUtil.getWrapType(mySettings.ASSIGNMENT_WRAP), true);
  }

  /**
   * Ternary parts. The grammar wraps the signs into QUESTION_OPERATOR and
   * COLON_OPERATOR elements, so the bare tokens never appear as children
   * here. With signs on the next line, the signs wrap and each branch
   * follows its sign on the same line. Otherwise the branches wrap and the
   * signs stay at the end of the previous line.
   */
  @Nullable
  private Wrap ternaryWrap(ASTNode child) {
    if (myNode.getElementType() != TERNARY_EXPRESSION) return null;
    IElementType childType = child.getElementType();
    boolean sign = childType == QUESTION_OPERATOR || childType == COLON_OPERATOR;
    boolean wraps = myNode.getFirstChildNode() != child && mySettings.TERNARY_OPERATION_SIGNS_ON_NEXT_LINE == sign;
    return wraps
           ? Wrap.createWrap(WrappingUtil.getWrapType(mySettings.TERNARY_OPERATION_WRAP), true)
           : Wrap.createWrap(WrapType.NONE, true);
  }

  private Wrap sharedItemWrap(int wrapSetting) {
    return sharedItemWrap(wrapSetting, true);
  }

  private Wrap sharedItemWrap(int wrapSetting, boolean wrapFirst) {
    if (sharedItemWrap == null) {
      sharedItemWrap = Wrap.createWrap(WrappingUtil.getWrapType(wrapSetting), wrapFirst);
    }
    return sharedItemWrap;
  }

  /** The chain's shared wrap. The outermost link owns it; a nested link asks its parent while the parent is still part of the chain. */
  private Wrap chainItemWrap(int wrapSetting) {
    ASTNode parent = myNode.getTreeParent();
    IElementType parentType = parent == null ? null : parent.getElementType();
    boolean parentIsChain = parentProcessor != null
                            && (parentType == CALL_EXPRESSION || parentType == REFERENCE_EXPRESSION);
    if (parentIsChain) {
      return parentProcessor.chainItemWrap(wrapSetting);
    }
    if (sharedChainWrap == null) {
      sharedChainWrap = Wrap.createWrap(WrappingUtil.getWrapType(wrapSetting), true);
    }
    return sharedChainWrap;
  }

  private boolean isRightOperand(ASTNode child) {
    return myNode.getLastChildNode() == child;
  }

  /** Whether the child is an argument of {@code new T(...)}: it follows the opening paren or a comma, and is not the closing paren. */
  private static boolean isNewArgument(ASTNode child) {
    if (child.getElementType() == PRPAREN) return false;
    ASTNode previous = FormatterUtil.getPreviousNonWhitespaceSibling(child);
    IElementType previousType = previous == null ? null : previous.getElementType();
    return previousType == PLPAREN || previousType == OCOMMA;
  }

  /** The wrap for a child that no rule claims: it never wraps. */
  private static Wrap noWrap() {
    return Wrap.createWrap(WrapType.NONE, false);
  }

  /** A normal wrap when an "on next line" setting is on, else no wrap. */
  private static Wrap wrapIf(boolean onNextLine) {
    return Wrap.createWrap(onNextLine ? WrapType.NORMAL : WrapType.NONE, true);
  }
}
