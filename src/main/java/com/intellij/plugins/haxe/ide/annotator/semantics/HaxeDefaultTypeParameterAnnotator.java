package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.annotator.HaxeStandardAnnotation;
import com.intellij.plugins.haxe.lang.lexer.HaxeTokenTypes;
import com.intellij.plugins.haxe.lang.psi.HaxeGenericDefaultType;
import com.intellij.plugins.haxe.lang.psi.HaxeGenericListPart;
import com.intellij.plugins.haxe.lang.psi.HaxeMethodDeclaration;
import com.intellij.plugins.haxe.model.fixer.HaxeFixer;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevel;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevelUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

public class HaxeDefaultTypeParameterAnnotator implements Annotator {
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (AnnotatorUtil.shouldSkip(element)) return;

    if (element instanceof HaxeGenericListPart genericListPart) {
      check(genericListPart, holder);
    }
  }

  public static void check(HaxeGenericListPart psi, AnnotationHolder holder) {
    HaxeGenericDefaultType type = psi.getGenericDefaultType();
    if (type == null) return;

    // default type parameters exist only at 4.3+; below that ANY default is the
    // error, and the 4.3-semantics check (only on types) does not apply
    if (!HaxeLanguageLevelUtil.isAtLeast(psi, HaxeLanguageLevel.HAXE_4_3)) {
      PsiElement equalsToken = PsiTreeUtil.findSiblingBackward(type, HaxeTokenTypes.OASSIGN, null);
      HaxeStandardAnnotation.requiresLanguageLevel(holder, type, HaxeLanguageLevel.HAXE_4_3,
                                                   HaxeBundle.message("haxe.feature.default.type.parameters"))
        .withFix(HaxeFixer.create(HaxeBundle.message("haxe.quickfix.remove.default.type"),
                                  () -> psi.deleteChildRange(equalsToken, type)))
        .create();
      return;
    }

    PsiElement parent1 = psi.getParent();
    PsiElement parent2 = parent1.getParent();

    if (parent2 instanceof HaxeMethodDeclaration) {
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
