package com.intellij.plugins.haxe.ide.inspections.members;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import com.intellij.plugins.haxe.ide.annotator.HaxeProblemReporter;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.ide.inspections.HaxeInspection;

/** Extern-class and interface methods must declare parameter and return types. */
public class HaxeMissingTypeTagOnExternAndInterfaceInspection extends HaxeInspection {

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return checkVisitor(holder, HaxeMethod.class, HaxeMissingTypeTagOnExternAndInterfaceInspection::checkMissingTypeTags);
  }
  public static void checkMissingTypeTags(final HaxeMethod methodPsi, final HaxeProblemReporter reporter) {
    checkTypeTagInInterfacesAndExternClass(methodPsi.getModel(), reporter);
  }
  private static void checkTypeTagInInterfacesAndExternClass(final HaxeMethodModel currentMethod, final HaxeProblemReporter reporter) {
    HaxeClassModel currentClass = currentMethod.getDeclaringClass();
    if (currentClass != null) { //make sure it's not a module method
      if (currentClass.isExtern() || currentClass.isInterface()) {
        if (currentMethod.getReturnTypeTagPsi() == null && !currentMethod.isConstructor()) {
          reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.type.required"))
            .range(currentMethod.getNameOrBasePsi())
            .create();
        }
        for (final HaxeParameterModel param : currentMethod.getParameters()) {
          if (param.getTypeTagPsi() == null) {
            reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.type.required"))
              .range(param.getBasePsi())
              .create();
          }
        }
      }
    }
  }

}
