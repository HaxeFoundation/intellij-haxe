package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.plugins.haxe.lang.psi.HaxePsiModifier;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.fixer.HaxeModifierAddFixer;
import com.intellij.plugins.haxe.model.fixer.HaxeModifierRemoveFixer;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HaxeAbstractClassAnnotator implements Annotator {
  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (element instanceof HaxeClass haxeClass) {
      checkClass(haxeClass, holder);
    }
    if (element instanceof HaxeMethod haxeMethod) {
      checkMethod(haxeMethod, holder);
    }
  }

  private void checkClass(@NotNull HaxeClass aClass, @NotNull AnnotationHolder holder) {
    HaxeClassModel classModel = aClass.getModel();
    boolean isAbstractClass = classModel.isAbstractClass();
    ;

    if (isAbstractClass && classModel.isFinal()) {
      PsiElement element = classModel.getNamePsi();
      if (element == null) classModel.getBasePsi();
      // TODO move to bundle
      String message = "An abstract class may not be final";
      holder.newAnnotation(HighlightSeverity.ERROR, message)
        .withFix(new HaxeModifierRemoveFixer(classModel.getModifiers(), HaxePsiModifier.ABSTRACT))
        .withFix(new HaxeModifierRemoveFixer(classModel.getModifiers(), HaxePsiModifier.FINAL))
        .range(element)
        .create();
    }


    List<HaxeMethodModel> methods = classModel.getMethods(null);
    boolean containsAbstractMethod = methods.stream().anyMatch(HaxeMethodModel::isAbstract);

    if (containsAbstractMethod && !isAbstractClass) {
      PsiElement element = classModel.getNamePsi();
      if (element == null) classModel.getBasePsi();
      // TODO move to bundle
      String message = "Class contains abstract members";
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
          // TODO move to bundle
          String message = "Type required for abstract functions";
          holder.newAnnotation(HighlightSeverity.ERROR, message)
            .range(methodModel.getBasePsi())
            .create();
        }
        if (methodModel.getBodyPsi() != null) {
          // TODO move to bundle
          String message = "Abstract methods may not have an expression";
          holder.newAnnotation(HighlightSeverity.ERROR, message)
            .withFix(new HaxeModifierRemoveFixer(methodModel.getModifiers(), HaxePsiModifier.ABSTRACT))
            .range(methodModel.getBodyPsi())
            .create();
        }
      }
    }
  }
}
