package com.intellij.plugins.haxe.ide.inspections.typing;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.plugins.haxe.ide.annotator.HaxeProblemReporter;
import com.intellij.plugins.haxe.ide.annotator.semantics.AnnotatorUtil;
import com.intellij.plugins.haxe.lang.psi.HaxeCoalescingExpression;
import com.intellij.plugins.haxe.lang.psi.HaxeFieldDeclaration;
import com.intellij.plugins.haxe.lang.psi.HaxeLocalVarDeclaration;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevel;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevelUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.annotator.semantics.HaxeSemanticsUtil;
import com.intellij.plugins.haxe.ide.annotator.HaxeStandardAnnotation;
import com.intellij.plugins.haxe.lang.psi.HaxeExpression;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.HaxeLocalVarModel;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import static com.intellij.plugins.haxe.ide.annotator.semantics.HaxeSemanticsUtil.TypeTagChecker.getTypeFromVarInit;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.*;
import com.intellij.plugins.haxe.ide.inspections.HaxeInspection;

import java.util.List;

/**
 * Initializer/operand type compatibility at every initialization-shaped site:
 * local variables, fields, and the null-coalescing operator's operands.
 */
public class HaxeIncompatibleInitializationInspection extends HaxeInspection {

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    HaxeProblemReporter reporter = HaxeProblemReporter.of(holder, getDefaultLevel().getSeverity());
    return new PsiElementVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement element) {
        if (AnnotatorUtil.shouldSkip(element)) return;
        if (element instanceof HaxeLocalVarDeclaration var) {
          checkLocalVar(var, reporter);
        }
        else if (element instanceof HaxeFieldDeclaration field) {
          checkInitializerType(field, reporter);
        }
        else if (element instanceof HaxeCoalescingExpression coalescing) {
          // below 4.3 the operator itself is the error (annotator); types are moot
          if (HaxeLanguageLevelUtil.isAtLeast(coalescing, HaxeLanguageLevel.HAXE_4_3)) {
            checkCoalescing(coalescing, reporter);
          }
        }
      }
    };
  }
  public static void checkLocalVar(@NotNull HaxeLocalVarDeclaration var, @NotNull HaxeProblemReporter reporter) {
    HaxeLocalVarModel local = (HaxeLocalVarModel) var.getModel();
    if (local.hasInitializer() && local.hasTypeTag()) {
      HaxeSemanticsUtil.TypeTagChecker.check(local.getBasePsi(), local.getTypeTagPsi(), local.getInitializerPsi(), false, reporter);
    }else if (local.hasInitializer()) {
      ResultHolder init = getTypeFromVarInit(local.getInitializerPsi(), null);
      if (init.isVoid()) {
        reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.void.variables.not.allowed"))
          .range(local.getBasePsi())
          .create();
      }
    }
  }

  /** Operand type compatibility of {@code a ?? b}; the 4.3 level gate stays in HaxeNullCoalescingAnnotator. */
  public static void checkCoalescing(@NotNull HaxeCoalescingExpression coalescingExpression, @NotNull HaxeProblemReporter reporter) {
    List<HaxeExpression> expressionList = coalescingExpression.getExpressionList();
    if (expressionList.size() == 2) {
      HaxeExpression left = expressionList.getFirst();
      HaxeExpression right = expressionList.getLast();

      ResultHolder leftType = HaxeExpressionEvaluator.evaluate(left).result;
      ResultHolder rightType = HaxeExpressionEvaluator.evaluate(right).result;

      if(leftType.isVoid()) {
        reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.cannot.use.void.as.value")).range(left).create();
        return;
      }
      if(rightType.isVoid()) {
        reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.cannot.use.void.as.value")).range(right).create();
        return;
      }

      if (!leftType.canAssign(rightType) && !rightType.canAssign(leftType)) {
        // the haxe compiler marks the right-hand expression as incorrect; so does the inspection
        HaxeStandardAnnotation.typeMismatch(reporter, right, leftType.toPresentationString(), rightType.toPresentationString()).create();
      }
    }
  }

  /** Initializer type against the declared type tag - shared shape with local vars. */
  public static void checkInitializerType(final HaxeFieldDeclaration var, final HaxeProblemReporter reporter) {
    HaxeFieldModel field = (HaxeFieldModel)var.getModel();
    if (field.hasInitializer() && field.hasTypeTag()) {
      HaxeSemanticsUtil.TypeTagChecker.check(field.getBasePsi(), field.getTypeTagPsi(), field.getInitializerPsi(), false, reporter);
    }
  }

}
