package com.intellij.plugins.haxe.ide.inspections.members;

import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.plugins.haxe.lang.psi.HaxeFieldDeclaration;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import com.intellij.plugins.haxe.ide.annotator.HaxeProblemReporter;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.ide.inspections.HaxeInspection;

/** Property accessor identifiers must be one of get/set/null/default/never/dynamic. */
public class HaxePropertyAccessorValidInspection extends HaxeInspection {

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return checkVisitor(holder, HaxeFieldDeclaration.class, HaxePropertyAccessorValidInspection::checkPropertyAccessorValid);
  }
  public static void checkPropertyAccessorValid(final HaxeFieldDeclaration var, final HaxeProblemReporter reporter) {
    HaxeFieldModel field = (HaxeFieldModel)var.getModel();
    if (!field.isProperty()) return;
// TODO: Bug here.  (set,get) are being marked as errors.
    // an accessor parsed as a reference expression is the pre-4.0 custom
    // accessor-method-name form, not a misplaced keyword: valid below 4.0,
    // flagged as removed at 4.0+ by HaxeLanguageFeatureAnnotator
    if (field.getGetterPsi() != null && field.getGetterPsi().getReferenceExpression() == null
        && !field.getGetterType().isValidGetAccessor()) {
      reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.invalid.getter.accessor"))
        .range(field.getGetterPsi())
        .create();
    }

    if (field.getSetterPsi() != null && field.getSetterPsi().getReferenceExpression() == null
        && !field.getSetterType().isValidSetAccessor()) {
      reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.invalid.setter.accessor"))
        .range(field.getSetterPsi())
        .create();
    }
  }

}
