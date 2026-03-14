package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.HaxeGenericDefaultType;
import com.intellij.plugins.haxe.lang.psi.HaxeGenericListPart;
import com.intellij.plugins.haxe.lang.psi.HaxeMethodDeclaration;
import com.intellij.plugins.haxe.model.fixer.HaxeFixer;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

public class HaxeDefaultTypeParameterAnnotator implements Annotator {
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if(!element.isValid()) return;

    if (element instanceof HaxeGenericListPart genericListPart) {
      check(genericListPart, holder);
    }
  }

  public static void check(HaxeGenericListPart psi, AnnotationHolder holder) {

    HaxeGenericDefaultType type = psi.getGenericDefaultType();
    PsiElement parent1 = psi.getParent();
    PsiElement parent2 = parent1.getParent();

    if (type != null && parent2 instanceof HaxeMethodDeclaration) {
      PsiElement equalsToken = PsiTreeUtil.findSiblingBackward(type, HaxeTokenTypes.OASSIGN, null);
    //TODO extract text to bundle
      holder.newAnnotation(HighlightSeverity.ERROR, "Default type parameters are only supported on types")
        .range(type)
        .withFix(new HaxeFixer("Remove default type") {
          @Override
          public void run() {
            psi.deleteChildRange(equalsToken, type);
          }
        })
        .create();
    }
  }
}
