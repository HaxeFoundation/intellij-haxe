package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.HaxeBundle;
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
      String popupText = HaxeBundle.message("haxe.quickfix.remove.default.type");
      String errorMessage = HaxeBundle.message("haxe.semantic.default.type.parameters.only.on.types");
      holder.newAnnotation(HighlightSeverity.ERROR, errorMessage)
        .range(type)
        .withFix(HaxeFixer.create(popupText, () -> psi.deleteChildRange(equalsToken, type)))
        .create();
    }
  }
}
