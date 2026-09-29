package com.intellij.plugins.haxe.ide.inspections.operators;

import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.plugins.haxe.lang.psi.HaxeBinaryExpression;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.ide.annotator.HaxeProblemReporter;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluatorContext;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import static com.intellij.plugins.haxe.model.evaluator.callexpression.EnumValueMatchUtil.isInsidePatternMatcher;
import com.intellij.plugins.haxe.ide.inspections.HaxeInspection;

/** Binary operators that cannot be applied to their operand types. */
public class HaxeBinaryOperatorApplicabilityInspection extends HaxeInspection {

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return checkVisitor(holder, HaxeBinaryExpression.class, HaxeBinaryOperatorApplicabilityInspection::check);
  }


  public static void check(@NotNull HaxeBinaryExpression binaryExpression, @NotNull HaxeProblemReporter reporter) {
    if (binaryExpression instanceof HaxeAssignExpression) {
      // assignment compatibility is its own inspection
      //HaxeCompareExpression : problem with Class<myClass> being detected as compare  (X < Y)
      return;
    }
    // TODO mlo, make a better check to see if element is part of @:op(...)
    if (binaryExpression.getParent() instanceof HaxeCompiletimeMetaArg) return;
    //  ignore if part of switch case expression
    if (PsiTreeUtil.getParentOfType(binaryExpression, HaxeSwitchCaseExpr.class) != null) return;

    PsiElement[] children = binaryExpression.getChildren();
    if (children.length == 3) {
      // skip ??: operand types are HaxeIncompatibleInitializationInspection's
      // checkCoalescing (the 4.3 level gate stays in HaxeNullCoalescingAnnotator)
      PsiElement operator = children[1];
      if (operator.textMatches("??")) return;


      PsiElement leftChild = children[0];
      PsiElement rightChild = children[2];

      HaxeGenericResolver lhsResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(leftChild);
      HaxeGenericResolver rhsResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(rightChild);

      ResultHolder lhsType = HaxeTypeResolver.getPsiElementType(leftChild, binaryExpression, lhsResolver);
      ResultHolder rhsType = HaxeTypeResolver.getPsiElementType(rightChild, binaryExpression, rhsResolver);

      ResultHolder nonNullLhsType = lhsType.tryUnwrapNullType();
      ResultHolder nonNullRhsType = rhsType.tryUnwrapNullType();
      String operatorText = operator.getText();

      // warning for operators on dynamic that are not simple equals expresions
      if (!operatorText.equals("==") && !operatorText.equals("!=")) {
        if (nonNullLhsType.isDynamic() || nonNullRhsType.isDynamic()) {
          String error = HaxeBundle.message("haxe.semantic.binary.operator.on.dynamic", operatorText);
          reporter.problem(HighlightSeverity.WEAK_WARNING, error)
                  .range(binaryExpression)
                  .create();
        }
      }

      HaxeExpressionEvaluatorContext context = new HaxeExpressionEvaluatorContext(binaryExpression);
      HaxeExpressionEvaluator.evaluate(binaryExpression, context, null);
      ResultHolder result = context.result;

      if (result.isUnknown()) {

        // an unknown operand cannot be judged - macro values included, their types are not always known
        boolean containsMacroExpression = HaxeMacroUtil.isMacroType(lhsType) | HaxeMacroUtil.isMacroType(rhsType);
        if (lhsType.isUnknown() || rhsType.isUnknown() || containsMacroExpression) {
          return;
        }
        // ignoring enums as they are often "OR-ed" (|) in switch expressions (and EnumValue.match)
        if (isInsidePatternMatcher(binaryExpression) && isAllPipedEnumValues(binaryExpression)) {
          return;
        }


        String error = HaxeBundle.message("haxe.semantic.binary.operator.not.applicable",
                                          operatorText, lhsType.getType(), rhsType.getType());
        reporter.problem(HighlightSeverity.ERROR, error)
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
