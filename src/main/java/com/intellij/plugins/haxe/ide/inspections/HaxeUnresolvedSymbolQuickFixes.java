package com.intellij.plugins.haxe.ide.inspections;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.plugins.haxe.ide.inspections.intentions.*;
import com.intellij.plugins.haxe.lang.psi.*;
import com.intellij.plugins.haxe.model.type.SpecificFunctionReference;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

public class HaxeUnresolvedSymbolQuickFixes {


  public static LocalQuickFix createFunctionQuickfix(@NotNull HaxeCallExpression expression) {
    return new HaxeIntroduceFunctionIntention(expression);
  }

  public static LocalQuickFix createMethodQuickfix(@NotNull HaxeCallExpression expression,  @NotNull HaxeClass targetClass) {
    return new HaxeIntroduceMethodIntention(expression, targetClass);
  }
  public static LocalQuickFix createMethodQuickfix(@NotNull SpecificFunctionReference functionReference, HaxeReferenceExpression referenceExpression, @NotNull HaxeClass targetClass) {
    return new HaxeIntroduceMethodFromTypeIntention(functionReference,referenceExpression, targetClass);
  }


  public static LocalQuickFix createLocalVarQuickfix(@NotNull HaxeReferenceExpression expression) {
    // do not suggest local var if reference is a chained expression
    if(expression.getParent() instanceof HaxeReferenceExpression) return null;
    if(expression.textContains('.')) return null;
    // prevent in init expressions that are not inside code blocks
    if (PsiTreeUtil.getParentOfType(expression, HaxeFieldDeclaration.class) != null) return null;
    if (PsiTreeUtil.getParentOfType(expression, HaxePropertyDeclaration.class) != null) return null;

    return new HaxeIntroduceVariableIntention(expression);
  }

  public static LocalQuickFix createFieldQuickfix(@NotNull HaxeReferenceExpression expression,  @NotNull HaxeClass targetClass) {
    if(expression.getParent() instanceof HaxeType) return null;
    return new HaxeIntroduceFieldIntention(expression, targetClass);

  }


  public static  LocalQuickFix createMethodParameterQuickfix(HaxeReferenceExpression reference) {
    // prevent if not in Method declaration
    HaxeMethodDeclaration declaration = PsiTreeUtil.getParentOfType(reference, HaxeMethodDeclaration.class);
    if(declaration == null) return null;

    return new HaxeIntroduceMethodParameterIntention(reference, declaration);
  }


}
