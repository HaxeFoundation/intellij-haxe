package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.lang.annotation.*;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.evaluator.assign.HaxeFunctionCompatible;
import com.intellij.plugins.haxe.model.evaluator.assign.HaxeOverrideOrImplementEvaluation;
import com.intellij.plugins.haxe.model.fixer.HaxeModifierAddFixer;
import com.intellij.plugins.haxe.model.fixer.HaxeModifierRemoveFixer;
import com.intellij.plugins.haxe.model.fixer.HaxeModifierReplaceVisibilityFixer;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.plugins.haxe.util.HaxeResolveUtil;
import com.intellij.psi.PsiElement;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;

import static com.intellij.plugins.haxe.ide.annotator.HaxeSemanticAnnotatorInspections.*;
import static com.intellij.plugins.haxe.ide.annotator.semantics.AnnotatorUtil.hasMacroForCodeGeneration;
import static com.intellij.plugins.haxe.lang.psi.HaxePsiModifier.*;
import static com.intellij.plugins.haxe.lang.psi.HaxePsiModifier.OVERRIDE;
import static com.intellij.plugins.haxe.model.evaluator.assign.HaxeTypeCompatible.canAssignToFromReference;

@CustomLog
public class HaxeMethodAnnotator implements Annotator {

  public static final String DEFAULT_ARG_NAME = "_";

  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
      if (element instanceof HaxeMethod haxeMethod) {
        check(haxeMethod, holder);
      }
  }
  static public void check(final HaxeMethod methodPsi, final AnnotationHolder holder) {
    final HaxeMethodModel currentMethod = methodPsi.getModel();
    checkTypeTagInInterfacesAndExternClass(currentMethod, holder);
    checkMethodArguments(currentMethod, holder);
    checkOverride(methodPsi, holder);
  }

  private static void checkTypeTagInInterfacesAndExternClass(final HaxeMethodModel currentMethod, final AnnotationHolder holder) {
    if (!MISSING_TYPE_TAG_ON_EXTERN_AND_INTERFACE.isEnabled(currentMethod.getBasePsi())) return;

    HaxeClassModel currentClass = currentMethod.getDeclaringClass();
    if (currentClass != null) { //make sure it's not a module method
      if (currentClass.isExtern() || currentClass.isInterface()) {
        if (currentMethod.getReturnTypeTagPsi() == null && !currentMethod.isConstructor()) {
          holder.newAnnotation(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.type.required"))
            .range(currentMethod.getNameOrBasePsi())
            .create();
        }
        for (final HaxeParameterModel param : currentMethod.getParameters()) {
          if (param.getTypeTagPsi() == null) {
            holder.newAnnotation(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.type.required"))
              .range(param.getBasePsi())
              .create();
          }
        }
      }
    }
  }

  private static void checkMethodArguments(final HaxeMethodModel currentMethod, final AnnotationHolder holder) {
    PsiElement methodPsi = currentMethod.getBasePsi();
    boolean checkParameterInitializers = PARAMETER_INITIALIZER_TYPES.isEnabled(methodPsi);
    boolean checkRepeatedParameterName = REPEATED_PARAMETER_NAME_CHECK.isEnabled(methodPsi);

    if (!checkParameterInitializers
        && !checkRepeatedParameterName) {
      return;
    }

    HashMap<String, PsiElement> argumentNames = new HashMap<String, PsiElement>();
    for (final HaxeParameterModel param : currentMethod.getParameters()) {
      String paramName = param.getName();



      if (checkParameterInitializers) {
        if (param.getVarInitPsi() != null && param.getTypeTagPsi() != null) {
          HaxeSemanticsUtil.TypeTagChecker.check(
            param.getBasePsi(),
            param.getTypeTagPsi(),
            param.getVarInitPsi(),
            true,
            holder
          );
        }
      }

      if (checkRepeatedParameterName) {
        if (argumentNames.containsKey(paramName) && !paramName.equals(DEFAULT_ARG_NAME)) {
          // @TODO: Move to bundle
          holder.newAnnotation(HighlightSeverity.WARNING,"Repeated argument name '" + paramName + "'").range(param.getNamePsi()).create();
          holder.newAnnotation(HighlightSeverity.WARNING, "Repeated argument name '" + paramName + "'").range(argumentNames.get(paramName)).create();
        }
        else {
          argumentNames.put(paramName, param.getNamePsi());
        }
      }
    }
  }

  private static final String[] OVERRIDE_FORBIDDEN_MODIFIERS = {FINAL, INLINE, STATIC};

  private static void checkOverride(final HaxeMethod methodPsi, final AnnotationHolder holder) {
    final HaxeMethodModel currentMethod = methodPsi.getModel();
    final HaxeClassModel currentClass = currentMethod.getDeclaringClass();
    final HaxeModifiersModel currentModifiers = currentMethod.getModifiers();

    final HaxeMethodModel parentMethod = currentClass != null ? currentClass.getAncestorMethod(currentMethod.getName(), null) : null;
    final HaxeClassModel parentClass = parentMethod != null ? parentMethod.getDeclaringClass() : null;
    final HaxeModifiersModel parentModifiers = (parentMethod != null) ? parentMethod.getModifiers() : null;

    // ignore local functions
    if (methodPsi instanceof HaxeLocalFunctionDeclaration) return;

    if (!METHOD_OVERRIDE_CHECK.isEnabled(methodPsi)) { // TODO: This check is not granular enough.
      // If the rest of the checks are disabled, we don't want to inhibit the signature check.
      if (null != parentMethod) {
        checkMethodsSignatureCompatibility(currentMethod, parentMethod, holder, true);
      }
      return;
    }

    boolean requiredOverride = false;

    if (currentMethod.isConstructor()) {
      if (currentModifiers.hasModifier(STATIC)) {
        // @TODO: Move to bundle
        holder.newAnnotation(HighlightSeverity.ERROR, "Constructor can't be static").range(currentMethod.getNameOrBasePsi())
        .withFix(
          new HaxeModifierRemoveFixer(currentModifiers, STATIC)
        )
          .create();
      }
    }
    else if (currentMethod.isStaticInit()) {
      if (!currentModifiers.hasModifier(STATIC)) {
        holder.newAnnotation(HighlightSeverity.ERROR, "__init__ must be static").range(currentMethod.getNameOrBasePsi())
        .withFix(
          new HaxeModifierAddFixer(currentModifiers, STATIC)
        )
          .create();
      }
    }
    else if (parentMethod != null) {
      if (parentMethod.isStatic()) {
        holder.newAnnotation(HighlightSeverity.WARNING, "Method '" + currentMethod.getName()
                                                        + "' overrides a static method of a superclass")
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
          AnnotationBuilder builder = holder.newAnnotation(HighlightSeverity.ERROR, "Can't override static, inline or final methods")
           .range(currentMethod.getNameOrBasePsi());

          for (String modifier : OVERRIDE_FORBIDDEN_MODIFIERS) {
            if (parentModifiers.hasModifier(modifier)) {
              builder.withFix(
                new HaxeModifierRemoveFixer(parentModifiers, modifier, "Remove " + modifier + " from " + parentMethod.getFullName())
              );
            }
          }
          builder.create();
        }
        // ignore if empty (override inherits from parent)
        if(!currentModifiers.getVisibility().equals(EMPTY)) {
          if (HaxePsiModifier.hasLowerVisibilityThan(currentModifiers.getVisibility(), parentModifiers.getVisibility())) {
            HaxeModifierReplaceVisibilityFixer changeCurrentVisibilityFix = new HaxeModifierReplaceVisibilityFixer(currentModifiers, parentModifiers.getVisibility(), "Change current method visibility to '"+parentModifiers.getVisibility()+"'");
            HaxeModifierReplaceVisibilityFixer changeParentVisibilityFix = new HaxeModifierReplaceVisibilityFixer(parentModifiers, currentModifiers.getVisibility(), "Change parent method visibility '"+currentModifiers.getVisibility()+"'");
            holder.newAnnotation(HighlightSeverity.ERROR, "Field " +
                                                            currentMethod.getName() +
                                                            " has less visibility (public/private) than superclass one.")
                    .range(currentMethod.getNameOrBasePsi())
                    .withFix(changeCurrentVisibilityFix)
                    .withFix(changeParentVisibilityFix)
                    .create();
          }
        }else {
          if (HaxePsiModifier.hasLowerVisibilityThan(currentModifiers.getVisibility(), parentModifiers.getVisibility())) {
            HaxeModifierReplaceVisibilityFixer changeCurrentVisibilityFix = new HaxeModifierReplaceVisibilityFixer(currentModifiers, parentModifiers.getVisibility(), "Add current method visibility to '"+parentModifiers.getVisibility()+"'");
            HaxeModifierReplaceVisibilityFixer changeParentVisibilityFix = new HaxeModifierReplaceVisibilityFixer(parentModifiers, currentModifiers.getVisibility(), "Add parent method visibility '"+currentModifiers.getVisibility()+"'");
            holder.newAnnotation(HighlightSeverity.WEAK_WARNING, "Field " +
                                                                 currentMethod.getName() +
                                                                 " has no visibility modifier but overrides parent with '" +
                                                                 parentModifiers.getVisibility() + "'")
                    .range(currentMethod.getNameOrBasePsi())
                    .withFix(changeCurrentVisibilityFix)
                    .withFix(changeParentVisibilityFix)
                    .create();
          }
        }
      }
    }

    //System.out.println(aClass);
    if (currentModifiers.hasModifier(OVERRIDE) && !requiredOverride) {
      if (!hasMacroForCodeGeneration(currentMethod.getDeclaringClass())) {
        holder.newAnnotation(HighlightSeverity.ERROR, "Overriding nothing").range(currentModifiers.getModifierPsi(OVERRIDE))
          .withFix(new HaxeModifierRemoveFixer(currentModifiers, OVERRIDE))
          .create();
      }
    }
    else if (requiredOverride) {
      if (!currentModifiers.hasModifier(OVERRIDE)) {
        if (hasMacroForCodeGeneration(currentMethod.getDeclaringClass())) {
          holder.newAnnotation(HighlightSeverity.WEAK_WARNING, "Positionally missing override")
            .range(currentMethod.getNameOrBasePsi())
            .withFix(new HaxeModifierAddFixer(currentModifiers, OVERRIDE))
            .create();
        } else {
          holder.newAnnotation(HighlightSeverity.ERROR, "Must override").range(currentMethod.getNameOrBasePsi())
            .withFix(new HaxeModifierAddFixer(currentModifiers, OVERRIDE))
            .create();
        }
      }

      else {
        // It is rightly overriden. Now check the signature.
//        checkMethodsSignatureCompatibility(currentMethod, parentMethod, holder);
        checkMethodsSignatureCompatibility(currentMethod, parentMethod, holder, true);
      }
    }
  }

  static boolean checkMethodsSignatureCompatibility(
    @NotNull final HaxeMethodModel currentMethod,
    @NotNull final HaxeMethodModel parentMethod ) {
    return checkMethodsSignatureCompatibility(currentMethod,parentMethod, null, false);
  }

  static boolean checkMethodsSignatureCompatibility(
    @NotNull final HaxeMethodModel currentMethod,
    @NotNull final HaxeMethodModel parentMethod,
    final AnnotationHolder holder,
    boolean shouldAnnotate) {

    if (!METHOD_SIGNATURE_COMPATIBILITY.isEnabled(currentMethod.getBasePsi())) return true;

    if (parentMethod.isInInterface() && !currentMethod.isInInterface()) {
      HaxeOverrideOrImplementEvaluation implementCheck = HaxeFunctionCompatible.checkThisImplementsThat(currentMethod,parentMethod, shouldAnnotate);
      implementCheck.annotate(holder);
      return implementCheck.result;

    } else {
      HaxeOverrideOrImplementEvaluation overrideCheck = HaxeFunctionCompatible.checkThisOverrideThat( currentMethod,parentMethod, shouldAnnotate);
      overrideCheck.annotate(holder);
      return overrideCheck.result;
    }
  }
}