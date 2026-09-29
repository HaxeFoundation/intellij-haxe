package com.intellij.plugins.haxe.ide.inspections.operators;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.options.OptPane;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.annotator.HaxeProblemReporter;
import com.intellij.plugins.haxe.ide.annotator.semantics.AnnotatorUtil;
import com.intellij.plugins.haxe.lang.psi.HaxeIsTypeExpression;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

import static com.intellij.codeInspection.options.OptPane.checkbox;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.fixer.HaxeSurroundFixer;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevel;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevelUtil;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import com.intellij.plugins.haxe.ide.inspections.HaxeInspection;

/**
 * `is` operator correctness. The pre-4.2 parenthesization restrictions apply
 * automatically below language level 4.2; the option keeps them on at newer
 * levels for code that must stay 4.1-compatible.
 */
public class HaxeIsTypeExpressionInspection extends HaxeInspection {

  @SuppressWarnings("WeakerAccess") // made public for options serialization
  public boolean enforce41Semantics = false;

  @Override
  public @NotNull OptPane getOptionsPane() {
    return OptPane.pane(
      checkbox("enforce41Semantics", HaxeBundle.message("haxe.inspections.is.type.expression.inspection.4dot1.compatible.name")));
  }

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    HaxeProblemReporter reporter = HaxeProblemReporter.of(holder, getDefaultLevel().getSeverity());
    return new PsiElementVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement element) {
        if (AnnotatorUtil.shouldSkip(element)) return;
        if (element instanceof HaxeIsTypeExpression expr) {
          check(expr, reporter, enforce41Semantics);
        }
      }
    };
  }


  public static void check(@NotNull HaxeIsTypeExpression expr, @NotNull HaxeProblemReporter reporter,
                           boolean enforce41Semantics) {
    PsiElement rhsType = getRightHandType(expr);
    if (null != rhsType) {
      annotateTypeError(rhsType, reporter);
    }
    else {
      PsiElement rhs = getRightHandElement(expr);
      reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.is.operator.rhs.must.be.type"))
        .range(null != rhs ? rhs : expr)
        .create();
    }

    boolean pre42Semantics = !HaxeLanguageLevelUtil.isAtLeast(expr, HaxeLanguageLevel.HAXE_4_2);
    if (pre42Semantics || enforce41Semantics) {

      PsiElement lhs = expr.getLeftExpression();
      if (isComplexExpression(lhs)) {
        reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.is.operator.lhs.cannot.be.complex.expression"))
          .range(lhs)
          .withFix(wrapLhsFixer(lhs))
          .withFix(wrapInnerIsFixer(expr))
          .create();
      }

      PsiElement parent = expr.getParent();
      if (parent instanceof HaxeAssignExpression) {
        // "a = b is expression" parses (in this plugin's parser) as "a = (b is expression)", so the parent is actually the assignment.
        TextRange assignMarkerRange = new TextRange(parent.getTextOffset(), lhs.getTextRange().getEndOffset());

        reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.is.operator.4_1.lhs.cannot.be.assignment"))
          .range(expr)
          .withFix(
            HaxeSurroundFixer.withParens(HaxeBundle.message("haxe.quickfix.wrap.assignment.with.parenthesis"), expr, assignMarkerRange))
          .withFix(wrapExpressionFixer(expr))
          .create();
      }
      else if (parent instanceof HaxeVarInit) {
        HaxeExpression initExpression = ((HaxeVarInit)parent).getExpression();
          reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.is.operator.4_1.lhs.cannot.be.var.init"))
            .range(expr)
            .withFix(wrapExpressionFixer(initExpression))
            .withFix(wrapInnerIsFixer(expr))
            .create();

      }
      else if (parent instanceof HaxeBinaryExpression
               || parent instanceof HaxeSwitchStatement
               || parent instanceof HaxeExpressionList
               || parent instanceof HaxeMapInitializerExpressionList
               || parent instanceof HaxeTernaryExpression
               || parent instanceof HaxeReturnStatement
               || parent instanceof HaxeDoWhileBody
               || parent instanceof HaxeTryStatement
               || parent instanceof HaxeFunctionLiteral
               || parent instanceof HaxeForStatement
               || parent instanceof HaxeMapInitializerExpression
               || parent instanceof HaxeBlockStatement
               || parent instanceof HaxeThrowStatement
               || parent instanceof HaxeUntypedStatement
               || parent instanceof HaxeMacroStatement
      ) {
        annotateIs(reporter, expr, HaxeBundle.message("haxe.semantic.unparenthesized.is.expression.cannot.be.used.here.pre.4.2.semantics"));
      }
      else if (parent instanceof HaxeGuard
               || parent instanceof HaxeWhileStatement) {
        annotateIs(reporter, expr,
                   HaxeBundle.message(
                     "haxe.semantic.is.expression.requires.double.parenthesis.when.used.as.a.guard.expression.pre.4.2.semantics"));
      }
    }
  }

  private static void annotateIs(HaxeProblemReporter reporter, HaxeIsTypeExpression expr, String message) {
    if (null == message) {
      message = HaxeBundle.message("haxe.semantic.unparenthesized.is.expression.cannot.be.used.here");
    }
    reporter.problem(HighlightSeverity.ERROR, message)
      .range(expr)
      .withFix(wrapInnerIsFixer(expr))
      .create();
  }

  @NotNull
  private static HaxeSurroundFixer wrapInnerIsFixer(@NotNull HaxeIsTypeExpression expr) {
    PsiElement lhs = expr.getLeftExpression();
    if (lhs instanceof HaxeBinaryExpression) {
      PsiElement lhsrhs = ((HaxeBinaryExpression)lhs).getRightExpression();
      if (null != lhsrhs) {
        PsiElement rhs = getRightHandElement(expr);
        if (null == rhs) rhs = UsefulPsiTreeUtil.getLastChild(expr, HaxePsiCompositeElement.class);
        if (null != rhs) {
          return wrapIsFixer(lhsrhs, rhs);
        }
      }
    }
    return wrapIsFixer(expr);
  }

  @NotNull
  private static HaxeSurroundFixer wrapIsFixer(@NotNull HaxeIsTypeExpression expr) {
    return HaxeSurroundFixer.withParens(HaxeBundle.message("haxe.quickfix.wrap.is.expression.with.parenthesis"), expr, expr.getTextRange());
  }

  @NotNull
  private static HaxeSurroundFixer wrapIsFixer(@NotNull PsiElement first, @NotNull PsiElement last) {
    TextRange range = first.getTextRange().union(last.getTextRange());
    return HaxeSurroundFixer.withParens(HaxeBundle.message("haxe.quickfix.wrap.is.expression.with.parenthesis"), first, range);
  }

  @NotNull
  private static HaxeSurroundFixer wrapExpressionFixer(@NotNull PsiElement expr) {
    return HaxeSurroundFixer.withParens(HaxeBundle.message("haxe.quickfix.wrap.expression.with.parenthesis"), expr, expr.getTextRange());
  }

  @NotNull
  private static HaxeSurroundFixer wrapLhsFixer(@NotNull PsiElement expr) {
    return HaxeSurroundFixer.withParens(HaxeBundle.message("haxe.quickfix.wrap.left.hand.side.with.parenthesis"), expr,
                                        expr.getTextRange());
  }


  @Nullable
  private static PsiElement getRightHandType(HaxeIsTypeExpression expr) {
    PsiElement element = expr.getFunctionType();
    if (null == element) {
      HaxeTypeOrAnonymous toa = expr.getTypeOrAnonymous();
      if (null != toa) {
        element = toa.getAnonymousType();
        if (null == element) element = toa.getType();
      }
    }
    if (null == element) {
      PsiElement rhs = getRightHandElement(expr);
      if (rhs instanceof HaxeObjectLiteral) element = rhs;
    }
    return element;
  }

  @Nullable
  private static PsiElement getRightHandElement(HaxeIsTypeExpression expr) {
    List<HaxeExpression> expressionList = expr.getExpressionList();
    if (expressionList.size() > 1) {
      return expressionList.get(1);
    }
    return null;
  }

  private static void annotateTypeError(PsiElement type, HaxeProblemReporter reporter) {
    if (type instanceof HaxeType haxeType) {
      if (null != haxeType.getTypeParam()) {
        reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.is.operator.type.cannot.have.parameters"))
          .range(type.getTextRange())
          .create();
      }

      HaxeReferenceExpression ref = haxeType.getReferenceExpression();
      PsiElement found = ref.resolve();

      if (found instanceof HaxeLocalVarDeclaration) {
        reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.is.operator.rhs.must.be.type"))
          .range(type.getTextRange())
          .create();
      }
      if (found instanceof HaxeClass haxeClass) {
        HaxeClassModel model = haxeClass.getModel();
        if(model.isAbstractType() && !model.isCoreType()) {
          reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.is.operator.rhs.cannot.be.abstract"))
                  .range(type.getTextRange())
                  .create();
        }
      }
    }
    else {
      reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.is.operator.type.not.supported"))
        .range(type.getTextRange())
        .create();
    }
  }

  private static boolean isComplexExpression(PsiElement expr) {
    return expr instanceof HaxeBinaryExpression
           || expr instanceof HaxeTernaryExpression
           || expr instanceof HaxeIsTypeExpression;
  }

}
