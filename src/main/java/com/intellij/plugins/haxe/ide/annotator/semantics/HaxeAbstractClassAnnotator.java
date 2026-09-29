package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.lang.psi.HaxePsiModifier;
import com.intellij.plugins.haxe.ide.annotator.HaxeStandardAnnotation;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.HaxeModifiersModel;
import com.intellij.plugins.haxe.model.fixer.HaxeModifierAddFixer;
import com.intellij.plugins.haxe.model.fixer.HaxeModifierRemoveFixer;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevel;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevelUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HaxeAbstractClassAnnotator implements Annotator {
  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (AnnotatorUtil.shouldSkip(element)) return;

    if (!(element instanceof HaxeClass) && !(element instanceof HaxeMethod)) return;
    // abstract classes exist only at 4.2+; below that the abstract MODIFIER is
    // the error, and the 4.2-semantics checks do not apply
    if (!HaxeLanguageLevelUtil.isAtLeast(element, HaxeLanguageLevel.HAXE_4_2)) {
      annotateAbstractModifierRequiresLevel(element, holder);
      return;
    }

    if (element instanceof HaxeClass haxeClass) {
      checkClass(haxeClass, holder);
    }
    if (element instanceof HaxeMethod haxeMethod) {
      checkMethod(haxeMethod, holder);
    }
  }

  private static void annotateAbstractModifierRequiresLevel(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    HaxeModifiersModel modifiers = null;
    if (element instanceof HaxeClass haxeClass && haxeClass.getModel().isAbstractClass()) {
      modifiers = haxeClass.getModel().getModifiers();
    }
    else if (element instanceof HaxeMethod haxeMethod
             && haxeMethod.getModel() != null && haxeMethod.getModel().isAbstract()) {
      modifiers = haxeMethod.getModel().getModifiers();
    }
    if (modifiers == null) return;

    PsiElement modifierPsi = modifiers.getModifierPsi(HaxePsiModifier.ABSTRACT);
    if (modifierPsi == null) return;
    HaxeStandardAnnotation.requiresLanguageLevel(holder, modifierPsi, HaxeLanguageLevel.HAXE_4_2,
                                                 HaxeBundle.message("haxe.feature.abstract.class"))
      .withFix(new HaxeModifierRemoveFixer(modifiers, HaxePsiModifier.ABSTRACT))
      .create();
  }

  private void checkClass(@NotNull HaxeClass aClass, @NotNull AnnotationHolder holder) {
    HaxeClassModel classModel = aClass.getModel();
    boolean isAbstractClass = classModel.isAbstractClass();
    ;

    if (isAbstractClass && classModel.isFinal()) {
      PsiElement element = classModel.getNamePsi();
      if (element == null) classModel.getBasePsi();
      String message = HaxeBundle.message("haxe.semantic.abstract.class.cannot.be.final");
      holder.newAnnotation(HighlightSeverity.ERROR, message)
        .withFix(new HaxeModifierRemoveFixer(classModel.getModifiers(), HaxePsiModifier.ABSTRACT))
        .withFix(new HaxeModifierRemoveFixer(classModel.getModifiers(), HaxePsiModifier.FINAL))
        .range(element)
        .create();
    }


    List<HaxeMethodModel> currentClassMethods = classModel.getMethodsSelf(null);
    boolean containsAbstractMethod = currentClassMethods.stream().anyMatch(HaxeMethodModel::isAbstract);

    if (containsAbstractMethod && !isAbstractClass) {
      PsiElement element = classModel.getNamePsi();
      if (element == null) classModel.getBasePsi();
      String message = HaxeBundle.message("haxe.semantic.class.contains.abstract.members");
      holder.newAnnotation(HighlightSeverity.ERROR, message)
        .withFix(new HaxeModifierAddFixer(classModel.getModifiers(), HaxePsiModifier.ABSTRACT))
        .range(element)
        .create();
    }
  }

  private void checkMethod(@NotNull HaxeMethod method, @NotNull AnnotationHolder holder) {
    HaxeMethodModel methodModel = method.getModel();
    if (methodModel != null) {
      if (methodModel.isAbstract()) {
        if (methodModel.getReturnTypeTagPsi() == null) {
          String message = HaxeBundle.message("haxe.semantic.type.required.for.abstract.functions");
          holder.newAnnotation(HighlightSeverity.ERROR, message)
            .range(methodModel.getBasePsi())
            .create();
        }
        if (methodModel.getBodyPsi() != null) {
          String message = HaxeBundle.message("haxe.semantic.abstract.method.cannot.have.expression");
          holder.newAnnotation(HighlightSeverity.ERROR, message)
            .withFix(new HaxeModifierRemoveFixer(methodModel.getModifiers(), HaxePsiModifier.ABSTRACT))
            .range(methodModel.getBodyPsi())
            .create();
        }
      }
    }
  }
}
