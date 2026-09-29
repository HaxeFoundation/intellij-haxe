package com.intellij.plugins.haxe.ide.inspections.hierarchy;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import com.intellij.plugins.haxe.ide.annotator.HaxeProblemReporter;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.fixer.*;
import static com.intellij.plugins.haxe.ide.annotator.semantics.AnnotatorUtil.hasMacroForCodeGeneration;
import static com.intellij.plugins.haxe.lang.psi.HaxePsiModifier.*;
import com.intellij.plugins.haxe.ide.inspections.HaxeInspection;

/** override modifier correctness: required, forbidden, visibility and shadowing rules. */
public class HaxeMethodOverrideInspection extends HaxeInspection {

  private static final String[] OVERRIDE_FORBIDDEN_MODIFIERS = {FINAL, INLINE, STATIC};

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return checkVisitor(holder, HaxeMethod.class, HaxeMethodOverrideInspection::checkOverride);
  }

  public static void checkOverride(final HaxeMethod methodPsi, final HaxeProblemReporter reporter) {
    final HaxeMethodModel currentMethod = methodPsi.getModel();
    final HaxeClassModel currentClass = currentMethod.getDeclaringClass();
    final HaxeModifiersModel currentModifiers = currentMethod.getModifiers();

    final HaxeMethodModel parentMethod = currentClass != null ? currentClass.getAncestorMethod(currentMethod.getName(), null) : null;
    final HaxeClassModel parentClass = parentMethod != null ? parentMethod.getDeclaringClass() : null;
    final HaxeModifiersModel parentModifiers = (parentMethod != null) ? parentMethod.getModifiers() : null;

    // ignore local functions
    if (methodPsi instanceof HaxeLocalFunctionDeclaration) return;

    boolean requiredOverride = false;

    if (currentMethod.isConstructor() || currentMethod.isStaticInit()) {
      // their static-modifier errors are language errors in HaxeMethodAnnotator
      return;
    }
    else if (parentMethod != null) {
      if (parentMethod.isStatic()) {
        reporter.problem(HighlightSeverity.WEAK_WARNING,
                         HaxeBundle.message("haxe.semantic.method.shadows.static", currentMethod.getName()))
          .range(currentMethod.getNameOrBasePsi())
          .create();
      }
      else {
        if (!currentClass.isInterface()
            && !currentClass.isAnonymous()
            && !parentMethod.isAbstract()
            && !parentClass.isInterface()) {
          requiredOverride = true;
        }

        if (parentModifiers.hasAnyModifier(OVERRIDE_FORBIDDEN_MODIFIERS) && !parentClass.isInterface()) {
          HaxeProblemReporter.Problem builder =
            reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.cannot.override.static.inline.final"))
              .range(currentMethod.getNameOrBasePsi());

          for (String modifier : OVERRIDE_FORBIDDEN_MODIFIERS) {
            if (parentModifiers.hasModifier(modifier)) {
              String fixLabel = HaxeBundle.message("haxe.quickfix.remove.modifier.from", modifier, parentMethod.getFullName());
              builder.withFix(new HaxeModifierRemoveFixer(parentModifiers, modifier, fixLabel));
            }
          }
          builder.create();
        }
        // ignore if empty (override inherits from parent)
        if(!currentModifiers.getVisibility().equals(EMPTY)) {
          if (HaxePsiModifier.hasLowerVisibilityThan(currentModifiers.getVisibility(), parentModifiers.getVisibility())) {
            HaxeModifierReplaceVisibilityFixer changeCurrentVisibilityFix = new HaxeModifierReplaceVisibilityFixer(
              currentModifiers, parentModifiers.getVisibility(),
              HaxeBundle.message("haxe.quickfix.change.method.visibility.current", parentModifiers.getVisibility()));
            HaxeModifierReplaceVisibilityFixer changeParentVisibilityFix = new HaxeModifierReplaceVisibilityFixer(
              parentModifiers, currentModifiers.getVisibility(),
              HaxeBundle.message("haxe.quickfix.change.method.visibility.parent", currentModifiers.getVisibility()));
            String message = HaxeBundle.message("haxe.semantic.method.visibility.lower.than.parent", currentMethod.getName());
            reporter.problem(HighlightSeverity.ERROR, message)
                    .range(currentMethod.getNameOrBasePsi())
                    .withFix(changeCurrentVisibilityFix)
                    .withFix(changeParentVisibilityFix)
                    .create();
          }
        }else {
          if (HaxePsiModifier.hasLowerVisibilityThan(currentModifiers.getVisibility(), parentModifiers.getVisibility())) {
            HaxeModifierReplaceVisibilityFixer changeCurrentVisibilityFix = new HaxeModifierReplaceVisibilityFixer(
              currentModifiers, parentModifiers.getVisibility(),
              HaxeBundle.message("haxe.quickfix.add.method.visibility.current", parentModifiers.getVisibility()));
            HaxeModifierReplaceVisibilityFixer changeParentVisibilityFix = new HaxeModifierReplaceVisibilityFixer(
              parentModifiers, currentModifiers.getVisibility(),
              HaxeBundle.message("haxe.quickfix.add.method.visibility.parent", currentModifiers.getVisibility()));
            String message = HaxeBundle.message("haxe.semantic.method.visibility.missing.overrides.parent",
                                                currentMethod.getName(), parentModifiers.getVisibility());
            reporter.problem(HighlightSeverity.WEAK_WARNING, message)
                    .range(currentMethod.getNameOrBasePsi())
                    .withFix(changeCurrentVisibilityFix)
                    .withFix(changeParentVisibilityFix)
                    .create();
          }
        }
      }
    }

    if (currentModifiers.hasModifier(OVERRIDE) && !requiredOverride) {
      if (!hasMacroForCodeGeneration(currentMethod.getDeclaringClass())) {
        reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.overriding.nothing"))
          .range(currentModifiers.getModifierPsi(OVERRIDE))
          .withFix(new HaxeModifierRemoveFixer(currentModifiers, OVERRIDE))
          .create();
      }
    }
    else if (requiredOverride) {
      if (!currentModifiers.hasModifier(OVERRIDE)) {
        if (hasMacroForCodeGeneration(currentMethod.getDeclaringClass())) {
          reporter.problem(HighlightSeverity.WEAK_WARNING, HaxeBundle.message("haxe.semantic.positionally.missing.override"))
            .range(currentMethod.getNameOrBasePsi())
            .withFix(new HaxeModifierAddFixer(currentModifiers, OVERRIDE))
            .create();
        } else {
          reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.must.override"))
            .range(currentMethod.getNameOrBasePsi())
            .withFix(new HaxeModifierAddFixer(currentModifiers, OVERRIDE))
            .create();
        }
      }

      // the signature itself is HaxeMethodSignatureCompatibilityInspection's job
    }
  }

}
