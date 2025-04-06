package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.ide.annotator.HaxeStandardAnnotation;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.evaluator.assign.AssignExplanation;
import com.intellij.plugins.haxe.model.evaluator.assign.HaxeAssignEvaluation;
import com.intellij.plugins.haxe.model.fixer.HaxeExpressionConversionFixer;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.intellij.plugins.haxe.ide.annotator.HaxeSemanticAnnotatorInspections.ASSIGNMENT_TYPE_COMPATIBILITY_CHECK;
import static com.intellij.plugins.haxe.ide.annotator.HaxeStandardAnnotation.typeMismatch;

public class HaxeAssignExpressionAnnotator implements Annotator {
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (element instanceof HaxeAssignExpression assignExpression) {
      check(assignExpression, holder);
    }
  }

  public static void check(HaxeAssignExpression psi, AnnotationHolder holder) {
    if (!ASSIGNMENT_TYPE_COMPATIBILITY_CHECK.isEnabled(psi)) return;

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

    // check if we try to assign to a method reference, if so its required ot be dynamic
    if(lhs instanceof HaxeReferenceExpression referenceExpression) {
      PsiElement resolve = referenceExpression.resolve();
      if(resolve instanceof HaxeMethodDeclaration methodDeclaration) {
        if(!methodDeclaration.getModel().isDynamic()) {
          holder.newAnnotation(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.cannot.assign.method.cannot.rebind.method"))
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
          HaxeStandardAnnotation.typeMismatchMissingMembers(holder, rhs, messages)
            .create();
        }
        if(messages.hasWrongTypeMembers()) {
          HaxeStandardAnnotation.addtypeMismatchWrongTypeMembersAnnotations(holder, rhs, messages);
        }
      }else {
        if (assignEvaluation.explanations.hasMissingModel()) {
          HaxeStandardAnnotation.typeModelMissing(holder, rhs, assignEvaluation.explanations.getMissingModel().getFirst());
        } else {
          AnnotationBuilder builder = typeMismatch(holder, rhs, rhsType.toPresentationString(), lhsType.toPresentationString());
          fixers.forEach(builder::withFix);
          builder.create();
        }
      }
    }
    if (lhsType.isImmutable()) {
      // TODO: Think about providing a quick-fix for immutability; remember final markings come from metadata, too.
      holder.newAnnotation(HighlightSeverity.ERROR, HaxeBundle.message("haxe.semantic.cannot.assign.value.to.final.variable"))
        .range(psi)
        .create();
    }
  }
}
