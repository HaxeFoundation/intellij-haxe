package com.intellij.plugins.haxe.ide.formatter;

import com.intellij.lang.ASTNode;
import com.intellij.plugins.haxe.ide.formatter.settings.HaxeCodeStyleSettings;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeInactiveBody;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Decides which inactive conditional-compilation branches the block
 * formatter leaves unformatted. With FORMAT_INACTIVE_BRANCHES off that is
 * every branch; otherwise it is every branch that does not parse cleanly.
 * The block formatter and the comment passes skip such a branch; the branch
 * aligner (ALIGN_INACTIVE_CONDITIONAL_BRANCHES) serves exactly these and
 * shifts each one as a group to its directive, so only with the aligner off
 * too does the branch stay byte-identical.
 */
public final class HaxeInactiveBranches {

  private HaxeInactiveBranches() {
  }

  public static boolean isPreservedVerbatim(@NotNull HaxeInactiveBody body, @NotNull HaxeCodeStyleSettings settings) {
    return !settings.FORMAT_INACTIVE_BRANCHES || !body.hasCleanParse();
  }

  /** Whether the node lies inside an inactive branch that stays as written. */
  public static boolean isInsidePreservedBranch(@NotNull ASTNode node, @NotNull HaxeCodeStyleSettings settings) {
    HaxeInactiveBody body = PsiTreeUtil.getParentOfType(node.getPsi(), HaxeInactiveBody.class);
    return body != null && isPreservedVerbatim(body, settings);
  }
}
