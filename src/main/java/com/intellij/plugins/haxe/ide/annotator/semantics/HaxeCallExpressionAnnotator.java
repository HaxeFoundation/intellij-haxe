package com.intellij.plugins.haxe.ide.annotator.semantics;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;


import static com.intellij.plugins.haxe.ide.annotator.semantics.HaxeCallExpressionUtil.*;

public class HaxeCallExpressionAnnotator implements Annotator {
  @Override
  public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
    if (element instanceof HaxeCallExpression expression) {
      if (expression.getExpression() instanceof HaxeReference reference) {
        final PsiElement resolved = reference.resolve();
        if (resolved instanceof HaxePsiField field) {
          CallExpressionValidation validation = checkFunctionCall(expression, field);
          createErrorAnnotations(validation, holder);
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
