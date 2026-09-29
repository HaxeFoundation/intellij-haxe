package com.intellij.plugins.haxe.ide.inspections.style;

import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.plugins.haxe.ide.annotator.HaxeProblemReporter;
import com.intellij.plugins.haxe.ide.annotator.semantics.AnnotatorUtil;
import com.intellij.plugins.haxe.lang.psi.HaxeAnonymousType;
import com.intellij.plugins.haxe.lang.psi.HaxeClass;
import com.intellij.plugins.haxe.lang.psi.HaxeType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.HaxeDocumentModel;
import com.intellij.plugins.haxe.model.fixer.HaxeFixer;
import com.intellij.psi.PsiIdentifier;
import org.jetbrains.annotations.Nullable;
import com.intellij.plugins.haxe.ide.inspections.HaxeInspection;

/** Type-name casing on type references and class declarations. */
public class HaxeInvalidTypeNameInspection extends HaxeInspection {

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    HaxeProblemReporter reporter = HaxeProblemReporter.of(holder, getDefaultLevel().getSeverity());
    return new PsiElementVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement element) {
        if (AnnotatorUtil.shouldSkip(element)) return;
        if (element instanceof HaxeType type) {
          check(type.getReferenceExpression().getIdentifier(), reporter);
        }
        else if (element instanceof HaxeClass clazz && !(element instanceof HaxeAnonymousType)) {
          check(clazz.getModel().getNamePsi(), reporter);
        }
      }
    };
  }


  public static void check(@Nullable PsiIdentifier identifier, HaxeProblemReporter reporter) {
    if (identifier == null) return;

    final String typeName = identifier.getText();
    if (!HaxeClassModel.isValidClassName(typeName)) {
      reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.type.name.must.start.upper.case"))
        .range(identifier)
        .withFix(new HaxeFixer(HaxeBundle.message("haxe.quickfix.change.name")) {
          @Override
          public void run() {
            HaxeDocumentModel.fromElement(identifier).replaceElementText(
              identifier,
              typeName.substring(0, 1).toUpperCase() + typeName.substring(1)
            );
          }
        }).create();
    }
  }

}
