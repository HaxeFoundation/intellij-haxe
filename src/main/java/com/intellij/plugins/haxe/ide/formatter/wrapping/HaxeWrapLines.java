package com.intellij.plugins.haxe.ide.formatter.wrapping;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.ide.formatter.settings.HaxeCodeStyleSettings;
import com.intellij.plugins.haxe.ide.formatter.wrapping.HaxeOperatorChainRules.Kind;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.LOCAL_VAR_DECLARATION;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.LOCAL_VAR_DECLARATION_LIST;

/**
 * Computes a node's joined line after the wraps the tool applies before it
 * judges the node. A split multi-var declaration puts the node's declarator
 * on its own line. An exploding operator chain puts the node's operand on its
 * own line, one step in from the chain's line. A chopped method chain then
 * cuts the line down to the node's link.
 * <p>
 * The computation recurses: an operand's line depends on its chain's line,
 * and a chain's split is judged on the line of the list or value holding it.
 * Every recursive call is on a STRICT ancestor of the node it started from,
 * so the depth of the tree bounds the recursion.
 */
final class HaxeWrapLines {

  private HaxeWrapLines() {
  }

  /** The node's joined line; null when the node is not in a file. */
  @Nullable
  static HaxeJoinedLine lineOf(@NotNull ASTNode node, @NotNull CommonCodeStyleSettings common, @NotNull HaxeCodeStyleSettings haxe) {
    HaxeJoinedLine.Context context = HaxeJoinedLine.Context.of(node, common);
    if (context == null) return null;
    HaxeJoinedLine line = context.statementLine();

    ASTNode declarator = splitDeclaratorOf(node, context.statement(), common, haxe);
    if (declarator != null) {
      line = context.declaratorLine(declarator);
    }

    ASTNode operand = explodedChainOperandOf(node, context.statement(), common, haxe);
    if (operand != null) {
      HaxeJoinedLine chainLine = lineOf(operand.getTreeParent(), common, haxe);
      int chainIndent = chainLine == null ? context.indent() : chainLine.indent();
      line = context.operandLine(operand, chainIndent);
    }

    return context.choppedLinkLine(line, node);
  }

  /** The declarator holding the node, when its declaration list splits one per line. */
  @Nullable
  private static ASTNode splitDeclaratorOf(ASTNode node, ASTNode statement, CommonCodeStyleSettings common, HaxeCodeStyleSettings haxe) {
    for (ASTNode ancestor = node; ancestor != null && ancestor != statement; ancestor = ancestor.getTreeParent()) {
      if (ancestor.getElementType() != LOCAL_VAR_DECLARATION) continue;
      ASTNode list = ancestor.getTreeParent();
      boolean splits = list != null
                       && list.getElementType() == LOCAL_VAR_DECLARATION_LIST
                       && HaxeMultiVarSplit.splits(list, common, haxe);
      return splits ? ancestor : null;
    }
    return null;
  }

  /**
   * The innermost operand below the statement that holds the node and
   * belongs to an exploding operator chain of either kind; null when no
   * enclosing chain explodes.
   */
  @Nullable
  private static ASTNode explodedChainOperandOf(ASTNode node, ASTNode statement, CommonCodeStyleSettings common, HaxeCodeStyleSettings haxe) {
    for (ASTNode ancestor = node; ancestor != null && ancestor != statement; ancestor = ancestor.getTreeParent()) {
      ASTNode parent = ancestor.getTreeParent();
      if (parent == null) return null;
      for (Kind kind : Kind.values()) {
        boolean operand = kind.chainTypes.contains(parent.getElementType()) && !kind.isOperator(ancestor)
                          && !kind.chainTypes.contains(ancestor.getElementType());
        if (operand && HaxeOperatorChainRules.explodes(kind, parent, common, haxe)) return ancestor;
      }
    }
    return null;
  }
}
