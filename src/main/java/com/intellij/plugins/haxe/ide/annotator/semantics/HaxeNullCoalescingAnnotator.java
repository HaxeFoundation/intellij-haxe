package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.annotator.HaxeStandardAnnotation;
import com.intellij.plugins.haxe.lang.psi.HaxeCoalescingExpression;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevel;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevelUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * The {@code ??} language-level gate: below 4.3 the construct itself is the
 * error. Operand type compatibility is
 * {@code HaxeIncompatibleInitializationInspection}.
 */
public class HaxeNullCoalescingAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
      if (AnnotatorUtil.shouldSkip(element)) return;

        if (element instanceof HaxeCoalescingExpression coalescingExpression) {
            if (!HaxeLanguageLevelUtil.isAtLeast(coalescingExpression, HaxeLanguageLevel.HAXE_4_3)) {
                HaxeStandardAnnotation.requiresLanguageLevel(holder, coalescingExpression, HaxeLanguageLevel.HAXE_4_3,
                                                             HaxeBundle.message("haxe.feature.null.coalescing"))
                  .create();
            }
        }
    }
}
