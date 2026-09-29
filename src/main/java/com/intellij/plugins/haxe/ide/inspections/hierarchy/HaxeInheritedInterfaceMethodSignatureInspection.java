package com.intellij.plugins.haxe.ide.inspections.hierarchy;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import com.intellij.plugins.haxe.ide.annotator.HaxeProblemReporter;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.*;
import java.util.*;
import com.intellij.plugins.haxe.ide.inspections.HaxeInspection;

/** Conformance against methods of INHERITED (super-) interfaces. */
public class HaxeInheritedInterfaceMethodSignatureInspection extends HaxeInspection {

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return checkVisitor(holder, HaxeClass.class, HaxeInheritedInterfaceMethodSignatureInspection::checkInheritedInterfaceMethodSignatures);
  }
  public static void checkInheritedInterfaceMethodSignatures(final HaxeClass clazzPsi, final HaxeProblemReporter reporter) {
    HaxeClassModel clazz = HaxeClassInspectionUtil.modelToCheck(clazzPsi);
    if (clazz == null || clazzPsi.isInterface() || clazzPsi.isTypeDef()) return;
    HaxeClassInspectionUtil.checkImplementedInterfaces(clazz, reporter, false, false, true);
  }

}
