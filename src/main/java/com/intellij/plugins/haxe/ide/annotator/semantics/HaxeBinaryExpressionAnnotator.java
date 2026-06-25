package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluatorContext;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import static com.intellij.plugins.haxe.model.evaluator.callexpression.EnumValueMatchUtil.isInsidePatternMatcher;
import static com.intellij.plugins.haxe.model.type.HaxeOperatorResolver.ARITHMETIC_OPERATORS;
import static com.intellij.plugins.haxe.model.type.HaxeOperatorResolver.BITWISE_OPERATORS;

public class HaxeBinaryExpressionAnnotator implements Annotator {
  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if(!element.isValid()) return;

    if (element instanceof HaxeAssignExpression) {
      // HaxeAssignExpression -> assign is handle by localVarAnnotator etc.
      //HaxeCompareExpression : problem with Class<myClass> being detected as compare  (X < Y)
      return;
    }
    // TODO make IS operator check (require ,  ref is Type combo)

    if (element instanceof HaxeBinaryExpression expression
        // TODO mlo, make a better check to see if element is part of @:op(...)

        && !(expression.getParent() instanceof HaxeCompiletimeMetaArg )) {
      //  ignore if part of switch case expression
      if (PsiTreeUtil.getParentOfType(element, HaxeSwitchCaseExpr.class) == null) {

        check(expression, holder);
      }
    }
  }

  public static void check(final HaxeBinaryExpression binaryExpression, final AnnotationHolder holder) {

    PsiElement[] children = binaryExpression.getChildren();
    if (children.length == 3) {
      // skip Null Coalescing here, it's handled in "HaxeNullCoalescingAnnotator"
      PsiElement operator = children[1];
      if (operator.textMatches("??")) return;


      PsiElement leftChild = children[0];
      PsiElement rightChild = children[2];

      HaxeGenericResolver lhsResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(leftChild);
      HaxeGenericResolver rhsResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(rightChild);

      ResultHolder lhsType = HaxeTypeResolver.getPsiElementType(leftChild, binaryExpression, lhsResolver);
      ResultHolder rhsType = HaxeTypeResolver.getPsiElementType(rightChild, binaryExpression, rhsResolver);

      ResultHolder nonNullLhsType = lhsType.tryUnwrapNullType();
      ResultHolder nonNullRhsType = lhsType.tryUnwrapNullType();
      String operatorText = operator.getText();

      // warning for operators on dynamic that are not simple equals expresions
      if (!operatorText.equals("==") && !operatorText.equals("!=")) {
        if (nonNullLhsType.isDynamic() || nonNullRhsType.isDynamic()) {
          String error = "Applying " + operatorText + " operator to a Dynamic value may cause Runtime exceptions on static targets if the value does not support the operation";
          holder.newAnnotation(HighlightSeverity.WEAK_WARNING, error)
                  .range(binaryExpression)
                  .create();
        }
      }

      HaxeExpressionEvaluatorContext context = new HaxeExpressionEvaluatorContext(binaryExpression);
      HaxeExpressionEvaluator.evaluate(binaryExpression, context, null);
      ResultHolder result = context.result;

      if (result.isUnknown()) {

        // ignoring macro values as we dont always know the type
        boolean containsMacroExpression = HaxeMacroUtil.isMacroType(lhsType) | HaxeMacroUtil.isMacroType(rhsType);
        // ignore  unknown and dynamic for now
        if (lhsType.isUnknown() || rhsType.isUnknown() || containsMacroExpression) {
          return;
        }
        // ignoring enums as they are often "OR-ed" (|) in switch expressions (and EnumValue.match)
        if (isInsidePatternMatcher(binaryExpression) && isAllPipedEnumValues(binaryExpression)) {
          return;
        }


        String error = "Unable to apply operator " + operatorText + " for types " + lhsType.getType() + " and " + rhsType.getType();
        holder.newAnnotation(HighlightSeverity.ERROR, error)
                .range(binaryExpression)
                .create();
      }
    }
  }

  private static boolean isAllPipedEnumValues(HaxeExpression expression) {
    if (expression instanceof HaxeBinaryExpression binaryExpression) {
      HaxeExpression leftExpression = binaryExpression.getLeftExpression();
      HaxeExpression rightExpression = binaryExpression.getRightExpression();
      return binaryExpression.getOperator().textMatches("|")
              && isAllPipedEnumValues(leftExpression)
              && isAllPipedEnumValues(rightExpression);

    } else if (expression instanceof HaxeCallExpression callExpression) {
      if(callExpression.getExpression() instanceof HaxeReferenceExpression referenceExpression ) {
        return referenceExpression.resolve() instanceof HaxeEnumValueDeclaration;
      }

    } else if (expression instanceof HaxeReferenceExpression referenceExpression) {
      HaxeExpressionEvaluatorContext evaluate = HaxeExpressionEvaluator.evaluate(referenceExpression);
      return evaluate.result.isEnumValueType();
    }
    return false;
  }
}
