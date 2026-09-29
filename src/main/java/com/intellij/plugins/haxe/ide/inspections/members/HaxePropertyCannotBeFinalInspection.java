package com.intellij.plugins.haxe.ide.inspections.members;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.plugins.haxe.lang.psi.HaxeFieldDeclaration;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import com.intellij.plugins.haxe.ide.annotator.HaxeProblemReporter;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.quickfix.HaxeSwitchMutabilityModifier;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.ide.inspections.HaxeInspection;

/** Properties cannot be declared final. */
public class HaxePropertyCannotBeFinalInspection extends HaxeInspection {

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return checkVisitor(holder, HaxeFieldDeclaration.class, HaxePropertyCannotBeFinalInspection::checkPropertyNotFinal);
  }
  public static void checkPropertyNotFinal(final HaxeFieldDeclaration var, final HaxeProblemReporter reporter) {
    HaxeFieldModel field = (HaxeFieldModel)var.getModel();
    if (!field.isProperty() || !field.isFinal()) return;
    reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.property.cant.be.final"))
      .range(field.getBasePsi())
      .withFix(new HaxeSwitchMutabilityModifier((HaxeFieldDeclaration)field.getBasePsi()))
      .create();
  }

}
