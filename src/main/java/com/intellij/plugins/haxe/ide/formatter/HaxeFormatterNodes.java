package com.intellij.plugins.haxe.ide.formatter;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.intellij.plugins.haxe.ide.formatter.HaxeFormatterTokenSets.FUNCTION_HEADER_END;
import static com.intellij.plugins.haxe.ide.formatter.HaxeFormatterTokenSets.FUNCTION_LIKE_OWNERS;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.COMMENTS;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.WHITESPACES;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;

/** AST predicates, tree walks and settings queries shared by the formatter's processors. */
public final class HaxeFormatterNodes {

  private HaxeFormatterNodes() {
  }

  /**
   * Whether the node is a link of a method chain: a REFERENCE_EXPRESSION whose
   * receiver (its first child) is itself a call, like {@code a().b} in {@code a().b()}.
   */
  public static boolean isChainLink(@Nullable ASTNode reference) {
    if (reference == null || reference.getElementType() != REFERENCE_EXPRESSION) return false;
    ASTNode receiver = reference.getFirstChildNode();
    return receiver != null && receiver.getElementType() == CALL_EXPRESSION;
  }

  /** The highest ancestor reachable from the node through parents of the given kinds; the node itself when its parent is of another kind. */
  @NotNull
  public static ASTNode outermostOfKind(@NotNull ASTNode node, @NotNull TokenSet kinds) {
    ASTNode root = node;
    while (root.getTreeParent() != null && kinds.contains(root.getTreeParent().getElementType())) {
      root = root.getTreeParent();
    }
    return root;
  }

  /** A run of metadata siblings and the code node that follows them on the same line. */
  public record MetadataRun(List<ASTNode> metadata, ASTNode decorated) {
  }

  /**
   * The run of EMBEDDED_META siblings that starts at {@code node} and ends at
   * the code node after them on the same line. In the PSI, metadata is a
   * sibling of the declaration it decorates, not its child, so this run is
   * the only link between a declaration and the metadata before it on its
   * line. Null when {@code node} is not metadata, when a line break follows
   * the run, or when a comment or directive ends it.
   */
  @Nullable
  public static MetadataRun sameLineMetadataRun(@NotNull ASTNode node) {
    if (node.getElementType() != EMBEDDED_META) return null;
    List<ASTNode> metadata = new ArrayList<>();
    for (ASTNode sibling = node; sibling != null; sibling = sibling.getTreeNext()) {
      IElementType type = sibling.getElementType();
      if (type == EMBEDDED_META) {
        metadata.add(sibling);
      }
      else if (WHITESPACES.contains(type)) {
        if (sibling.textContains('\n')) return null;
      }
      else {
        return COMMENTS.contains(type) ? null : new MetadataRun(metadata, sibling);
      }
    }
    return null;
  }

  /**
   * The #if or #end of a conditional-compilation region, found by walking
   * from a leaf, and whether the walk crossed a newline. The directive is
   * null when the region has no such end in that direction.
   */
  public record RegionEnd(@Nullable ASTNode directive, boolean crossedNewline) {
  }

  /** The #if that opens the leaf's region, skipping nested regions, and whether a newline lies between them. */
  @NotNull
  public static RegionEnd regionOpener(@NotNull ASTNode leaf) {
    return regionEnd(leaf, false);
  }

  /** The #end that closes the leaf's region, skipping nested regions, and whether a newline lies between them. */
  @NotNull
  public static RegionEnd regionCloser(@NotNull ASTNode leaf) {
    return regionEnd(leaf, true);
  }

  private static RegionEnd regionEnd(ASTNode leaf, boolean forward) {
    IElementType target = forward ? PPEND : PPIF;
    IElementType nested = forward ? PPIF : PPEND;
    int depth = 0;
    boolean crossedNewline = false;
    PsiElement current = leaf.getPsi();
    while ((current = neighbourLeaf(current, forward)) != null) {
      IElementType type = current.getNode().getElementType();
      if (type == target && depth == 0) return new RegionEnd(current.getNode(), crossedNewline);
      if (type == target) depth--;
      else if (type == nested) depth++;
      else if (current.textContains('\n')) crossedNewline = true;
    }
    return new RegionEnd(null, crossedNewline);
  }

  @Nullable
  private static PsiElement neighbourLeaf(PsiElement leaf, boolean forward) {
    return forward ? PsiTreeUtil.nextLeaf(leaf) : PsiTreeUtil.prevLeaf(leaf);
  }

  /** Whether the node is a BLOCK_STATEMENT holding nothing but its braces and whitespace. */
  public static boolean isEmptyBlock(@Nullable ASTNode block) {
    if (block == null || block.getElementType() != BLOCK_STATEMENT) return false;
    for (ASTNode child = block.getFirstChildNode(); child != null; child = child.getTreeNext()) {
      IElementType type = child.getElementType();
      if (type != PLCURLY && type != PRCURLY && !WHITESPACES.contains(type)) return false;
    }
    return true;
  }

  /** A function's body: the first node after the parameters that is not part of the header; null for a declaration without a body. */
  @Nullable
  public static ASTNode functionBody(@NotNull ASTNode function) {
    ASTNode parameters = function.findChildByType(PARAMETER_LIST);
    if (parameters == null) return null;
    for (ASTNode child = parameters.getTreeNext(); child != null; child = child.getTreeNext()) {
      IElementType type = child.getElementType();
      boolean header = FUNCTION_HEADER_END.contains(type) || type == OSEMI || WHITESPACES.contains(type) || COMMENTS.contains(type);
      if (!header) return child;
    }
    return null;
  }

  /** Whether the function has a body that is neither absent nor an empty block. */
  public static boolean hasStatementBody(@NotNull ASTNode function) {
    ASTNode body = functionBody(function);
    return body != null && !isEmptyBlock(body);
  }

  /** Whether the statement brace style puts a '{' on its own line: NEXT_LINE, NEXT_LINE_SHIFTED or NEXT_LINE_SHIFTED2. */
  public static boolean isBraceOnNextLine(@NotNull CommonCodeStyleSettings settings) {
    return isNextLineStyle(settings.BRACE_STYLE);
  }

  /** Whether the body's '{' goes on its own line: under the method brace style for a function body, the statement style otherwise. */
  public static boolean isBraceOnNextLine(@NotNull CommonCodeStyleSettings settings, @NotNull ASTNode body) {
    ASTNode owner = body.getTreeParent();
    boolean function = owner != null && FUNCTION_LIKE_OWNERS.contains(owner.getElementType());
    return isNextLineStyle(function ? settings.METHOD_BRACE_STYLE : settings.BRACE_STYLE);
  }

  private static boolean isNextLineStyle(int braceStyle) {
    return braceStyle == CommonCodeStyleSettings.NEXT_LINE
           || braceStyle == CommonCodeStyleSettings.NEXT_LINE_SHIFTED
           || braceStyle == CommonCodeStyleSettings.NEXT_LINE_SHIFTED2;
  }
}
