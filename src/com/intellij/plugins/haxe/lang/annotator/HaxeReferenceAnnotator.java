package com.intellij.plugins.haxe.lang.annotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.impl.HaxeReferenceImpl;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeReferenceAnnotator implements Annotator {
  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (!(element instanceof HaxeReferenceImpl)) {
      return;
    }
    final HaxeReferenceImpl reference = (HaxeReferenceImpl)element;

    if (reference.resolve() == null) {
      holder.createWeakWarningAnnotation(element, HaxeBundle.message("cannot.resolve.reference"));
    }
  }
}
