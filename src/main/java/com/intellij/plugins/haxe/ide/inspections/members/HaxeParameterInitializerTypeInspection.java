package com.intellij.plugins.haxe.ide.inspections.members;

import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.plugins.haxe.lang.psi.HaxeMethod;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import com.intellij.plugins.haxe.ide.annotator.HaxeProblemReporter;
import com.intellij.plugins.haxe.ide.annotator.semantics.HaxeSemanticsUtil;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.*;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.Collection;
import com.intellij.plugins.haxe.ide.inspections.HaxeInspection;

/** Parameter default values must be constant and match the declared type. */
public class HaxeParameterInitializerTypeInspection extends HaxeInspection {

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return checkVisitor(holder, HaxeMethod.class, HaxeParameterInitializerTypeInspection::checkParameterInitializers);
  }
  public static void checkParameterInitializers(final HaxeMethod methodPsi, final HaxeProblemReporter reporter) {
    for (final HaxeParameterModel param : methodPsi.getModel().getParameters()) {
      HaxeVarInit varInitPsi = param.getVarInitPsi();
      HaxeTypeTag typeTagPsi = param.getTypeTagPsi();
      if (varInitPsi != null) {
        checkConstExpression(varInitPsi, reporter);
        if (typeTagPsi != null) {
          HaxeSemanticsUtil.TypeTagChecker.check(param.getBasePsi(), typeTagPsi, varInitPsi, true, reporter);
        }
      }
    }
  }
  private static void checkConstExpression(HaxeVarInit varInitPsi, HaxeProblemReporter reporter) {
    HaxeExpression expression = varInitPsi.getExpression();
    checkConstExpression(reporter, expression);

  }
  private static void checkConstExpression(HaxeProblemReporter reporter, PsiElement expression) {
    if (expression instanceof HaxeConstantExpression) return;
    if (expression instanceof HaxeArrayLiteral
        || expression instanceof HaxeMapLiteral
        || expression instanceof HaxeObjectLiteral) {
      annotateNotConstant(expression, reporter);

    } else if (expression instanceof HaxeCallExpression ) {
      annotateNotConstant(expression, reporter);

    } else if (expression instanceof HaxeParenthesizedExpression parenthesizedExpression) {
      Collection<HaxeExpression> children = PsiTreeUtil.findChildrenOfAnyType(parenthesizedExpression,
              HaxeParenthesizedExpression.class,
              HaxeReferenceExpression.class,
              HaxeArrayLiteral.class,
              HaxeMapLiteral.class,
              HaxeObjectLiteral.class);

      for (HaxeExpression haxeExpression : children) {
        checkConstExpression(reporter, haxeExpression);
      }


    } else if (expression instanceof HaxeReferenceExpression referenceExpression) {
      PsiElement resolve = referenceExpression.resolve();
      if (resolve instanceof HaxeEnumValueDeclaration) return;
      if (resolve instanceof HaxePsiField field ) {
        if( field.isInline())return;
        PsiClass containingClass = field.getContainingClass();
        if(containingClass != null && containingClass.isEnum()){
          // make sure its not a property if in abstract enum
          if(field instanceof HaxeFieldDeclaration declaration
             && declaration.getPropertyDeclaration() == null) return;
        }
      }
      annotateNotConstant(expression, reporter);
    }
  }
  private static void annotateNotConstant(PsiElement element, HaxeProblemReporter reporter) {
    reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.default.argument.value.should.be.constant"))
            .range(element)
            .create();
  }

}
