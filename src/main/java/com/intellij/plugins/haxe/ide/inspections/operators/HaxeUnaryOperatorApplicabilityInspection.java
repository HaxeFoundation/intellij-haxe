package com.intellij.plugins.haxe.ide.inspections.operators;

import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.plugins.haxe.lang.psi.HaxeUnaryExpression;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.ide.annotator.HaxeProblemReporter;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.HaxeFieldModel;
import com.intellij.plugins.haxe.model.HaxeMethodModel;
import com.intellij.plugins.haxe.model.evaluator.HaxeExpressionEvaluator;
import com.intellij.plugins.haxe.model.type.HaxeTypeResolver;
import com.intellij.plugins.haxe.model.type.ResultHolder;
import com.intellij.plugins.haxe.model.type.SpecificHaxeClassReference;
import com.intellij.plugins.haxe.model.type.SpecificTypeReference;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import java.util.List;
import com.intellij.plugins.haxe.ide.inspections.HaxeInspection;

/** Increment/decrement applied to expressions that cannot be written to. */
public class HaxeUnaryOperatorApplicabilityInspection extends HaxeInspection {

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return checkVisitor(holder, HaxeUnaryExpression.class, HaxeUnaryOperatorApplicabilityInspection::check);
  }


  public static void check(@NotNull HaxeUnaryExpression unaryExpression, @NotNull HaxeProblemReporter reporter) {
    // ignore if  inside meta, ex @:op(A++)
    if (unaryExpression.getParent() instanceof HaxeCompiletimeMetaArg) return;

    HaxeOperator operator = unaryExpression.getOperator();
    HaxeExpression expression = unaryExpression.getExpression();

    if (operator.textMatches("++") || operator.textMatches("--")) {

        if (expression instanceof HaxeLiteralExpression || expression instanceof HaxeStringLiteralExpression) {
            reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.invalid.assign")).range(unaryExpression).create();
            return;
        }
        if (expression instanceof HaxeCallExpression ) {
            reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.invalid.assign")).range(unaryExpression).create();
            return;
        }
        if (expression instanceof HaxeNewExpression  newExpression) {
            ResultHolder resultHolder = HaxeTypeResolver.getTypeFromType(newExpression.getType());
            if (!resultHolder.isUnknown()) {
                // abstract types can have operator overloads, a "new BigInt(0)++" (abstract) might be allowed
                // but other classes does not support that and  "a new MyClass()++" does not make sense
                if(!resultHolder.getType().isAbstractType()) {
                    reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.invalid.assign")).range(unaryExpression).create();
                }
            }
            return;
        }


        if (expression instanceof HaxeArrayAccessExpression) {
            ResultHolder result = HaxeExpressionEvaluator.evaluate(expression).result;
            checkIfPostOrPrefixIsValid(unaryExpression, reporter, result);
            return;

        }
        if (expression instanceof HaxeReferenceExpression referenceExpression) {
            ResultHolder result = HaxeExpressionEvaluator.evaluate(expression).result;

            boolean isWritable = checkIfPropertyWritable(referenceExpression);
            if (!isWritable) {
                reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.expression.not.writable"))
                        .range(unaryExpression)
                        .create();
                return;
            }

            // shortcut for common types
            SpecificTypeReference type = result.getType();
            if (!result.isImmutable() && isWritable && (type.isInt() || type.isFloat() || type.isSingle())) return;

            checkIfPostOrPrefixIsValid(unaryExpression, reporter, result);
        }
    }
  }

  private static void checkIfPostOrPrefixIsValid(@NotNull HaxeUnaryExpression unaryExpression, @NotNull HaxeProblemReporter reporter, @NotNull ResultHolder result) {
      if (result.isUnknown()) return;

      SpecificTypeReference type = result.getType();

      //  resolve is typedef before checking
      if(result.isTypeDef() && result.getClassType() != null) {
          type = result.getClassType().fullyResolveTypeDefAndUnwrapNullTypeReference();
      }

      if (result.isImmutable()) {
          reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.cannot.assign.immutable.reference"))
            .range(unaryExpression).create();
      } else if (!type.isAbstractType()) {
          reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.unary.type.should.be.int", type.toPresentationString()))
            .range(unaryExpression).create();
      } else {

          if (type.isBool()) {
              reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.expression.not.writable")).range(unaryExpression).create();
          }
          if (type.isString()) {
              reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.invalid.assign")).range(unaryExpression).create();
          }
          if (!type.isAbstractType()) {
              reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.invalid.assign")).range(unaryExpression).create();
          }
          if(type instanceof SpecificHaxeClassReference classReference && classReference.isAbstractType()) {
              if (!classReference.isCoreType()) {
                  List<HaxeMethodModel> overloads = classReference.getOperatorOverloads(unaryExpression.getOperator());
                  if (overloads.isEmpty()) {
                      String operator = unaryExpression.getOperator().getText();
                      reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.no.unary.overload.found", operator))
                        .range(unaryExpression).create();
                  }
              }
          }
      }
  }

  private static boolean checkIfPropertyWritable(HaxeReferenceExpression referenceExpression) {
      PsiElement refResolve = referenceExpression.resolve();
      if (refResolve instanceof HaxeFieldDeclaration declaration) {
          if (declaration.getModel() instanceof HaxeFieldModel model && model.isProperty()) {
              if (model.isWritableFromOutside()) return true;

              boolean sameScope = isInsideSameClassOrFile(referenceExpression, declaration);
              return model.isWritableFromInside() && sameScope;
          }
      }
      return true;
  }

  private static boolean isInsideSameClassOrFile(HaxeReferenceExpression referenceExpression, HaxeFieldDeclaration declaration) {
      HaxeClass haxeClass = PsiTreeUtil.getStubOrPsiParentOfType(referenceExpression, HaxeClass.class);
      if (haxeClass != null) return haxeClass == declaration.getContainingClass();

      HaxeModule haxeModule = PsiTreeUtil.getStubOrPsiParentOfType(referenceExpression, HaxeModule.class);
      if (haxeModule != null) return haxeModule.getContainingFile() == declaration.getContainingFile();

      return false;
  }

}
