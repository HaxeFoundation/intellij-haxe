package com.intellij.plugins.haxe.ide.inspections.members;

import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.plugins.haxe.ide.annotator.HaxeProblemReporter;
import com.intellij.plugins.haxe.ide.annotator.semantics.AnnotatorUtil;
import com.intellij.plugins.haxe.lang.psi.HaxeFieldDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeReferenceExpression;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.util.HaxeExpressionUtil;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.model.fixer.HaxeFixer;
import com.intellij.psi.util.PsiTreeUtil;
import static com.intellij.plugins.haxe.lang.psi.HaxePsiModifier.IS_VAR_META;
import com.intellij.plugins.haxe.ide.inspections.HaxeInspection;

/**
 * Properties without physical storage: initializers on them, and reads/writes
 * from their own accessors.
 */
public class HaxePropertyIsNotARealVariableInspection extends HaxeInspection {

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    HaxeProblemReporter reporter = HaxeProblemReporter.of(holder, getDefaultLevel().getSeverity());
    return new PsiElementVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement element) {
        if (AnnotatorUtil.shouldSkip(element)) return;
        if (element instanceof HaxeFieldDeclaration field) {
          checkPropertyIsNotRealVariable(field, reporter);
        }
        else if (element instanceof HaxeReferenceExpression reference) {
          checkAccessorFieldAccess(reference, reporter);
        }
      }
    };
  }
  public static void checkPropertyIsNotRealVariable(final HaxeFieldDeclaration var, final HaxeProblemReporter reporter) {
    HaxeFieldModel field = (HaxeFieldModel)var.getModel();
    if (!field.isProperty() || field.isFinal()) return;
    final HaxeDocumentModel document = field.getDocument();
    final HaxeVarInit initializerPsi = field.getInitializerPsi();
    if (!field.isRealVar() && null != initializerPsi) {

      HaxeProblemReporter.Problem builder =
        reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.property.not.real.variable.cannot.initialize"))
          .range(initializerPsi)
          .withFix(removeInitFix(document, initializerPsi))
          .withFix(addIsVarFix(field));

      if (field.getSetterPsi() != null) {
        builder.withFix(makeSetterNullFix(field, document));
      }

      builder.create();
    }
  }

  /** Not-a-real-variable fields must not be read/written from their own accessors. */
  public static void checkAccessorFieldAccess(final HaxeReferenceExpression expression, final HaxeProblemReporter reporter) {
    checkFieldAccessFromGetterSetter(reporter, expression);
  }

  private static void checkFieldAccessFromGetterSetter(@NotNull HaxeProblemReporter reporter, HaxeReferenceExpression expression) {
    if(expression.getParent() instanceof HaxeType) return;
    // chained expressions are skipped - only self references matter
    // and updating other instances should be allowed
    // TODO: this also (incorrectly?) skips this check for `this.property`
    if(expression.getChildren().length > 1) return;
    HaxeMethodDeclaration method = PsiTreeUtil.getParentOfType(expression, HaxeMethodDeclaration.class);
    if(method != null) {
      PsiElement resolve = expression.resolve();
      if(resolve instanceof HaxeFieldDeclaration fieldDeclaration) {
        HaxeFieldModel fieldModel = (HaxeFieldModel)fieldDeclaration.getModel();
        if(fieldModel.isRealVar()) return;
        HaxeMethodModel methodModel = method.getModel();
        boolean inGetterMethod = fieldModel.getGetterMethod() == methodModel;
        boolean inSetterMethod = fieldModel.getSetterMethod() == methodModel;
        boolean isWriteExpression = HaxeExpressionUtil.isInWriteOperation(expression);
        boolean isReadExpression = HaxeExpressionUtil.isInReadOperation(expression);
        if((inGetterMethod && isReadExpression) || (inSetterMethod && isWriteExpression)) {
          reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.property.not.real.variable.cannot.access"))
                  .range(expression)
                  .withFix(addIsVarFix(fieldModel))
                  .create();
        }
      }
    }
  }

  @NotNull
  private static HaxeFixer makeSetterNullFix(HaxeFieldModel field, HaxeDocumentModel document) {
    return new HaxeFixer(HaxeBundle.message("haxe.quickfix.make.setter.null")) {
      @Override
      public void run() {
        document.replaceElementText(field.getSetterPsi(), "null");
      }
    };
  }

  @NotNull
  private static HaxeFixer addIsVarFix(HaxeFieldModel field) {
    return new HaxeFixer(HaxeBundle.message("haxe.quickfix.add.isvar.meta")) {
      @Override
      public void run() {
        field.getModifiers().addModifier(IS_VAR_META);
      }
    };
  }

  @NotNull
  private static HaxeFixer removeInitFix(HaxeDocumentModel document, HaxeVarInit initializerPsi) {
    return new HaxeFixer(HaxeBundle.message("haxe.quickfix.remove.init")) {
      @Override
      public void run() {
        document.replaceElementText(initializerPsi, "", StripSpaces.BEFORE);
      }
    };
  }

}
