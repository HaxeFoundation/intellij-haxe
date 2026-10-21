package com.intellij.plugins.haxe.ide.inspections.style;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.plugins.haxe.lang.psi.HaxeStringLiteralExpression;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.annotator.HaxeProblemReporter;
import com.intellij.plugins.haxe.model.fixer.HaxeSurroundFixer;
import static com.intellij.plugins.haxe.ide.annotator.semantics.HaxeStringTemplateUtils.isSingleQuotesRequired;
import com.intellij.plugins.haxe.ide.inspections.HaxeInspection;

/** Interpolation in double-quoted strings, where $ does not interpolate. */
public class HaxeStringInterpolationQuoteInspection extends HaxeInspection {

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return checkVisitor(holder, HaxeStringLiteralExpression.class, HaxeStringInterpolationQuoteInspection::check);
  }


  public static void check(@NotNull HaxeStringLiteralExpression psi, @NotNull HaxeProblemReporter reporter) {
    if (isSingleQuotesRequired(psi)) {
      reporter.problem(HighlightSeverity.WARNING,
                       HaxeBundle.message(
                         "haxe.semantic.inspection.message.expression.that.contains.string.interpolation.should.be.wrapped.with.single.quotes"))
        .range(psi)
        .withFix(HaxeSurroundFixer.replaceQuotesWithSingleQuotes(psi))
        .create();
    }
  }

}
