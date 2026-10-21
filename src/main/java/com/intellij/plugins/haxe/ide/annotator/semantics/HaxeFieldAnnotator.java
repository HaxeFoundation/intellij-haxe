package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeFieldDeclaration;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeFieldModel;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * The field-level LANGUAGE error (mirroring the compiler): a variable needs
 * an initializer or a type hint. Everything togglable lives in the
 * field/property inspections.
 */
public class HaxeFieldAnnotator implements Annotator {
  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (AnnotatorUtil.shouldSkip(element)) return;

    if (element instanceof HaxeFieldDeclaration field) {
      check(field, holder);
    }
  }

  private static void check(final HaxeFieldDeclaration var, final AnnotationHolder holder) {
    HaxeFieldModel field = (HaxeFieldModel)var.getModel();
    if (!field.hasInitializer() && !field.hasTypeTag()) {
      HaxeClassModel declaringClass = field.getDeclaringClass();
      //NOTE: abstract enums can have finals without init or typeHint
      if(declaringClass != null  && !declaringClass.isEnum()) {
        holder.newAnnotation(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.var.init.or.type.hint", field.getName()))
                .range(var)
                .create();
      }
    }
  }
}
