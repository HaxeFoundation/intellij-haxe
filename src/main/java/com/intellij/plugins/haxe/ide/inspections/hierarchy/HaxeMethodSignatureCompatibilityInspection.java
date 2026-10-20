package com.intellij.plugins.haxe.ide.inspections.hierarchy;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import com.intellij.plugins.haxe.ide.annotator.HaxeProblemReporter;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.evaluator.assign.HaxeFunctionCompatible;
import com.intellij.plugins.haxe.model.evaluator.assign.HaxeOverrideOrImplementEvaluation;
import com.intellij.plugins.haxe.model.fixer.*;
import static com.intellij.plugins.haxe.lang.psi.HaxePsiModifier.*;
import com.intellij.plugins.haxe.ide.inspections.HaxeInspection;

/** Signature compatibility against the overridden or implemented ancestor method. */
public class HaxeMethodSignatureCompatibilityInspection extends HaxeInspection {

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return checkVisitor(holder, HaxeMethod.class, HaxeMethodSignatureCompatibilityInspection::checkSignatureAgainstAncestor);
  }

  /**
   * Signature compatibility of an {@code override} method against its
   * ancestor. Abstract-base and interface conformance is class-side checking
   * (the class-shaped inspections), so those ancestors are excluded here.
   */
  public static void checkSignatureAgainstAncestor(final HaxeMethod methodPsi, final HaxeProblemReporter reporter) {
    if (methodPsi instanceof HaxeLocalFunctionDeclaration) return;
    final HaxeMethodModel currentMethod = methodPsi.getModel();
    if (currentMethod.isConstructor() || currentMethod.isStaticInit()) return;
    if (!currentMethod.getModifiers().hasModifier(OVERRIDE)) return;
    final HaxeClassModel currentClass = currentMethod.getDeclaringClass();
    if (currentClass == null || currentClass.isInterface() || currentClass.isAnonymous()) return;
    final HaxeMethodModel parentMethod = currentClass.getAncestorMethod(currentMethod.getName(), null);
    if (parentMethod == null || parentMethod.isStatic() || parentMethod.isAbstract()) return;
    final HaxeClassModel parentClass = parentMethod.getDeclaringClass();
    if (parentClass != null && parentClass.isInterface()) return;
    checkMethodsSignatureCompatibility(currentMethod, parentMethod, reporter, true);
  }

  public static boolean checkMethodsSignatureCompatibility(
    @NotNull final HaxeMethodModel currentMethod,
    @NotNull final HaxeMethodModel parentMethod ) {
    return checkMethodsSignatureCompatibility(currentMethod,parentMethod, null, false);
  }

  public static boolean checkMethodsSignatureCompatibility(
    @NotNull final HaxeMethodModel currentMethod,
    @NotNull final HaxeMethodModel parentMethod,
    final HaxeProblemReporter reporter,
    boolean shouldAnnotate) {

    if (parentMethod.isInInterface() && !currentMethod.isInInterface()) {
      HaxeOverrideOrImplementEvaluation implementCheck = HaxeFunctionCompatible.checkThisImplementsThat(currentMethod,parentMethod, shouldAnnotate);
      implementCheck.annotate(reporter);
      return implementCheck.result;

    } else {
      HaxeOverrideOrImplementEvaluation overrideCheck = HaxeFunctionCompatible.checkThisOverrideThat( currentMethod,parentMethod, shouldAnnotate);
      overrideCheck.annotate(reporter);
      return overrideCheck.result;
    }
  }

}
