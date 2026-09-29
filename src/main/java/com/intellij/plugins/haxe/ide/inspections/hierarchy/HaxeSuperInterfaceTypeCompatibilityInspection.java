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
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import java.util.*;
import com.intellij.plugins.haxe.ide.inspections.HaxeInspection;

/** The implements clause names something that is not an interface. */
public class HaxeSuperInterfaceTypeCompatibilityInspection extends HaxeInspection {

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return checkVisitor(holder, HaxeClass.class, HaxeSuperInterfaceTypeCompatibilityInspection::checkSuperInterfaces);
  }
  public static void checkSuperInterfaces(final HaxeClass clazzPsi, final HaxeProblemReporter reporter) {
    HaxeClassModel clazz = HaxeClassInspectionUtil.modelToCheck(clazzPsi);
    if (clazz != null) checkInterfaces(clazz, reporter);
  }
  private static void checkInterfaces(final HaxeClassModel clazz, final HaxeProblemReporter reporter) {

    for (HaxeClassReferenceModel interfaze : clazz.getImplementingInterfaces()) {
      HaxeClassModel interfazeModel = interfaze.getHaxeClassModel();
      HaxeClass interfazeClass = null;
      if (interfazeModel != null) {
        if (interfazeModel.isAnonymous()) {
          SpecificHaxeClassReference reference =
            interfazeModel.getUnderlyingClassReference(interfaze.getSpecificHaxeClassReference().getGenericResolver());
          if (reference!= null) interfazeClass = reference.getHaxeClass();
        }else {
          interfazeClass = interfazeModel.haxeClass;
        }
      }

      if (interfazeClass != null) {
        if (clazz.isInterface()) {
          HaxeProblemReporter.Problem builder =
            reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.interfaces.cannot.implement.interface"))
              .range(interfaze.getPsi());
          if (interfazeClass.isInterface()) {
            builder.withFix(HaxeFixer.create(HaxeBundle.message("haxe.quickfix.change.to.extends"), () -> clazz.changeToExtends(interfazeModel.getName())));
          }
          builder.create();
        } else {
          boolean isDynamic = SpecificHaxeClassReference.withoutGenerics(interfazeModel.getReference()).isDynamic();
          if (!(interfazeClass.isInterface() || isDynamic)) {
            HaxeProblemReporter.Problem builder =
              reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.interface.error.message"))
                .range(interfaze.getPsi());
            if (interfazeModel.isClass() || interfazeModel.isAbstractClass()) {
              builder.withFix(HaxeFixer.create(HaxeBundle.message("haxe.quickfix.change.to.extends"), () -> clazz.changeToExtends(interfazeModel.getName())));
            }
            builder.create();
          }
        }
      }
    }
  }

}
