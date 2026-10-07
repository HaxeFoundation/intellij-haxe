package com.intellij.plugins.haxe.runner.debugger.interp;

import static com.intellij.plugins.haxe.runner.debugger.eval.EvalDebugAdapter.stripTrailingSemicolons;

import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeDebuggerBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeExpression;
import com.intellij.plugins.haxe.util.HaxeElementGenerator;
import com.intellij.plugins.haxe.util.HaxeReadActions;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Checks a breakpoint condition BEFORE it reaches the eval VM. The VM parses
 * a condition when the breakpoint is registered, and a parse failure kills
 * its debug socket thread: no error response, and every later request of the
 * session times out. Only text the plugin's parser reads as exactly one Haxe
 * expression is let through; anything else is reported on the breakpoint.
 */
final class InterpConditionValidator {
  private InterpConditionValidator() {
  }

  /** Why {@code condition} cannot be sent to the VM, or null when it is one well-formed expression. */
  static @Nullable String problem(Project project, @NotNull String condition) {
    // the adapter drops trailing semicolons before sending, so they are not a defect here
    String sent = stripTrailingSemicolons(condition);
    return HaxeReadActions.compute(() -> problemUnderReadLock(project, sent));
  }

  private static @Nullable String problemUnderReadLock(Project project, String condition) {
    PsiFile fragment = HaxeElementGenerator.createExpressionCodeFragment(project, condition, null, false);
    PsiErrorElement error = PsiTreeUtil.findChildOfType(fragment, PsiErrorElement.class);
    if (error != null) {
      return HaxeDebuggerBundle.message("interp.breakpoint.condition.invalid", error.getErrorDescription());
    }
    // the fragment parser reads one expression and swallows whatever follows,
    // so "a == 1; b()" parses clean - the expression must be all there is
    List<PsiElement> parsed = significantChildren(fragment.getFirstChild());
    boolean single = parsed.size() == 1 && parsed.getFirst() instanceof HaxeExpression;
    return single ? null : HaxeDebuggerBundle.message("interp.breakpoint.condition.not.single");
  }

  private static List<PsiElement> significantChildren(@Nullable PsiElement parent) {
    List<PsiElement> children = new ArrayList<>();
    if (parent == null) {
      return children;
    }
    for (PsiElement child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (!(child instanceof PsiWhiteSpace) && !(child instanceof PsiComment)) {
        children.add(child);
      }
    }
    return children;
  }
}
