package com.intellij.plugins.haxe.ide.annotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.lang.psi.HaxeReference;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeReferenceAnnotator implements Annotator {
  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (!(element instanceof HaxeReference)) {
      return;
    }
    final HaxeReference reference = (HaxeReference)element;
    final boolean chain = PsiTreeUtil.getChildOfType(reference, HaxeReference.class) != null;
    if (!chain && reference.resolve() == null) {
      holder.createWeakWarningAnnotation(element, HaxeBundle.message("cannot.resolve.reference"));
    }
  }
}
