package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.codeInsight.daemon.HighlightDisplayKey;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.InspectionProfile;
import com.intellij.codeInspection.InspectionProfileEntry;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.TextRange;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.annotator.color.HaxeColorAnnotatorUtil;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.fixer.HaxeSurroundFixer;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.profile.codeInspection.InspectionProfileManager;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.intellij.plugins.haxe.ide.annotator.HaxeSemanticAnnotatorInspections.IS_TYPE_INSPECTION;
import static com.intellij.plugins.haxe.ide.annotator.HaxeSemanticAnnotatorInspections.IS_TYPE_INSPECTION_4dot1_COMPATIBLE;
import static com.intellij.plugins.haxe.ide.annotator.color.HaxeColorAnnotatorUtil.colorizeKeyword;

public class HaxeIsTypeExpressionAnnotator implements Annotator, DumbAware {

  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
      if (element instanceof HaxeIsTypeExpression typeExpression) {
        check(typeExpression, holder);
      }
  }

  public void check(final HaxeIsTypeExpression expr, final AnnotationHolder holder) {
    if (!IS_TYPE_INSPECTION.isEnabled(expr)) return;

    PsiElement rhsType = getRightHandType(expr);
    if (null != rhsType) {
      annotateTypeError(rhsType, holder);
    }
    else {
      PsiElement rhs = getRightHandElement(expr);
      holder.newAnnotation(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.is.operator.rhs.must.be.type"))
        .range(null != rhs ? rhs : expr)
        .create();
    }

    if (IS_TYPE_INSPECTION_4dot1_COMPATIBLE.isEnabled(expr)) {

      PsiElement lhs = expr.getLeftExpression();
      if (isComplexExpression(lhs)) {
        holder.newAnnotation(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.is.operator.lhs.cannot.be.complex.expression"))
          .range(lhs)
          .withFix(wrapLhsFixer(lhs))
          .withFix(wrapInnerIsFixer(expr))
          .create();
      }

      PsiElement parent = expr.getParent();
      if (parent instanceof HaxeAssignExpression) {
        // "a = b is expression" parses (in our parser) as "a = (b is expression)", so the parent is actually the assignment.
        TextRange assignMarkerRange = new TextRange(parent.getTextOffset(), lhs.getTextRange().getEndOffset());

        holder.newAnnotation(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.is.operator.4_1.lhs.cannot.be.assignment"))
          .range(expr)
          .withFix(
            HaxeSurroundFixer.withParens(HaxeBundle.message("haxe.quickfix.wrap.assignment.with.parenthesis"), expr, assignMarkerRange))
          .withFix(wrapExpressionFixer(expr))
          .create();
      }
      else if (parent instanceof HaxeVarInit) {
        HaxeExpression initExpression = ((HaxeVarInit)parent).getExpression();
          holder.newAnnotation(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.is.operator.4_1.lhs.cannot.be.var.init"))
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
      ) {
        annotateIs(holder, expr, HaxeBundle.message("haxe.semantic.unparenthesized.is.expression.cannot.be.used.here.pre.4.2.semantics"));
      }
      else if (parent instanceof HaxeGuard
               || parent instanceof HaxeWhileStatement) {
        annotateIs(holder, expr,
                   HaxeBundle.message(
                     "haxe.semantic.is.expression.requires.double.parenthesis.when.used.as.a.guard.expression.pre.4.2.semantics"));
      }
    }

    // Recolor the keyword, because error markings revert the color.
    recolorIsKeyword(holder, expr);
  }

  private static void recolorIsKeyword(AnnotationHolder holder, HaxeIsTypeExpression element) {
    HaxeIsOperator op = element.getOperator();
    colorizeKeyword(holder, op);
  }

  @NotNull
  private static void annotateIs(AnnotationHolder holder, HaxeIsTypeExpression expr, String message) {
    if (null == message) {
      message = HaxeBundle.message("haxe.semantic.unparenthesized.is.expression.cannot.be.used.here");
    }
    holder.newAnnotation(HighlightSeverity.ERROR, message)
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

  private static void annotateTypeError(PsiElement type, AnnotationHolder holder) {
    if (type instanceof HaxeType haxeType) {
      if (null != haxeType.getTypeParam()) {
        holder.newAnnotation(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.is.operator.type.cannot.have.parameters"))
          .range(type.getTextRange())
          .create();
      }

      HaxeReferenceExpression ref = haxeType.getReferenceExpression();
      PsiElement found = ref.resolve();

      if (found instanceof HaxeLocalVarDeclaration) {
        holder.newAnnotation(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.is.operator.rhs.must.be.type"))
          .range(type.getTextRange())
          .create();
      }
    }
    else {
      holder.newAnnotation(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.is.operator.type.not.supported"))
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
