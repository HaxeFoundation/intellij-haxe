package com.intellij.plugins.haxe.ide.inspections.typing;

import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.plugins.haxe.lang.psi.HaxeAssignExpression;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.annotator.HaxeProblemReporter;
import com.intellij.plugins.haxe.ide.annotator.semantics.HaxeSemanticsUtil;
import com.intellij.plugins.haxe.ide.annotator.HaxeStandardAnnotation;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.evaluator.assign.AssignExplanation;
import com.intellij.plugins.haxe.model.evaluator.assign.HaxeAssignEvaluation;
import com.intellij.plugins.haxe.model.fixer.HaxeExpressionConversionFixer;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import java.util.List;
import static com.intellij.plugins.haxe.ide.annotator.HaxeStandardAnnotation.typeMismatch;
import static com.intellij.plugins.haxe.ide.annotator.HaxeStandardAnnotation.typeMismatchShadowing;
import static com.intellij.plugins.haxe.model.evaluator.assign.HaxeTypeCompatible.isShadowingType;
import com.intellij.plugins.haxe.ide.inspections.HaxeInspection;

/** Assignment type compatibility, via the plugin's type evaluator. */
public class HaxeAssignmentTypeCompatibilityInspection extends HaxeInspection {

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return checkVisitor(holder, HaxeAssignExpression.class, HaxeAssignmentTypeCompatibilityInspection::check);
  }


  public static void check(@NotNull HaxeAssignExpression psi, @NotNull HaxeProblemReporter reporter) {
    // TODO: Think about how to use models to do this instead. :/
    PsiElement lhs = UsefulPsiTreeUtil.getFirstChildSkipWhiteSpacesAndComments(psi);
    PsiElement assignOperation = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(lhs);
    PsiElement rhs = UsefulPsiTreeUtil.getNextSiblingSkipWhiteSpacesAndComments(assignOperation);
    if (lhs == null || rhs == null) return;

    HaxeGenericResolver lhsResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(lhs);
    HaxeGenericResolver rhsResolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(rhs);

    ResultHolder lhsType = HaxeTypeResolver.getPsiElementType(lhs, psi, lhsResolver);
    rhsResolver.setAssignHint(lhsType.tryUnwrapNullType());

    ResultHolder rhsType = HaxeTypeResolver.getPsiElementType(rhs, psi, rhsResolver);

    // assigning to a method reference requires it to be dynamic
    if(lhs instanceof HaxeReferenceExpression referenceExpression) {
      PsiElement resolve = referenceExpression.resolve();
      if(resolve instanceof HaxeMethodDeclaration methodDeclaration) {
        if(!methodDeclaration.getModel().isDynamic()) {
          reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.cannot.assign.method.cannot.rebind.method"))
                  .range(psi)
                  .create();
          return;
        }
      }
    }
    // Allow Literal Maps and arrays to be assigned to any specifics they have in common with assigned variable.
    if ((rhs instanceof HaxeMapLiteral || rhs instanceof HaxeArrayLiteral) && lhsType.isClassType() && rhsType.isClassType()) {
      ResultHolder[] lhsSpecifics = lhsType.getClassType().getSpecifics();
      ResultHolder[] rhsSpecifics = rhsType.getClassType().getSpecifics();
      int minSpecifics = Math.min(lhsSpecifics.length, rhsSpecifics.length);
      for (int i = 0; i < minSpecifics; i++) {
        ResultHolder unified = HaxeTypeUnifier.unify(lhsSpecifics[i], rhsSpecifics[i]);
        if(!unified.isUnknown()) {
          rhsSpecifics[i] = unified;
        }
      }
    }
    // hack for String since its not a class with operator overloads but can have any object added to it;
    ResultHolder unwrap = lhsType.tryUnwrapNullType();
    if (unwrap.getType().isString() && assignOperation.textMatches("+=")) {
      return;
    }
    HaxeAssignEvaluation assignEvaluation = lhsType.canAssignEvaluation(rhsType);
    if (!assignEvaluation.result) {
      List<HaxeExpressionConversionFixer> fixers = HaxeExpressionConversionFixer.createStdTypeFixers(rhs, rhsType.getType(), lhsType.getType());

      AssignExplanation messages = assignEvaluation.explanations;
      if(messages.hasMissingMembers() || messages.hasWrongTypeMembers()) {
        if(messages.hasMissingMembers()) {
          HaxeStandardAnnotation.typeMismatchMissingMembers(reporter, rhs, messages)
            .create();
        }
        if(messages.hasWrongTypeMembers()) {
          HaxeStandardAnnotation.addTypeMismatchWrongTypeMembersAnnotations(reporter, rhs, messages);
        }
      }else {
        if (assignEvaluation.explanations.hasMissingModel()) {
          HaxeStandardAnnotation.typeModelMissing(reporter, rhs, assignEvaluation.explanations.getMissingModel().getFirst());
        } else if(isShadowingType(rhsType.getType(), lhsType.getType())) {
          typeMismatchShadowing(reporter, rhs, rhsType.toPresentationString(), lhsType.toPresentationString()).create();
        } else {
          HaxeProblemReporter.Problem builder = typeMismatch(reporter, rhs, rhsType.toPresentationString(), lhsType.toPresentationString());
          fixers.forEach(builder::withFix);
          builder.create();
        }
      }
    }
    if (lhsType.isImmutable()) {
      // TODO: Think about providing a quick-fix for immutability; remember final markings come from metadata, too.
      reporter.problem(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.cannot.assign.value.to.final.variable"))
        .range(psi)
        .create();
    }
      HaxeSemanticsUtil.checkNullAssignForNonNullableType(reporter, rhsType, lhsType, rhs);
  }

}
