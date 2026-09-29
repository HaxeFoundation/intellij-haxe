package com.intellij.plugins.haxe.ide.inspections.hierarchy;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import com.intellij.plugins.haxe.ide.annotator.HaxeProblemReporter;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.fixer.HaxeFixer;
import com.intellij.psi.PsiElement;
import java.util.*;
import com.intellij.plugins.haxe.ide.inspections.HaxeInspection;

/** The extends clause names something that cannot be extended here. */
public class HaxeSuperclassTypeCompatibilityInspection extends HaxeInspection {

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return checkVisitor(holder, HaxeClass.class, HaxeSuperclassTypeCompatibilityInspection::checkSuperclass);
  }
  public static void checkSuperclass(final HaxeClass clazzPsi, final HaxeProblemReporter reporter) {
    HaxeClassModel clazz = HaxeClassInspectionUtil.modelToCheck(clazzPsi);
    if (clazz != null) checkExtends(clazz, reporter);
  }
  private static void checkExtends(final HaxeClassModel clazz, final HaxeProblemReporter reporter) {

    //HaxeClassModel reference = clazz.getParentClass(); // Get first in extends list, not PSI parent.
    for (HaxeType type : clazz.getExtendsList()) {
      HaxeReferenceExpression referenceExpression = type.getReferenceExpression();
      PsiElement resolved = referenceExpression.resolve();
      if (resolved instanceof HaxeClass haxeClass) {
        HaxeClassModel extendedClassModel = haxeClass.getModel();


        // TODO: Need to loop over all interfaces or types.
        if (extendedClassModel != null) {
          if (HaxeClassInspectionUtil.isAnonymousType(clazz)) {
            if (!HaxeClassInspectionUtil.isAnonymousType(extendedClassModel)) {
              reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.not.anonymous.type")).range(referenceExpression)
                .create();
            }
          }
          else if (clazz.isInterface()) {
            if (!extendedClassModel.isInterface() && !extendedClassModel.isTypedef()) {
              String errorMessage = HaxeBundle.message("haxe.semantic.cannot.extend.not.interface", extendedClassModel.getName());
              reporter.problem(HighlightSeverity.ERROR, errorMessage)
                      .range(referenceExpression)
                      .create();
            }
          }
          else if (clazz.isClass()) {
            if (!extendedClassModel.isClass() && !extendedClassModel.isTypedef()) {
              String errorMessage = HaxeBundle.message("haxe.semantic.cannot.extend.not.class", extendedClassModel.getName());
              HaxeProblemReporter.Problem builder = reporter.problem(HighlightSeverity.ERROR, errorMessage).range(referenceExpression);

              if(extendedClassModel.isInterface()) {
                String popupMessage = HaxeBundle.message("haxe.quickfix.change.to.implements");
                builder = builder.withFix(HaxeFixer.create(popupMessage, () -> clazz.changeToInterface(extendedClassModel.getName())));
              }

              builder.create();
            }
          }

          final String qname1 = extendedClassModel.haxeClass.getQualifiedName();
          final String qname2 = clazz.haxeClass.getQualifiedName();
          if (qname1 != null && qname1.equals(qname2)) {
            reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.cannot.extend.self"))
                    .range(referenceExpression)
                    .create();
          }
        }
      }
    }
  }

}
