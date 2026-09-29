package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.lang.psi.HaxeFile;
import com.intellij.plugins.haxe.lang.psi.HaxePackageStatement;
import com.intellij.plugins.haxe.model.StripSpaces;
import com.intellij.plugins.haxe.model.fixer.HaxeRemoveElementFixer;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * The multiple-package-statements language error. Package NAMING conventions
 * are {@code HaxePackageNameInspection}.
 */
public class HaxePackageAnnotator implements Annotator {
  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (AnnotatorUtil.shouldSkip(element)) return;

    if (element instanceof HaxePackageStatement packageStatement) {
      check(packageStatement, holder);
    }
  }

  static void check(final HaxePackageStatement element, final AnnotationHolder holder) {
    HaxeFile file = (HaxeFile)element.getContainingFile();
    if (element != file.getPackageStatement()) {  // If it's not the first one...
      holder.newAnnotation(HighlightSeverity.ERROR, "Multiple package names are not allowed.")
        .range(element)
        .withFix(new HaxeRemoveElementFixer("Remove extra package declaration", element, StripSpaces.BOTH))
        .create();
    }
  }
}
