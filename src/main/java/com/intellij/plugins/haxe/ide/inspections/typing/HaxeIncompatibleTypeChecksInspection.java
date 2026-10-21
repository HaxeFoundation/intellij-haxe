package com.intellij.plugins.haxe.ide.inspections.typing;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.plugins.haxe.lang.psi.HaxeTypeCheckExpr;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.annotator.HaxeProblemReporter;
import com.intellij.plugins.haxe.lang.psi.HaxeMacroValueExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeTypeOrAnonymous;
import com.intellij.plugins.haxe.model.HaxeDocumentModel;
import com.intellij.plugins.haxe.model.fixer.HaxeFixer;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.plugins.haxe.ide.inspections.HaxeInspection;

/** (expr : Type) assertions whose expression does not unify with the asserted type. */
public class HaxeIncompatibleTypeChecksInspection extends HaxeInspection {

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return checkVisitor(holder, HaxeTypeCheckExpr.class, HaxeIncompatibleTypeChecksInspection::check);
  }


  public static void check(@NotNull HaxeTypeCheckExpr expr, @NotNull HaxeProblemReporter reporter) {
    if(PsiTreeUtil.getParentOfType(expr, HaxeMacroValueExpression.class) != null) {
      // TODO: type checks inside macro expressions (reification makes their types dynamic)
      return;
    }
    final PsiElement[] children = expr.getChildren();
    if (children.length == 2) {
      final HaxeGenericResolver resolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(expr);
      final ResultHolder statementResult = HaxeTypeResolver.getPsiElementType(children[0], expr, resolver);
      ResultHolder assertionResult = SpecificTypeReference.getUnknown(expr).createHolder();
      if (children[1] instanceof HaxeTypeOrAnonymous) {
        assertionResult = HaxeTypeResolver.getTypeFromTypeOrAnonymous((HaxeTypeOrAnonymous)children[1]);
        ResultHolder resolveResult = resolver.resolve(assertionResult);
        if (null != resolveResult) {
          assertionResult = resolveResult;
        }
      }
      if (!assertionResult.canAssign(statementResult)) {
        final HaxeDocumentModel document = HaxeDocumentModel.fromElement(expr);
        reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.statement.does.not.unify.with.asserted.type",
                                                                     statementResult.getType().toStringWithoutConstant(),
                                                                     assertionResult.getType().toStringWithoutConstant()))
          .range(children[0])
          .withFix(new HaxeFixer(HaxeBundle.message("haxe.quickfix.remove.type.check")) {
            @Override
            public void run() {
              document.replaceElementText(expr, children[0].getText());
            }
          })
          .withFix(
            new HaxeFixer(HaxeBundle.message("haxe.quickfix.change.type.check.to.0", statementResult.toStringWithoutConstant())) {
              @Override
              public void run() {
                document.replaceElementText(children[1], statementResult.toStringWithoutConstant());
              }
            })
          .create();
        // TODO: Add type conversion fixers. (eg. Wrap with Std.int(), wrap with Std.toString())
      }
    }
  }

}
