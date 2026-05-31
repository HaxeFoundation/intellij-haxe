package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.lang.psi.fakes.HaxeFakeComponentBindMethod;
import com.intellij.plugins.haxe.model.FullyQualifiedInfo;
import com.intellij.plugins.haxe.model.HaxeAbstractClassModel;
import com.intellij.plugins.haxe.model.HaxeClassModel;
import com.intellij.plugins.haxe.model.evaluator.callexpression.*;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;


import java.util.ArrayList;
import java.util.List;

import static com.intellij.plugins.haxe.model.evaluator.callexpression.HaxeCallExpressionUtil.tryGetCallieType;


public class HaxeCallExpressionAnnotator implements Annotator {
  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if(!element.isValid()) return;
    if (element instanceof HaxeCallExpression callExpression) {
      if (callExpression.getExpression() instanceof HaxeReference reference) {
        PsiElement resolved = reference.resolve();

        if(resolved instanceof HaxeFakeComponentBindMethod bindMethod) {
          // map bind call to original method that we are binding as that's where the parameter info is.
          resolved = bindMethod.getOriginalMethodOrFunction();
        }
        if (resolved instanceof HaxePsiField  || resolved instanceof HaxeParameter ) {
          HaxeNamedComponent component = (HaxeNamedComponent)resolved;
          HaxeGenericResolver resolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(reference);

          SpecificTypeReference callieType = tryGetCallieType(callExpression);
          if (callieType instanceof SpecificHaxeClassReference classReference &&  !callieType.isUnknown()) {
            resolver.addAll(classReference.getGenericResolver());
          }


          ResultHolder type = HaxeTypeResolver.getFieldOrMethodReturnType(component, resolver);
          SpecificTypeReference typeReference;
          if (type.getClassType() != null) {
             typeReference = type.getClassType().fullyResolveTypeDefAndUnwrapNullTypeReference(true);
          } else  {
            typeReference = type.getType();
          }

          if (typeReference  instanceof  SpecificFunctionReference functionType) {
            // function type or function literal
            if ( functionType.method == null) {
              HaxeCallExpressionContext context = HaxeCallExpressionUtil.createContextForFunctionCall(callExpression, functionType);
              HaxeCallExpressionEvaluation validation = context.evaluateWithAnnotationData(callExpression);
              createAnnotations(holder, validation);
            } else {
              HaxeCallExpressionContextContainer contextContainer = HaxeCallExpressionUtil.createContextForMethodCall(callExpression, functionType.method.getMethod());
              HaxeCallExpressionEvaluation validation = contextContainer.evaluateContextsWithAnnotationData(callExpression);
              if(validation != null) {
                createAnnotations(holder, validation);
              }
            }
          } else if (typeReference instanceof SpecificHaxeClassReference classReference) {
            HaxeClassModel haxeClassModel = classReference.getHaxeClassModel();
            if (haxeClassModel != null) {
              if (haxeClassModel.isCallable()) return;
            }
            if(haxeClassModel instanceof HaxeAbstractClassModel abstractModel) {
              // abstracts can be casted to functionTypes so we need to check for function types that matches our callExpressions
              List<SpecificTypeReference> castToTypes = new ArrayList<>();
              castToTypes.addAll(abstractModel.getDirectCastToTypes(classReference.getGenericResolver()));
              castToTypes.addAll(abstractModel.getImplicitCastToTypes(classReference, classReference.getGenericResolver()));
              List<SpecificFunctionReference> functionTypes = castToTypes.stream()
                      .filter(SpecificFunctionReference.class::isInstance)
                      .map(SpecificFunctionReference.class::cast)
                      .toList();

              HaxeCallExpressionEvaluation validation = null;
              for (SpecificFunctionReference functionType : functionTypes) {
                HaxeCallExpressionContext context = HaxeCallExpressionUtil.createContextForFunctionCall(callExpression, functionType);
                validation = context.evaluateWithAnnotationData(callExpression);
                if(validation.isValid()) return;
              }

              if(validation != null) {
                createAnnotations(holder, validation);
                return;
              }
            }
            // if not enum value constructor, expr, dynamic or unknown, show error
            if (!type.isEnumValueType() && !type.isDynamic() && !type.isUnknown() && !type.getType().isExpr()) {
              // TODO bundle
              holder.newAnnotation(HighlightSeverity.ERROR, typeReference.toPresentationString(true) + " is not a callable type")
                .range(element)
                .create();
            }
          }
        }
        else if (resolved instanceof HaxeMethod method) {
          if (isTrace(method))return;
          HaxeCallExpressionContextContainer contextContainer = HaxeCallExpressionUtil.createContextForMethodCall(callExpression, method);
          HaxeCallExpressionEvaluation validation = contextContainer.evaluateContextsWithAnnotationData(callExpression);
          if(validation != null) {
            createAnnotations(holder, validation);
          }
        }
      }
    }
    if (element instanceof HaxeNewExpression newExpression) {
      HaxeCallExpressionContextContainer context = HaxeCallExpressionUtil.createContextForConstructorCall(newExpression);
        HaxeCallExpressionEvaluation validation = context.evaluateContextsWithAnnotationData(newExpression);
        if(validation != null) {
          createAnnotations(holder, validation);
        }
    }
  }

  private void createAnnotations(@NotNull AnnotationHolder holder, HaxeCallExpressionEvaluation validation) {
    List<EvaluationAnnotationData> errors = validation.getErrors();
    List<EvaluationAnnotationData> warnings = validation.getWarnings();
    if (!errors.isEmpty())createErrorAnnotations(errors, holder);
    if (!warnings.isEmpty())createWarningAnnotations(warnings, holder);
  }

  // the trace method in std does not have rest arg so we ignore it
  private static boolean isTrace(HaxeMethod method) {
    FullyQualifiedInfo info = method.getModel().getQualifiedInfo();
    if (info == null) return false;
    return "Log".equals(info.className)
           && "haxe".equals(info.packageName)
           && "trace".equals(info.memberName);
  }

  private void createErrorAnnotations(List<EvaluationAnnotationData> annotationData, @NotNull AnnotationHolder holder) {
    annotationData.forEach(record -> holder.newAnnotation(HighlightSeverity.ERROR, record.message())
      .range(record.range())
      .create());
  }
  private void createWarningAnnotations(List<EvaluationAnnotationData> annotationData, @NotNull AnnotationHolder holder) {
    annotationData.forEach(record -> holder.newAnnotation(HighlightSeverity.WEAK_WARNING, record.message())
      .range(record.range())
      .create());
  }


}
