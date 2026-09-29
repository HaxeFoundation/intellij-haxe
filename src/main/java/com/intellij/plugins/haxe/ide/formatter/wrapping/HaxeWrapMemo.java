package com.intellij.plugins.haxe.ide.formatter.wrapping;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.Key;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.ide.formatter.settings.HaxeCodeStyleSettings;
import com.intellij.plugins.haxe.ide.formatter.wrapping.HaxeOperatorChainRules.Kind;
import com.intellij.plugins.haxe.ide.formatter.wrapping.HaxeOperatorChainRules.Thresholds;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.util.PsiModificationTracker;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Caches wrap decisions, such as an argument list's moved arguments or an
 * operator chain's split, as user data on the node's PSI. The formatter asks
 * for the same decision once per pair of adjacent blocks, so it is computed
 * once per node.
 * <p>
 * An entry stays valid while nothing the decision reads has changed: the
 * file's tree and text (the PSI modification count and the file's
 * modification stamp) and the settings the wrap rules read. Settings are
 * compared by VALUE, because a transient per-file settings copy has no
 * stable identity and a scheme's fields change without notice.
 */
final class HaxeWrapMemo {

  /** A cached value with the inputs it was computed from. */
  record Entry<T>(Inputs inputs, T value) {
  }

  /** Everything a wrap decision reads besides the node's subtree: the file's state and the wrap rules' settings. */
  record Inputs(long psiModificationCount, long fileStamp, int margin, int tabSize, int chainWrap, int arrayWrap,
                Thresholds additive, Thresholds logic, HaxeLiteralItemRules.AllThresholds literals,
                int multiVarSplitWidth, int multiVarFillItemLength) {

    static Inputs of(@NotNull PsiFile file, @NotNull CommonCodeStyleSettings common, @NotNull HaxeCodeStyleSettings haxe) {
      long psiModificationCount = PsiModificationTracker.getInstance(file.getProject()).getModificationCount();
      int margin = common.getRootSettings().getRightMargin(HaxeLanguage.INSTANCE);
      return new Inputs(psiModificationCount, file.getModificationStamp(), margin, HaxeJoinedLine.tabSize(common),
                        common.METHOD_CALL_CHAIN_WRAP, common.ARRAY_INITIALIZER_WRAP,
                        Kind.ADDITIVE.thresholds(haxe), Kind.LOGIC.thresholds(haxe), HaxeLiteralItemRules.AllThresholds.of(haxe),
                        haxe.MULTI_VAR_SPLIT_WIDTH, haxe.MULTI_VAR_FILL_ITEM_LENGTH);
    }
  }

  private HaxeWrapMemo() {
  }

  /** The node's cached value under the key, recomputed when its inputs have changed. */
  static <T> T cached(@NotNull ASTNode node, @NotNull Key<Entry<T>> key,
                      @NotNull CommonCodeStyleSettings common, @NotNull HaxeCodeStyleSettings haxe, @NotNull Supplier<T> compute) {
    PsiElement psi = node.getPsi();
    PsiFile file = psi.getContainingFile();
    if (file == null) return compute.get();
    Inputs inputs = Inputs.of(file, common, haxe);
    Entry<T> entry = psi.getUserData(key);
    if (entry != null && entry.inputs().equals(inputs)) return entry.value();
    T value = compute.get();
    psi.putUserData(key, new Entry<>(inputs, value));
    return value;
  }
}
