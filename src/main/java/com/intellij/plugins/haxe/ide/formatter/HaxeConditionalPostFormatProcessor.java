package com.intellij.plugins.haxe.ide.formatter;

import com.intellij.formatting.IndentInfo;
import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.ide.formatter.settings.HaxeCodeStyleSettings;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeInactiveBody;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypeSets.PPBODY;
import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.*;

/**
 * Aligns the inactive branches of #if/#elseif/#else regions after a reformat.
 * <p>
 * A branch that parses cleanly is block-formatted like active code (see
 * FORMAT_INACTIVE_BRANCHES). This pass serves only the branches that do not
 * parse and so stay one unstructured piece of text. It shifts all lines of
 * such a branch by the same amount, keeping their relative nesting, so the
 * branch lines up with the nearest directive before it. Regions written on
 * a single line are left alone. A region nested inside an inactive branch
 * is part of that branch's text, because the lexer does not split it out,
 * so its directives move together with the enclosing branch.
 */
public class HaxeConditionalPostFormatProcessor extends HaxeTextPostFormatProcessor {

  private static final TokenSet DIRECTIVES_AND_BRANCHES = TokenSet.create(PPIF, PPELSEIF, PPELSE, PPEND, PPBODY);

  @Override
  protected boolean enabled(@NotNull HaxeCodeStyleSettings settings) {
    return settings.ALIGN_INACTIVE_CONDITIONAL_BRANCHES;
  }

  @Override
  protected boolean handles(@NotNull ASTNode node) {
    return DIRECTIVES_AND_BRANCHES.contains(node.getElementType());
  }

  @Override
  protected @NotNull List<Replacement> replacements(@NotNull List<ASTNode> leaves, @NotNull Pass pass) {
    return new RegionWalk(leaves, pass).run();
  }

  /** Whether block formatting owns the branch; this pass then leaves it alone. */
  private static boolean isBlockFormatted(@NotNull ASTNode leaf, @NotNull HaxeCodeStyleSettings settings) {
    return leaf.getPsi() instanceof HaxeInactiveBody body && !HaxeInactiveBranches.isPreservedVerbatim(body, settings);
  }

  /**
   * Shifts every line of the branch text so that its first code line sits at
   * {@code firstLineIndent}. A whitespace-only last line holds the indent of
   * the next directive and becomes exactly {@code trailingIndent}.
   */
  private static String reindentBranchText(String branchText, String firstLineIndent, String trailingIndent,
                                           CommonCodeStyleSettings.IndentOptions options) {
    if (branchText.indexOf('\n') < 0) return branchText;
    // every line, trailing empty ones kept
    String[] lines = branchText.split("\n", -1);
    int targetColumns = HaxeIndentText.indentWidth(firstLineIndent, options.TAB_SIZE);
    int referenceColumns = firstInteriorIndentColumns(lines, options.TAB_SIZE);
    int delta = referenceColumns < 0 ? 0 : targetColumns - referenceColumns;

    StringBuilder result = new StringBuilder(branchText.length());
    result.append(lines[0]);
    for (int i = 1; i < lines.length; i++) {
      result.append('\n');
      String line = lines[i];
      boolean trailingIndentLine = i == lines.length - 1 && line.isBlank();
      if (trailingIndentLine) {
        result.append(trailingIndent);
      }
      else if (!line.isBlank()) {
        String lead = HaxeIndentText.leadingWhitespace(line, 0);
        int columns = Math.max(0, HaxeIndentText.indentWidth(lead, options.TAB_SIZE) + delta);
        result.append(renderIndent(columns, options));
        result.append(line, lead.length(), line.length());
      }
    }
    return result.toString();
  }

  /** The indent column of the first non-blank line after the opening one; -1 when there is none. */
  private static int firstInteriorIndentColumns(String[] lines, int tabSize) {
    for (int i = 1; i < lines.length; i++) {
      if (lines[i].isBlank()) continue;
      return HaxeIndentText.indentWidth(HaxeIndentText.leadingWhitespace(lines[i], 0), tabSize);
    }
    return -1;
  }

  /** The whitespace that reaches the column: tabs of TAB_SIZE plus remaining spaces, or only spaces. */
  private static String renderIndent(int columns, CommonCodeStyleSettings.IndentOptions options) {
    return new IndentInfo(0, columns, 0).generateNewWhiteSpace(options);
  }

  /**
   * A walk over the directives and branches in file order. It edits a working
   * copy of the text as it goes, because reindenting a branch also moves the
   * directive after it, and that directive's new indent is the target for
   * the next branch.
   */
  private static final class RegionWalk {
    private final List<ASTNode> leaves;
    private final Pass pass;
    private final CommonCodeStyleSettings.IndentOptions options;
    private final StringBuilder workingText;
    private final List<Replacement> replacements = new ArrayList<>();
    // how far the edits so far have moved original offsets in the working text
    private int shift;
    /** The line indent of the nearest preceding directive; null before the first. */
    @Nullable private String targetIndent;

    RegionWalk(List<ASTNode> leaves, Pass pass) {
      this.leaves = leaves;
      this.pass = pass;
      options = pass.indentOptions();
      workingText = new StringBuilder(pass.text());
    }

    List<Replacement> run() {
      for (ASTNode leaf : leaves) {
        if (leaf.getElementType() == PPBODY) alignBranch(leaf);
        else recordDirectiveIndent(leaf);
      }
      return replacements;
    }

    private void recordDirectiveIndent(ASTNode leaf) {
      if (leaf.getElementType() == PPEND) return;
      targetIndent = HaxeIndentText.lineIndentAt(workingText, leaf.getStartOffset() + shift);
    }

    private void alignBranch(ASTNode leaf) {
      boolean alignable = targetIndent != null && !isBlockFormatted(leaf, pass.haxeSettings());
      if (alignable) reindent(leaf, targetIndent, targetIndent);
    }

    private void reindent(ASTNode leaf, String firstLineIndent, String trailingIndent) {
      if (!pass.covers(leaf)) return;
      int start = leaf.getStartOffset() + shift;
      String branchText = workingText.substring(start, start + leaf.getTextLength());
      String reindented = reindentBranchText(branchText, firstLineIndent, trailingIndent, options);
      workingText.replace(start, start + branchText.length(), reindented);
      shift += reindented.length() - branchText.length();
      if (!reindented.equals(branchText)) {
        replacements.add(new Replacement(leaf, reindented));
      }
    }
  }
}
