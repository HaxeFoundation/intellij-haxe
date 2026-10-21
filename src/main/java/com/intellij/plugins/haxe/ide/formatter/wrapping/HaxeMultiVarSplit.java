package com.intellij.plugins.haxe.ide.formatter.wrapping;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.ide.formatter.HaxeIndentText;
import com.intellij.plugins.haxe.ide.formatter.settings.HaxeCodeStyleSettings;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.jetbrains.annotations.NotNull;

import static com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes.LOCAL_VAR_DECLARATION;

/**
 * Reproduces haxe-formatter's wrapping.multiVar split. A declaration list
 * whose joined line reaches the split width puts each declarator on its own
 * line. A list keeps filling instead when one of its declarators is short
 * enough, as the tool measures it, because the tool's anyItemLength rule
 * comes before its split rule.
 */
public final class HaxeMultiVarSplit {

  // the tool measures a declarator with its trailing comma or semicolon; the
  // first one also counts the space after the var keyword
  private static final int DECLARATOR_EXTRA = 1;
  private static final int FIRST_DECLARATOR_EXTRA = 2;

  private HaxeMultiVarSplit() {
  }

  /** Whether the declaration list splits one declarator per line. */
  public static boolean splits(@NotNull ASTNode declarationList, @NotNull CommonCodeStyleSettings common, @NotNull HaxeCodeStyleSettings haxe) {
    int splitWidth = haxe.MULTI_VAR_SPLIT_WIDTH;
    if (splitWidth <= 0) return false;
    int fillItemLength = haxe.MULTI_VAR_FILL_ITEM_LENGTH;
    if (fillItemLength > 0 && shortestDeclaratorLength(declarationList) <= fillItemLength) return false;
    PsiFile file = declarationList.getPsi().getContainingFile();
    if (file == null) return false;
    // the line's current indent stands in for its indent after formatting;
    // the two differ only for unusual input
    CharSequence text = file.getViewProvider().getContents();
    int tabSize = HaxeJoinedLine.tabSize(common);
    int indentColumns = HaxeIndentText.indentWidth(HaxeIndentText.lineIndentAt(text, declarationList.getStartOffset()), tabSize);
    return indentColumns + HaxeJoinedLine.oneLineWidth(declarationList) >= splitWidth;
  }

  /** The length of the shortest declarator, measured as the tool measures multiVar items; 0 for a list without declarators. */
  private static int shortestDeclaratorLength(@NotNull ASTNode declarationList) {
    int shortest = Integer.MAX_VALUE;
    boolean first = true;
    for (ASTNode child = declarationList.getFirstChildNode(); child != null; child = child.getTreeNext()) {
      if (child.getElementType() != LOCAL_VAR_DECLARATION) continue;
      int trailing = first ? FIRST_DECLARATOR_EXTRA : DECLARATOR_EXTRA;
      shortest = Math.min(shortest, HaxeJoinedLine.oneLineWidth(child) + trailing);
      first = false;
    }
    return shortest == Integer.MAX_VALUE ? 0 : shortest;
  }
}
