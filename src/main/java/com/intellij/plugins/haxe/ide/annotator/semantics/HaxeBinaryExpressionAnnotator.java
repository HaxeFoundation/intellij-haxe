package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

public class HaxeBinaryExpressionAnnotator implements Annotator {
  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {

    if (element instanceof HaxeAssignExpression) {
      // HaxeAssignExpression -> assign is handle by localVarAnnotator etc.
      //HaxeCompareExpression : problem with Class<myClass> being detected as compare  (X < Y)
      return;
    }
    // TODO make IS operator check (require ,  ref is Type combo)

    if (element instanceof HaxeBinaryExpression expression
        // TODO mlo, make a better check to see if element is part of @:op(...)

        && !(expression.getParent() instanceof HaxeCompileTimeMetaArg )) {
      //  ignore if part of switch case expression
      if (PsiTreeUtil.getParentOfType(element, HaxeSwitchCaseExpr.class) == null) {

        check(expression, holder);
      }
    }
  }

  public static void check(final HaxeBinaryExpression binaryExpression, final AnnotationHolder holder) {

    PsiElement[] children = binaryExpression.getChildren();
    if (children.length == 3) {

      HaxeExpressionEvaluatorContext context = new HaxeExpressionEvaluatorContext(binaryExpression);
      HaxeExpressionEvaluator.evaluate(binaryExpression, context, null);

      ResultHolder result = context.result;

      if (result.isUnknown()) {


        PsiElement LeftChild = children[0];
        PsiElement rightChild = children[2];

        HaxeGenericResolver lhsResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(LeftChild);
        HaxeGenericResolver rhsResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(rightChild);
        ResultHolder lhsType = HaxeTypeResolver.getPsiElementType(LeftChild, binaryExpression, lhsResolver);
        ResultHolder rhsType = HaxeTypeResolver.getPsiElementType(rightChild, binaryExpression, rhsResolver);

        // ignoring macro values as we dont always know the type
        boolean containsMacroExpression = HaxeMacroUtil.isMacroType(lhsType) | HaxeMacroUtil.isMacroType(rhsType);
        // ignore  unknown and dynamic for now
        if (lhsType.isUnknown()  || lhsType.isDynamic() || rhsType.isUnknown() || rhsType.isDynamic()  || containsMacroExpression) {
          return;
        }
        // ignoring enums as they are often  "OR-ed" (|) in switch expressions (and EnumValue.match)
        if (lhsType.isEnum() && rhsType.isEnum()) {
          return;
        }
        String operatorText = children[1].getText();
        String error = "Unable to apply operator " + operatorText + " for types " + lhsType.getType() + " and " + rhsType.getType();
        holder.newAnnotation(HighlightSeverity.ERROR, error)
          .range(binaryExpression)
          .create();
      }
    }
  }
}
