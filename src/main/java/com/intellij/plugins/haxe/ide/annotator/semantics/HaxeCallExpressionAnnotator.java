package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.type.*;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;


import static com.intellij.plugins.haxe.ide.annotator.semantics.HaxeCallExpressionUtil.*;
import static com.intellij.plugins.haxe.model.type.HaxeTypeCompatible.getUnderlyingFunctionIfAbstractNull;

public class HaxeCallExpressionAnnotator implements Annotator {
  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (element instanceof HaxeCallExpression expression) {
      if (expression.getExpression() instanceof HaxeReference reference) {
        final PsiElement resolved = reference.resolve();
        if (resolved instanceof HaxePsiField field) {
          HaxeGenericResolver resolver = HaxeGenericResolverUtil.generateResolverFromScopeParents(reference);
          ResultHolder type = HaxeTypeResolver.getFieldOrMethodReturnType(field, resolver);
          SpecificFunctionReference functionType = type.getFunctionType();

          // handle Null<FunctionType>
          if (type.getClassType() != null) {
            if (type.getClassType().isNullType()) {
              SpecificFunctionReference functionReference = getUnderlyingFunctionIfAbstractNull(type.getClassType());
              if (functionReference != null) {
                functionType = functionReference;
              }
            }
          }

          if (functionType != null) {
            if (functionType.functionType != null) {
              CallExpressionValidation validation = checkFunctionCall(expression, functionType.functionType);
              createErrorAnnotations(validation, holder);
            }
            else if (functionType.method != null) {
              CallExpressionValidation validation = checkMethodCall(expression, functionType.method.getMethod());
              createErrorAnnotations(validation, holder);
            }
          }else {
            SpecificTypeReference typeReference = type.getType();
            // if not enum value constructor or dynamic, show error
            if (!type.isEnumValueType() && !type.isDynamic()) {
              // TODO bundle
              holder.newAnnotation(HighlightSeverity.ERROR, typeReference.toPresentationString() + " is not a callable type")
                .range(element)
                .create();
            }
          }
        }
        else if (resolved instanceof HaxeMethod method) {
          CallExpressionValidation validation = checkMethodCall(expression, method);
          createErrorAnnotations(validation, holder);
        }
      }
    }
    if (element instanceof HaxeNewExpression newExpression) {
      CallExpressionValidation validation = checkConstructor(newExpression);
      createErrorAnnotations(validation, holder);
    }
  }

  private void createErrorAnnotations(@NotNull CallExpressionValidation validation, @NotNull AnnotationHolder holder) {
    validation.errors.forEach(record -> holder.newAnnotation(HighlightSeverity.ERROR, record.message())
      .range(record.range())
      .create());
  }


}
