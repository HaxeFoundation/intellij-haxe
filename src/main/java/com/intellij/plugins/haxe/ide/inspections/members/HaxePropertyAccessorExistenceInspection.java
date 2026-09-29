package com.intellij.plugins.haxe.ide.inspections.members;

import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.plugins.haxe.lang.psi.HaxeFieldDeclaration;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import com.intellij.plugins.haxe.ide.annotator.HaxeProblemReporter;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.ide.quickfix.CreateGetterSetterQuickfix;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.ide.inspections.HaxeInspection;

/** get_/set_ methods a property declaration promises must exist. */
public class HaxePropertyAccessorExistenceInspection extends HaxeInspection {

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return checkVisitor(holder, HaxeFieldDeclaration.class, HaxePropertyAccessorExistenceInspection::checkPropertyAccessorExistence);
  }
  public static void checkPropertyAccessorExistence(final HaxeFieldDeclaration var, final HaxeProblemReporter reporter) {
    HaxeFieldModel field = (HaxeFieldModel)var.getModel();
    if (!field.isProperty()) return;
    checkPropertyAccessorMethods(field, reporter);
  }

  private static void checkPropertyAccessorMethods(final HaxeFieldModel field, final HaxeProblemReporter reporter) {
    HaxeClassModel declaringClass = field.getDeclaringClass();
    if(declaringClass != null) {
      if (declaringClass.isInterface() || declaringClass.isAnonymous() || declaringClass.isExtern()) return;
    }


    HaxeCommonMembersModel membersModel = declaringClass != null ? declaringClass : field.getDeclaringModule();

    if (field.getGetterType().isGetter()) {
      HaxeMethodModel getterMethod = field.getGetterMethod();
      if (getterMethod == null && field.getGetterPsi() != null) {
        reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.cannot.find.getter.method"))
          .range(field.getGetterPsi())
          .withFix(new CreateGetterSetterQuickfix(membersModel, field, true))
          .create();
      }
    }

    if (field.getSetterType().isSetter()) {
      HaxeMethodModel setterMethod = field.getSetterMethod();
      if (setterMethod == null && field.getSetterPsi() != null) {
        reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.cannot.find.setter.method"))
          .range(field.getSetterPsi())
          .withFix(new CreateGetterSetterQuickfix(membersModel, field, false))
          .create();
      }
    }
  }

}
